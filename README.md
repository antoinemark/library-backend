# Library Backend

API REST do sistema de empréstimo de livros: usuários, acervo (livros, autores e gêneros) e empréstimos com bloqueio automático para quem tem pendências. A especificação original está em [Documento de Análise (PDF)](docs/Documento_de_An%C3%A1lise_-_Sistema_de_Empr%C3%A9stimo.pdf).

O frontend fica no repositório irmão [`biblioteca-web-frontend`](../biblioteca-web-frontend/README.md).

Quer só subir a API, sem instalar Java nem Maven? Veja [Rodando com Docker](#rodando-com-docker).

## Sumário

- [Stack](#stack)
- [Rodando localmente](#rodando-localmente)
- [Dados iniciais (seed)](#dados-iniciais-seed)
- [Endpoints](#endpoints)
- [Regras principais](#regras-principais)
- [Testes](#testes)
- [Rodando com Docker](#rodando-com-docker)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Console do H2](#console-do-h2)
- [Documentação](#documentação)

## Stack

- Java 21 e Spring Boot 4.1.1 (Web MVC, Data JPA, Validation)
- H2 em memória
- Lombok
- springdoc-openapi (Swagger UI)

## Rodando localmente

Pré-requisitos: Java 21 e Maven (o `mvnw` também está no repositório).

```bash
mvn spring-boot:run
```

Ou, no IntelliJ, rode a classe `LibraryApplication`.

A API sobe em `http://localhost:8080`:

| O quê | URL |
| --- | --- |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI (JSON) | `http://localhost:8080/v3/api-docs` |

O CORS libera `http://localhost:5173` e `http://localhost:4173` (o Vite em desenvolvimento e em `preview`). Para liberar outras origens, defina `app.cors.allowed-origins` (lista separada por vírgula) ou a variável de ambiente `APP_CORS_ALLOWED_ORIGINS`.

## Dados iniciais (seed)

O banco é o H2 em memória: a cada subida o schema é recriado e o arquivo [`data.sql`](src/main/resources/data.sql) é executado. Nada persiste entre execuções.

| Entidade | Quantidade |
| --- | --- |
| Autores | 12 |
| Gêneros | 9 |
| Livros | 20 |
| Usuários | 10 |
| Empréstimos | 14 (4 `ATIVO`, 3 `ATRASADO`, 7 `CONCLUIDO`) |

- As datas dos empréstimos são relativas a `CURRENT_DATE`. Por isso, em qualquer dia que a aplicação subir, existem empréstimos ativos, atrasados e concluídos.
- Os usuários com pendência (`debtFree = false`) são Bruno Oliveira, Elena Rostova e Heitor Costa.
- Todo autor do seed tem pelo menos um livro.

Mais detalhes em [modelo e seed](docs/banco/modelo-e-seed.md).

## Endpoints

| Recurso | Rotas |
| --- | --- |
| Autores | `GET/POST /authors`, `GET/PUT/DELETE /authors/{id}` |
| Gêneros | `GET/POST /genres`, `GET/PUT/DELETE /genres/{id}` |
| Livros | `GET/POST /books`, `GET/PUT/DELETE /books/{id}` |
| Usuários | `GET/POST /users`, `GET/PUT/DELETE /users/{id}` |
| Empréstimos | `GET/POST /lendings` (filtros `userId` e `status`), `GET /lendings/{id}`, `PATCH /lendings/{id}/return` |

- Datas sempre no formato `YYYY-MM-DD`.
- Erros 4xx têm o corpo `{ status, error, message, fields, timestamp }`.
- Não existe `DELETE` de empréstimo: o histórico é preservado.

Exemplos de request e response de cada rota em [endpoints](docs/api/endpoints.md).

## Regras principais

- **Pendência:** um usuário com empréstimo em aberto e com a data prevista de devolução vencida não pode fazer novos empréstimos (`409`).
- **Disponibilidade:** um livro que já tem empréstimo em aberto não pode ser emprestado de novo (`409`).
- **Status do empréstimo** (derivado, não gravado): `CONCLUIDO` se foi devolvido; senão `ATRASADO` se hoje passou da data prevista; senão `ATIVO`.
- **Exclusões bloqueadas** (`409`): autor ou gênero com livros; livro ou usuário com empréstimos registrados.
- **CPF:** aceita com ou sem máscara, é gravado só com os 11 dígitos e é único.
- **Autor sem livros** pode ser cadastrado, porque o autor precisa existir antes do livro. O seed garante pelo menos um livro por autor, conforme a especificação.

Todas as regras, com a ordem das validações, em [regras de negócio](docs/regras/regras-de-negocio.md).

## Testes

```bash
mvn test
```

São 77 testes de integração e unitários (MockMvc sobre o H2), cobrindo os controllers, as regras de empréstimo, o seed e o formato de erro e CORS.

## Rodando com Docker

Com o Docker não é preciso instalar Java nem Maven. O `Dockerfile` e o `docker-compose.yml` deste repositório cuidam só da API. O frontend tem os próprios, no repositório irmão `biblioteca-web-frontend`. Os dois containers ficam separados e se encontram pela rede Docker `biblioteca-net`.

### Subir a API (docker compose up)

```bash
docker network create biblioteca-net   # uma vez por máquina; se ela já existir, o erro pode ser ignorado
docker compose up -d --build           # builda a imagem e sobe a API
```

A API fica em `http://localhost:8080` (Swagger em `http://localhost:8080/swagger-ui/index.html`).

Comandos úteis, rodados na pasta deste repositório:

| Comando | Para que serve |
| --- | --- |
| `docker compose up -d` | Sobe usando a imagem que já existe (só builda se ela faltar). |
| `docker compose up -d --build` | Rebuilda a imagem e sobe. Use depois de mudar o código. |
| `docker compose ps` | Mostra o estado e o health do container. |
| `docker compose logs -f` | Acompanha os logs. |
| `docker compose down` | Derruba a API (os dados do H2 somem). |

Para publicar em outra porta: `BACKEND_PORT=9080 docker compose up -d`. Outras variáveis (ambiente ou `.env` ao lado do compose): `APP_CORS_ALLOWED_ORIGINS`, `JAVA_OPTS` e `BACKEND_TZ` (padrão `America/Sao_Paulo`).

### Subir o sistema completo (API + site)

Os dois repositórios lado a lado, cada um com o próprio compose:

```bash
docker network create biblioteca-net                              # uma vez por máquina
docker compose up -d --build                                      # aqui: API em http://localhost:8080
(cd ../biblioteca-web-frontend && docker compose up -d --build)   # site em http://localhost:8081
```

A ordem não importa. Os dados ficam em memória: voltam ao padrão (seed) a cada subida. Para derrubar, rode `docker compose down` nos dois repositórios.

### Gerar a imagem e levar para outra máquina

```bash
./scripts/build-image.sh     # gera images/library-backend_<tag>.tar.gz e o .sha256
./scripts/load-images.sh     # na outra máquina: confere o checksum e carrega (docker load)
docker network create biblioteca-net
docker compose up -d         # usa a imagem carregada, sem rebuildar
```

Copie para a outra máquina a pasta `images/`, o `docker-compose.yml` e o `scripts/load-images.sh`.

Variáveis do compose, fuso horário e problemas comuns: [Docker do backend](docs/docker/imagens-e-stack.md).

## Estrutura do projeto

Pacote base: `com.anhanguera.library` (em `src/main/java`).

| Pacote | Conteúdo |
| --- | --- |
| `controller` | Controllers REST, um por recurso |
| `service` | Regras de negócio e conversão entidade para DTO |
| `repository` | Repositórios Spring Data JPA |
| `model` | Entidades JPA e o enum `LendingStatus` |
| `dtos` | Records de request e response, uma subpasta por recurso |
| `exception` | Exceções de negócio, `ApiError` e o `GlobalExceptionHandler` |
| `config` | CORS e o `Clock` da aplicação |
| `validation` | Validador de CPF |
| `util` | Ordenação alfabética pt-BR e normalização de texto |

## Console do H2

O console do H2 **não está disponível**: `/h2-console` responde 404. As propriedades `spring.h2.console.*` estão no `application.properties`, mas, conforme o comentário ali, no Spring Boot 4 o console depende do módulo `spring-boot-h2console`, que não está no `pom.xml`. Para consultar os dados, use a própria API (Swagger UI) ou os testes.

## Documentação

- [Endpoints](docs/api/endpoints.md)
- [Regras de negócio](docs/regras/regras-de-negocio.md)
- [Modelo de dados e seed](docs/banco/modelo-e-seed.md)
- [Docker do backend](docs/docker/imagens-e-stack.md)
- [Documento de Análise (PDF)](docs/Documento_de_An%C3%A1lise_-_Sistema_de_Empr%C3%A9stimo.pdf)
