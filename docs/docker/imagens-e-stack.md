# Docker do backend

Como a API vira uma imagem Docker, como subi-la com o `docker-compose.yml` deste repositório, como levar a imagem para outra máquina e como ligá-la ao frontend.

O backend e o frontend são **projetos separados**: cada repositório tem o próprio `Dockerfile`, o próprio `docker-compose.yml` e o próprio `scripts/build-image.sh`. Nenhum builda o outro. Os dois containers se encontram por uma rede Docker compartilhada, a `biblioteca-net`.

## Sumário

- [Visão geral](#visão-geral)
- [Subir a API](#subir-a-api)
- [Variáveis do compose](#variáveis-do-compose)
- [Dockerfile](#dockerfile)
- [Scripts de imagem](#scripts-de-imagem)
- [Levar a imagem para outra máquina](#levar-a-imagem-para-outra-máquina)
- [Subir junto com o frontend](#subir-junto-com-o-frontend)
- [Rodar sem o compose](#rodar-sem-o-compose)
- [Fuso horário](#fuso-horário)
- [Problemas comuns](#problemas-comuns)

## Visão geral

| Imagem | Base de runtime | Porta interna | Porta no host (padrão) |
| --- | --- | --- | --- |
| `library-backend` | `eclipse-temurin:21-jre-alpine` | 8080 | 8080 (`BACKEND_PORT`) |

```text
rede Docker "biblioteca-net"
  ├── library-backend            (este repositório)         publica 8080
  └── biblioteca-web-frontend    (repositório do frontend)  publica 8081, proxy /api ──► library-backend:8080
```

O banco é o H2 em memória: ao derrubar o container, os dados somem e voltam ao padrão (seed) na próxima subida.

## Subir a API

A rede é externa nos dois compose; crie-a uma única vez:

```bash
docker network create biblioteca-net
```

A partir da raiz do repositório:

```bash
docker compose up -d --build   # builda a imagem e sobe a API
docker compose up -d           # reaproveita a imagem existente (ex.: carregada do .tar.gz)
docker compose logs -f         # acompanha os logs
docker compose ps              # estado e health
docker compose down            # derruba a API (os dados do H2 somem)
```

A API fica em `http://localhost:8080` (Swagger em `http://localhost:8080/swagger-ui/index.html`).

Detalhes do `docker-compose.yml`:

- Projeto `library-backend` (campo `name:`), com um serviço `library-backend`: imagem `library-backend:latest`, `build: .`, `restart: unless-stopped`. O healthcheck vem do Dockerfile.
- O serviço entra na rede externa `biblioteca-net`. O nome do serviço (`library-backend`) é o endereço que os outros containers da rede usam: `http://library-backend:8080`.
- Com `image:` e `build:`, `up -d` sem `--build` usa a imagem local se ela existir e só builda se faltar.

## Variáveis do compose

Defina no ambiente ou em um arquivo `.env` ao lado do `docker-compose.yml`. Todas são opcionais.

| Variável | Padrão | Efeito |
| --- | --- | --- |
| `BACKEND_PORT` | `8080` | Porta do host para a API |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:4173` | Origens liberadas no CORS (vira a propriedade `app.cors.allowed-origins`). O frontend em container não precisa estar na lista: o Nginx dele repassa `/api` sem o header `Origin` |
| `JAVA_OPTS` | vazio | Opções extras da JVM (ex.: `-Xmx512m`) |
| `BACKEND_TZ` | `America/Sao_Paulo` | Fuso do backend, que define o "hoje" (veja [Fuso horário](#fuso-horário)) |

Exemplo, em outra porta:

```bash
BACKEND_PORT=18080 docker compose up -d
```

## Dockerfile

Arquivo: `Dockerfile` (raiz do repositório). Dois estágios:

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

Para buildar e rodar só a imagem, sem o compose:

```bash
docker build -t library-backend .
docker run --rm -p 8080:8080 library-backend
```

## Scripts de imagem

Funcionam de qualquer diretório de trabalho (partem da raiz do repositório) e exigem `docker` com o daemon rodando, `gzip` e `sha256sum` (ou `shasum`). Todos têm `--help`.

| Script | Para que serve |
| --- | --- |
| `scripts/build-image.sh [TAG]` | Builda a imagem e salva em `.tar.gz` |
| `scripts/load-images.sh [DIRETORIO]` | Carrega no Docker (`docker load`) todos os `.tar.gz` de um diretório |

### Gerar a imagem

```bash
./scripts/build-image.sh                    # tag automática
./scripts/build-image.sh 1.0.0              # tag explícita
OUTPUT_DIR=/tmp/imagens ./scripts/build-image.sh demo
```

Regras do `build-image.sh`:

- **Tag:** o argumento; senão a variável `TAG`; senão `git describe --always --dirty` (por exemplo `853ed4a-dirty`; caracteres inválidos viram `-`); sem git, `dev`. A imagem recebe também a tag `latest`. A tag precisa casar com `^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$`.
- **Saída:** `<OUTPUT_DIR>/<IMAGE_NAME>_<TAG>.tar.gz` (`docker save` + `gzip`) e, ao lado, `<arquivo>.tar.gz.sha256`. `OUTPUT_DIR` padrão: `<repo>/images`, que está no `.gitignore` e no `.dockerignore`.
- **Nome padrão:** `library-backend` (`IMAGE_NAME` troca).
- **Labels:** `org.opencontainers.image.version`, `created` e (com git) `revision`.
- **Variáveis:** `TAG`, `IMAGE_NAME`, `OUTPUT_DIR`.

Exemplo do conteúdo de `images/` depois de um build:

```text
library-backend_853ed4a-dirty.tar.gz
library-backend_853ed4a-dirty.tar.gz.sha256
```

### Conferir o checksum

O `.sha256` usa o nome relativo do arquivo:

```bash
cd images
sha256sum -c library-backend_853ed4a-dirty.tar.gz.sha256
```

## Levar a imagem para outra máquina

Na máquina de origem, gere a imagem (`./scripts/build-image.sh`). Copie para a máquina de destino a pasta `images/` (`.tar.gz` e `.sha256`), o `scripts/load-images.sh` e o `docker-compose.yml`. No destino só é preciso ter o Docker:

```bash
./load-images.sh ./images           # confere os checksums e executa docker load em cada arquivo
docker network create biblioteca-net
docker compose up -d                # usa a imagem carregada, sem rebuildar
```

O `load-images.sh`:

- confere o `.sha256` de cada arquivo antes de carregar; divergência interrompe o script sem carregar (arquivo sem `.sha256` só gera um aviso);
- usa, sem argumento, o diretório da variável `OUTPUT_DIR` ou `<repo>/images`;
- ao final, reaponta a tag `latest` de cada imagem para o build **mais recente** (pela data de criação), mesmo que o diretório tenha vários arquivos da mesma imagem. Sem isso, o `docker load` deixaria `latest` apontando para o último arquivo em ordem alfabética, e o compose subiria uma versão antiga.

Sem o script, o equivalente é `docker load -i library-backend_<TAG>.tar.gz`.

## Subir junto com o frontend

Cada projeto sobe pelo próprio compose, na mesma rede:

```bash
docker network create biblioteca-net                       # uma vez

# neste repositório (API em http://localhost:8080)
docker compose up -d --build

# no repositório biblioteca-web-frontend (site em http://localhost:8081)
docker compose up -d --build
```

O Nginx do frontend repassa `/api/...` para `http://library-backend:8080`, removendo o prefixo `/api`. A ordem não importa: o frontend sobe mesmo sem a API e as chamadas `/api` respondem 502 até ela ficar de pé. Para derrubar, rode `docker compose down` em cada repositório.

## Rodar sem o compose

Equivale ao que o compose faz. O nome `library-backend` precisa resolver na rede do outro container, e o DNS embutido do Docker só funciona em redes definidas pelo usuário:

```bash
docker network create biblioteca-net
docker run -d --name library-backend --network biblioteca-net -p 8080:8080 library-backend:latest

# Para encerrar:
docker rm -f library-backend
docker network rm biblioteca-net
```

Se uma página de outra origem chamar a API diretamente (sem passar pelo `/api` do Nginx do frontend), inclua a origem em `APP_CORS_ALLOWED_ORIGINS` (por exemplo `-e APP_CORS_ALLOWED_ORIGINS=http://localhost:8081` no `docker run`).

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
| `docker compose up` falha com "network biblioteca-net declared as external, but could not be found" | A rede compartilhada ainda não foi criada | `docker network create biblioteca-net` (uma vez) |
| Porta já em uso | Outra aplicação (por exemplo o backend rodando pelo IntelliJ na 8080) | Mude `BACKEND_PORT` |
| Escritas direto na API dão 403 "Origem não permitida" | A página que chama a API está em uma origem fora de `APP_CORS_ALLOWED_ORIGINS` | Passe pelo `/api` do Nginx do frontend (que remove o `Origin`) ou inclua a origem na variável |
| O frontend abre, mas `/api` responde 502 ou 504 | O container da API não está de pé ou não está na rede `biblioteca-net` | `docker compose ps` e `docker compose logs library-backend` |
| Compose sobe uma versão antiga | `latest` aponta para uma imagem antiga (vários arquivos carregados) | Rode de novo o `load-images.sh` (ele reaponta `latest`) ou apague os arquivos antigos de `images/` |
| Datas deslocadas em um dia à noite | Backend rodando em UTC | Confirme `TZ`/`BACKEND_TZ` (veja acima) |
| `/h2-console` dá 404 | O console do H2 não está disponível neste projeto | Veja o [README](../../README.md#console-do-h2) |
