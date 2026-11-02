# Docker: imagens e stack completa

Como o backend e o frontend viram imagens Docker, como subir os dois juntos com o `docker-compose.yml` (que fica neste repositório) e como levar as imagens para outra máquina sem rebuildar.

Os dois repositórios precisam estar lado a lado ao gerar as imagens:

```text
faculdade/
├── library-backend/            # este repositório (Dockerfile, docker-compose.yml, scripts/)
└── biblioteca-web-frontend/    # repositório do frontend (Dockerfile, nginx/, scripts/build-image.sh)
```

## Sumário

- [Visão geral](#visão-geral)
- [Subir a stack](#subir-a-stack)
- [Variáveis do compose](#variáveis-do-compose)
- [Dockerfile do backend](#dockerfile-do-backend)
- [Dockerfile do frontend e Nginx](#dockerfile-do-frontend-e-nginx)
- [Scripts de imagem](#scripts-de-imagem)
- [Levar as imagens para outra máquina](#levar-as-imagens-para-outra-máquina)
- [Rodar sem o compose](#rodar-sem-o-compose)
- [Fuso horário](#fuso-horário)
- [Problemas comuns](#problemas-comuns)

## Visão geral

```text
navegador ──► http://localhost:8081 ──► Nginx (frontend) ──► /api/*  ──► backend:8080
                                          │                    (remove o prefixo /api)
                                          └─► demais rotas ──► arquivos estáticos do React (SPA)
```

| Imagem | Base de runtime | Porta interna | Porta no host (padrão) |
| --- | --- | --- | --- |
| `library-backend` | `eclipse-temurin:21-jre-alpine` | 8080 | 8080 (`BACKEND_PORT`) |
| `biblioteca-web-frontend` | `nginx:1.30-alpine` | 80 | 8081 (`FRONTEND_PORT`) |

O frontend chama `/api/...` no próprio Nginx, que repassa ao backend removendo o prefixo (`/api/books` vira `/books`). Por isso o navegador só conhece uma origem e não há problema de CORS no uso normal. O banco é o H2 em memória: ao derrubar a stack, os dados somem.

## Subir a stack

A partir de `library-backend/`:

```bash
docker compose up -d --build   # builda as duas imagens e sobe a stack
docker compose up -d           # reaproveita imagens que já existem (ex.: carregadas do .tar.gz)
docker compose logs -f         # acompanha os logs
docker compose ps              # estado e health dos serviços
docker compose down            # derruba a stack (os dados do H2 somem)
```

Acesse o frontend em `http://localhost:8081` e a API direta em `http://localhost:8080` (Swagger em `http://localhost:8080/swagger-ui/index.html`).

Detalhes do `docker-compose.yml`:

- Projeto `biblioteca` (campo `name:`). Com outro nome de projeto (`docker compose -p outro up -d`) é possível subir uma segunda stack em paralelo, desde que as portas sejam diferentes.
- Serviço `library-backend`: imagem `library-backend:latest`, build com `.` (Dockerfile da raiz), `restart: unless-stopped`. O healthcheck vem do Dockerfile.
- Serviço `biblioteca-web-frontend`: imagem `biblioteca-web-frontend:latest`, build com contexto `../biblioteca-web-frontend` e `VITE_API_URL` como build arg, `depends_on` do backend com `condition: service_healthy` (o frontend só sobe depois de o backend passar no healthcheck) e `restart: unless-stopped`.
- Como os dois serviços têm `image:` e `build:`, `up -d` sem `--build` usa a imagem local se ela existir e só builda se faltar.

## Variáveis do compose

Defina no ambiente ou em um arquivo `.env` ao lado do `docker-compose.yml`. Todas são opcionais.

| Variável | Padrão | Efeito |
| --- | --- | --- |
| `BACKEND_PORT` | `8080` | Porta do host para a API |
| `FRONTEND_PORT` | `8081` | Porta do host para o frontend |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:4173,http://localhost:${FRONTEND_PORT:-8081}` | Origens liberadas no CORS do backend (vira a propriedade `app.cors.allowed-origins`) |
| `JAVA_OPTS` | vazio | Opções extras da JVM do backend (ex.: `-Xmx512m`) |
| `BACKEND_TZ` | `America/Sao_Paulo` | Fuso do backend, que define o "hoje" (veja [Fuso horário](#fuso-horário)) |
| `VITE_API_URL` | `/api` | URL base da API embutida no bundle do frontend (só vale em build; mudar exige rebuildar a imagem do frontend) |

Exemplo, subindo em outras portas:

```bash
BACKEND_PORT=18080 FRONTEND_PORT=18081 docker compose up -d
```

O frontend também lê, em tempo de execução, `API_UPSTREAM` (destino do proxy `/api`, padrão `http://library-backend:8080`, definido no compose) e `NGINX_RESOLVER` (padrão `127.0.0.11`, o DNS embutido do Docker).

## Dockerfile do backend

Arquivo: `Dockerfile` (raiz de `library-backend`). Dois estágios:

1. **build** (`maven:3.9-eclipse-temurin-21`): copia só o `pom.xml` e roda `mvn dependency:go-offline` com cache do BuildKit (`--mount=type=cache,target=/root/.m2`), de modo que as dependências só são baixadas de novo quando o `pom.xml` muda. Depois copia `src/` e roda `mvn package -DskipTests`. Os testes não rodam no build da imagem.
2. **runtime** (`eclipse-temurin:21-jre-alpine`): só o JRE e o jar (`/app/app.jar`).

Pontos do runtime:

| Item | Valor |
| --- | --- |
| Usuário | `app` (sem privilégios, sem shell), não roda como root |
| Porta | `EXPOSE 8080` |
| Fuso | `ENV TZ=America/Sao_Paulo` |
| Opções da JVM | `ENV JAVA_OPTS=""`; o entrypoint é `sh -c "exec java $JAVA_OPTS -jar /app/app.jar"` |
| Healthcheck | `wget -q -O /dev/null http://127.0.0.1:8080/genres` a cada 15 s, timeout 5 s, 60 s de carência e 5 tentativas. Usa `127.0.0.1` porque o `wget` do busybox pode resolver `localhost` para `::1` e falhar sem IPv6 |
| Labels | `org.opencontainers.image.title`, `description` e `source`; o `build-image.sh` acrescenta `version`, `created` e `revision` |

O `.dockerignore` deixa de fora tudo que o Dockerfile não usa (`target/`, `images/`, `.git`, `.idea`, `docs/`, `*.md`, scripts, `mvnw`...), então só `pom.xml` e `src/` entram no contexto e uma mudança em documentação não invalida o cache.

Para buildar e rodar só o backend:

```bash
docker build -t library-backend .
docker run --rm -p 8080:8080 library-backend
```

## Dockerfile do frontend e Nginx

Arquivos no repositório `biblioteca-web-frontend`: `Dockerfile`, `nginx/default.conf.template` e `.dockerignore`.

**Dockerfile (dois estágios)**

1. **build** (`node:22-alpine`): `npm ci` com cache do BuildKit e depois copia só o que o bundle usa (`index.html`, `vite.config.ts`, `postcss.config.cjs`, `tsconfig*.json`, `public/`, `src/`). Mudanças em `nginx/`, docs ou configs de lint não invalidam o cache do `npm run build`. O build arg `VITE_API_URL` (padrão `/api`) é embutido no bundle.
2. **runtime** (`nginx:1.30-alpine`): copia `dist/` para `/usr/share/nginx/html` e o `nginx/default.conf.template` para `/etc/nginx/templates/`. Na inicialização, o entrypoint oficial do Nginx roda `envsubst` no template e grava `/etc/nginx/conf.d/default.conf`. `NGINX_ENVSUBST_FILTER="API_UPSTREAM|NGINX_RESOLVER"` limita a substituição a essas duas variáveis, preservando as do próprio Nginx (`$uri`, `$host`...).
3. Healthcheck: `wget -q -O /dev/null http://127.0.0.1/` a cada 15 s (timeout 5 s, carência 10 s, 3 tentativas).

**Nginx (`default.conf.template`)**

| Trecho | O que faz |
| --- | --- |
| `resolver ${NGINX_RESOLVER} valid=30s ipv6=off` | Resolve o nome do backend a cada requisição (cache de 30 s). Como o destino fica numa variável (`set $api_upstream ${API_UPSTREAM};`), o Nginx sobe mesmo se o backend ainda não existir |
| `location /api/` | `rewrite ^/api/(.*)$ /$1 break;` remove o prefixo e `proxy_pass $api_upstream;` repassa (a query string é preservada). Timeouts: conexão 5 s, leitura 60 s |
| Headers do proxy | `Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`, `X-Forwarded-Host` e `X-Forwarded-Prefix: /api`. Esses `X-Forwarded-*` fazem o backend montar o `Location` dos `201` com a URL pública (`http://localhost:8081/api/genres/10`) |
| `proxy_set_header Origin ""` | Remove o header `Origin`. O navegador envia `Origin` em POST/PUT/PATCH/DELETE mesmo em chamadas da mesma origem; se chegasse ao backend, o Spring trataria como CORS e responderia 403 quando o frontend fosse aberto por uma origem fora da lista (por exemplo `http://127.0.0.1:8081` ou o IP da rede). O `vite.config.ts` faz o mesmo no desenvolvimento |
| `location /assets/` | Arquivos com hash no nome: `Cache-Control: public, max-age=31536000, immutable` |
| `location = /index.html` | `Cache-Control: no-cache`, para o navegador pegar sempre o deploy novo |
| `location /` | `try_files $uri $uri/ /index.html`: fallback do SPA (rotas desconhecidas devolvem o `index.html`) |
| gzip | Ativo para texto, CSS, JS, JSON e SVG acima de 1 KiB |

Para apontar o frontend para um backend em outro lugar, passe `API_UPSTREAM` (URL completa, sem barra no final): `docker run --rm -p 8081:80 -e API_UPSTREAM=http://192.168.0.10:8080 biblioteca-web-frontend`. Se o destino for um nome (e não um IP), ele precisa ser resolvido pelo DNS indicado em `NGINX_RESOLVER`.

## Scripts de imagem

Os scripts geram a imagem, a exportam para um arquivo e o carregam em outra máquina. Eles funcionam de qualquer diretório de trabalho (partem da raiz do repositório) e exigem `docker` com o daemon rodando, `gzip` e `sha256sum` (ou `shasum`). Todos têm `--help`.

| Script | Repositório | Para que serve |
| --- | --- | --- |
| `scripts/build-image.sh [TAG]` | backend e frontend (um em cada) | Builda a imagem do repositório e salva em `.tar.gz` |
| `scripts/build-all-images.sh [TAG]` | backend | Chama o `build-image.sh` dos dois repositórios e junta as saídas no mesmo diretório |
| `scripts/load-images.sh [DIRETORIO]` | backend | Carrega no Docker (`docker load`) todos os `.tar.gz` de um diretório |

### Gerar as imagens

```bash
cd ~/faculdade/library-backend

./scripts/build-all-images.sh              # backend e frontend, tag automática
./scripts/build-all-images.sh 1.0.0        # tag explícita nas duas
./scripts/build-image.sh                   # só o backend
OUTPUT_DIR=/tmp/imagens ./scripts/build-all-images.sh demo
```

(No frontend: `cd ~/faculdade/biblioteca-web-frontend && ./scripts/build-image.sh [TAG]`.)

Regras do `build-image.sh`:

- **Tag:** o argumento; senão a variável `TAG`; senão `git describe --always --dirty` (por exemplo `853ed4a-dirty`; caracteres inválidos viram `-`); sem git, `dev`. A imagem recebe também a tag `latest`. A tag precisa casar com `^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$`.
- **Saída:** `<OUTPUT_DIR>/<IMAGE_NAME>_<TAG>.tar.gz` (`docker save` + `gzip`) e, ao lado, `<arquivo>.tar.gz.sha256`. `OUTPUT_DIR` padrão: `<repo>/images`, que está no `.gitignore` e no `.dockerignore`. Barras e dois-pontos no `IMAGE_NAME` viram `_` no nome do arquivo.
- **Nomes padrão:** `library-backend` e `biblioteca-web-frontend` (`IMAGE_NAME` troca).
- **Labels:** `org.opencontainers.image.version`, `created` e (com git) `revision`.
- **Variáveis:** `TAG`, `IMAGE_NAME`, `OUTPUT_DIR`; no frontend também `VITE_API_URL` (repassada como `--build-arg`, padrão `/api`).

Regras do `build-all-images.sh`:

- Procura o frontend em `../biblioteca-web-frontend`; use `FRONTEND_DIR` para outro caminho. Se o script do frontend não existir, avisa e gera só o backend.
- Variáveis: `TAG`, `OUTPUT_DIR` (padrão `<library-backend>/images`), `FRONTEND_DIR`, `BACKEND_IMAGE_NAME`, `FRONTEND_IMAGE_NAME`, `VITE_API_URL`.
- No resumo final lista como "gerados nesta execução" só os arquivos novos e avisa quando o diretório tem arquivos de execuções anteriores.

Exemplo do conteúdo de `images/` depois de um build:

```text
library-backend_853ed4a-dirty.tar.gz
library-backend_853ed4a-dirty.tar.gz.sha256
biblioteca-web-frontend_2e2f063-dirty.tar.gz
biblioteca-web-frontend_2e2f063-dirty.tar.gz.sha256
```

### Conferir o checksum

O `.sha256` usa o nome relativo do arquivo:

```bash
cd images
sha256sum -c library-backend_853ed4a-dirty.tar.gz.sha256
```

## Levar as imagens para outra máquina

Na máquina de origem, gere as imagens (`./scripts/build-all-images.sh`). Copie para a máquina de destino (pendrive, `scp`, pasta compartilhada):

- a pasta `images/` com os `.tar.gz` e os `.sha256`;
- `scripts/load-images.sh`;
- `docker-compose.yml`.

Na máquina de destino (só é preciso ter o Docker):

```bash
./load-images.sh ./images        # confere os checksums e executa docker load em cada arquivo
docker compose up -d             # usa as imagens carregadas, sem rebuildar
```

O `load-images.sh`:

- confere o `.sha256` de cada arquivo antes de carregar; divergência interrompe o script sem carregar (arquivo sem `.sha256` só gera um aviso);
- usa, sem argumento, o diretório da variável `OUTPUT_DIR` ou `<repo>/images`;
- ao final, reaponta a tag `latest` de cada imagem para o build **mais recente** (pela data de criação), mesmo que o diretório tenha vários arquivos da mesma imagem. Sem isso, o `docker load` deixaria `latest` apontando para o último arquivo em ordem alfabética, e o compose subiria uma versão antiga.

Esse último passo foi testado: com as duas imagens já presentes no Docker (como ficam depois do `load-images.sh`) e só o `docker-compose.yml` numa pasta sem o código-fonte (sem o repositório irmão do frontend), `docker compose up -d` não tenta buildar, o backend fica `healthy` e o frontend responde em `/` e em `/api/books`.

## Rodar sem o compose

Equivale ao que o compose faz. O backend precisa ser alcançável pelo nome `library-backend` na rede do frontend (padrão de `API_UPSTREAM`), e o DNS embutido do Docker só funciona em redes definidas pelo usuário:

```bash
docker network create biblioteca-net

docker run -d --name library-backend --network biblioteca-net \
  -p 8080:8080 library-backend:latest

docker run -d --name biblioteca-web-frontend --network biblioteca-net \
  -p 8081:80 biblioteca-web-frontend:latest

# Para encerrar:
docker rm -f biblioteca-web-frontend library-backend
docker network rm biblioteca-net
```

Se uma página de outra origem chamar a API diretamente (sem passar pelo `/api` do Nginx), inclua a origem em `APP_CORS_ALLOWED_ORIGINS` (por exemplo `-e APP_CORS_ALLOWED_ORIGINS=http://localhost:8081` no `docker run` do backend). Pelo `/api` do Nginx isso não é necessário.

## Fuso horário

A data de "hoje" do backend define o status dos empréstimos (`ATRASADO`), a data padrão da devolução e as datas relativas do seed. Contêineres rodam em UTC por padrão; entre 21h e 24h (Brasília) o "hoje" em UTC já é o dia seguinte, o que deslocaria tudo isso em um dia.

Por isso o `Dockerfile` define `ENV TZ=America/Sao_Paulo` e o compose usa `TZ: ${BACKEND_TZ:-America/Sao_Paulo}`. Para outro fuso, `BACKEND_TZ=Europe/Lisbon docker compose up -d`. Fora do Docker, vale o fuso do sistema. Conferência rápida:

```bash
docker compose exec library-backend date
```

Mais detalhes em [regras de negócio](../regras/regras-de-negocio.md#datas-fuso-e-relógio).

## Problemas comuns

| Sintoma | Causa provável | O que fazer |
| --- | --- | --- |
| `docker compose up --build` falha no frontend por falta de contexto | O repositório do frontend não está em `../biblioteca-web-frontend` | Clone-o ao lado do backend, ou carregue as imagens prontas e use `up -d` sem `--build` |
| Porta já em uso | Outra aplicação (por exemplo o backend rodando pelo IntelliJ na 8080) | Mude `BACKEND_PORT` / `FRONTEND_PORT` |
| Escritas pelo frontend dão 403 "Origem não permitida" | O frontend está falando direto com a API a partir de uma origem fora de `APP_CORS_ALLOWED_ORIGINS` | Use o `/api` do Nginx (que remove o `Origin`) ou inclua a origem na variável |
| Frontend abre, mas `/api` responde 502 ou 504 | Backend fora do ar ou destino incorreto em `API_UPSTREAM` | `docker compose ps` e `docker compose logs library-backend` |
| Compose sobe uma versão antiga | `latest` aponta para uma imagem antiga (vários arquivos carregados) | Rode de novo o `load-images.sh` (ele reaponta `latest`) ou apague os arquivos antigos de `images/` |
| Datas deslocadas em um dia à noite | Backend rodando em UTC | Confirme `TZ`/`BACKEND_TZ` (veja acima) |
| `/h2-console` dá 404 | O console do H2 não está disponível neste projeto | Veja o [README](../../README.md#console-do-h2) |
