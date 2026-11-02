# Contrato da API REST

Referência completa das rotas do backend (`library-backend`): corpos de requisição e resposta, códigos HTTP, formato de erro, filtros, CORS e header `Location`.

Os exemplos de resposta foram obtidos rodando a aplicação com o seed original (veja [modelo-e-seed.md](../banco/modelo-e-seed.md)). As datas dos empréstimos são relativas ao dia em que a aplicação sobe; os exemplos abaixo foram gerados em 29/10/2026.

A documentação interativa (Swagger UI) está em `http://localhost:8080/swagger-ui/index.html` e o documento OpenAPI em `http://localhost:8080/v3/api-docs`. Em caso de divergência entre este arquivo e o código, vale o código.

## Sumário

- [Convenções gerais](#convenções-gerais)
- [Resumo das rotas](#resumo-das-rotas)
- [Formato de erro (ApiError)](#formato-de-erro-apierror)
- [Autores](#autores)
- [Gêneros](#gêneros)
- [Livros](#livros)
- [Usuários](#usuários)
- [Empréstimos](#empréstimos)
- [CORS](#cors)
- [Header Location e proxy](#header-location-e-proxy)
- [Limitações conhecidas](#limitações-conhecidas)

## Convenções gerais

| Item | Valor |
| --- | --- |
| URL base (direto no backend) | `http://localhost:8080` (sem prefixo; as rotas começam em `/authors`, `/genres`...) |
| URL base (pelo frontend na stack Docker) | `http://localhost:8081/api` (o Nginx remove o prefixo `/api` antes de repassar ao backend) |
| Formato | JSON em UTF-8 (`Content-Type: application/json` nas requisições com corpo) |
| Ids | Numéricos (`Long`), gerados pelo banco |
| Datas | Strings ISO `YYYY-MM-DD` (ex.: `"2026-10-29"`), sem hora e sem fuso. Outro formato resulta em 400 |
| Autenticação | Não existe |
| Paginação | Não existe: cada listagem devolve todos os registros |
| Ordem das listagens | Autores, gêneros, livros (por título) e usuários em ordem alfabética pt-BR (veja [regras de negócio](../regras/regras-de-negocio.md#ordenação-alfabética-pt-br)); empréstimos do mais recente para o mais antigo |
| Mensagens de erro | Sempre em português |

Códigos de sucesso: `200` (GET, PUT, PATCH), `201` (POST, com header `Location` e o recurso criado no corpo) e `204` (DELETE, sem corpo).

`PUT` substitui o recurso inteiro: envie os mesmos campos do `POST`, todos obrigatórios.

## Resumo das rotas

| Método | Rota | O que faz | Sucesso | Erros possíveis |
| --- | --- | --- | --- | --- |
| GET | `/authors` | Lista os autores com a quantidade de livros | 200 | |
| GET | `/authors/{id}` | Busca um autor | 200 | 400, 404 |
| POST | `/authors` | Cadastra um autor | 201 | 400 |
| PUT | `/authors/{id}` | Atualiza um autor | 200 | 400, 404 |
| DELETE | `/authors/{id}` | Exclui um autor sem livros | 204 | 404, 409 |
| GET | `/genres` | Lista os gêneros com a quantidade de livros | 200 | |
| GET | `/genres/{id}` | Busca um gênero | 200 | 400, 404 |
| POST | `/genres` | Cadastra um gênero | 201 | 400 |
| PUT | `/genres/{id}` | Atualiza um gênero | 200 | 400, 404 |
| DELETE | `/genres/{id}` | Exclui um gênero sem livros | 204 | 404, 409 |
| GET | `/books` | Lista os livros (por título) com a disponibilidade | 200 | |
| GET | `/books/{id}` | Busca um livro | 200 | 400, 404 |
| POST | `/books` | Cadastra um livro | 201 | 400, 404 |
| PUT | `/books/{id}` | Atualiza um livro | 200 | 400, 404 |
| DELETE | `/books/{id}` | Exclui um livro sem empréstimos | 204 | 404, 409 |
| GET | `/users` | Lista os usuários com o indicador de pendência | 200 | |
| GET | `/users/{id}` | Busca um usuário | 200 | 400, 404 |
| POST | `/users` | Cadastra um usuário | 201 | 400, 409 |
| PUT | `/users/{id}` | Atualiza um usuário | 200 | 400, 404, 409 |
| DELETE | `/users/{id}` | Exclui um usuário sem empréstimos | 204 | 404, 409 |
| GET | `/lendings` | Lista os empréstimos (filtros `userId` e `status`) | 200 | 400, 404 |
| GET | `/lendings/{id}` | Busca um empréstimo | 200 | 400, 404 |
| POST | `/lendings` | Registra um empréstimo | 201 | 400, 404, 409 |
| PATCH | `/lendings/{id}/return` | Registra a devolução | 200 | 400, 404, 409 |
| GET | `/swagger-ui/index.html` | Swagger UI | 200 | |
| GET | `/v3/api-docs` | Documento OpenAPI (JSON) | 200 | |

Não existe `DELETE` nem `PUT` em `/lendings`: o histórico de empréstimos nunca é apagado nem editado (essas chamadas respondem 405). Também não existe rota de console do H2 (`/h2-console` responde 404; veja o [README](../../README.md#console-do-h2)).

## Formato de erro (ApiError)

Todo erro (inclusive 404 de rota, 405 e 415) usa o mesmo corpo JSON:

| Campo | Tipo | Descrição |
| --- | --- | --- |
| `status` | número | Código HTTP |
| `error` | string | Descrição padrão do código (`Bad Request`, `Not Found`, `Conflict`...) |
| `message` | string | Mensagem legível, em português |
| `fields` | objeto ou `null` | Erro por campo (`{"campo": "mensagem"}`), preenchido nos 400 de validação. Nos demais erros é `null` |
| `timestamp` | string | Instante do erro, ISO-8601 em UTC |

Exemplo (400 com um único campo inválido):

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "O nome do autor é obrigatório.",
  "fields": { "name": "O nome do autor é obrigatório." },
  "timestamp": "2026-10-30T00:36:28.366225958Z"
}
```

Com mais de um campo inválido, `message` vira `"Há campos inválidos na requisição. Verifique os dados informados."` e os detalhes ficam em `fields`:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Há campos inválidos na requisição. Verifique os dados informados.",
  "fields": {
    "authorId": "O autor é obrigatório.",
    "publisher": "A editora é obrigatória.",
    "publishDate": "A data de publicação não pode ser futura.",
    "genreId": "O gênero é obrigatório.",
    "title": "O título deve ter entre 2 e 255 caracteres."
  },
  "timestamp": "2026-10-30T00:36:28.463293570Z"
}
```

A ordem das chaves de `fields` não é garantida. Os nomes dos campos são os do JSON da API (por exemplo `publishDate`, não `publicationDate`, que é o nome usado no frontend).

### Quando cada código aparece

| Código | Situação | Exemplo de `message` |
| --- | --- | --- |
| 400 | Falha de validação do corpo (campo ausente, vazio, grande demais, CPF inválido...) | `O nome é obrigatório.` |
| 400 | Data fora do formato `AAAA-MM-DD` | `Data inválida. Use o formato AAAA-MM-DD.` (campo em `fields`) |
| 400 | Valor não numérico em campo numérico do corpo | `O valor informado deve ser numérico.` (campo em `fields`) |
| 400 | JSON malformado ou corpo ausente | `O corpo da requisição está ausente ou não é um JSON válido.` |
| 400 | Id da URL ou filtro com tipo inválido | `Valor inválido para o parâmetro 'id': 'abc'.` |
| 400 | Filtro `status` desconhecido | `Valor inválido para o parâmetro 'status': 'XYZ'. Valores aceitos: ATIVO, ATRASADO, CONCLUIDO.` |
| 400 | Regra de data do empréstimo ou da devolução | `A data do empréstimo não pode ser futura.` (campo em `fields`) |
| 403 | Origem não liberada no CORS | `Origem não permitida pelo CORS da API. Acesse por uma das origens liberadas.` |
| 404 | Recurso inexistente | `Livro não encontrado.` |
| 404 | Rota inexistente | `Recurso não encontrado.` |
| 405 | Método não suportado na rota (vem com o header `Allow`) | `Método HTTP não suportado para este recurso.` |
| 409 | Regra de negócio ou conflito de estado | `Este livro já está emprestado.` |
| 409 | Violação de integridade do banco (rede de segurança) | `A operação não pode ser concluída porque viola uma restrição de integridade dos dados.` |
| 415 | `Content-Type` diferente de JSON | `Tipo de conteúdo não suportado. Envie os dados em JSON.` |
| 500 | Erro inesperado (o detalhe fica só no log do servidor) | `Ocorreu um erro inesperado no servidor. Tente novamente mais tarde.` |

Exemplo de 404 de recurso:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Livro não encontrado.",
  "fields": null,
  "timestamp": "2026-10-30T00:36:28.987723443Z"
}
```

Mensagens de 404: `Autor não encontrado.`, `Gênero não encontrado.`, `Livro não encontrado.`, `Usuário não encontrado.`, `Empréstimo não encontrado.`

## Autores

### Campos

Requisição (`POST` e `PUT`):

| Campo | Tipo | Regras |
| --- | --- | --- |
| `name` | string | Obrigatório, até 255 caracteres. Espaços das pontas são removidos antes da validação (inclusive espaço não separável) |

Resposta:

| Campo | Tipo | Descrição |
| --- | --- | --- |
| `id` | número | Id do autor |
| `name` | string | Nome |
| `bookCount` | número | Quantidade de livros do autor (calculada) |

Mensagens de validação: `O nome do autor é obrigatório.`, `O nome do autor deve ter no máximo 255 caracteres.`

### GET /authors

Ordenados por nome em ordem alfabética pt-BR. Primeiros itens do seed:

```json
[
  { "id": 8, "name": "Agatha Christie", "bookCount": 2 },
  { "id": 10, "name": "Aldous Huxley", "bookCount": 1 },
  { "id": 12, "name": "Cecília Meireles", "bookCount": 1 },
  { "id": 2, "name": "Clarice Lispector", "bookCount": 2 }
]
```

### POST /authors

```http
POST /authors
Content-Type: application/json

{ "name": "Lygia Fagundes Telles" }
```

Resposta `201`, com `Location: http://localhost:8080/authors/13`:

```json
{ "id": 13, "name": "Lygia Fagundes Telles", "bookCount": 0 }
```

Um autor pode ser cadastrado sem livros (é preciso que ele exista antes do primeiro livro). Veja o [desvio da regra 1..*](../specs/conformidade.md#desvios-justificados).

### PUT /authors/{id}

Mesmo corpo do `POST`; devolve `200` com o autor atualizado. Id inexistente: `404`.

### DELETE /authors/{id}

`204` sem corpo. Se o autor tem livros: `409`.

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Não é possível excluir o autor, pois ele possui livros cadastrados.",
  "fields": null,
  "timestamp": "2026-10-30T00:36:28.850823094Z"
}
```

## Gêneros

### Campos

Requisição (`POST` e `PUT`):

| Campo | Tipo | Regras |
| --- | --- | --- |
| `name` | string | Obrigatório, até 255 caracteres, espaços das pontas removidos |

Resposta: `id`, `name` e `bookCount` (quantidade de livros do gênero).

Mensagens de validação: `O nome do gênero é obrigatório.`, `O nome do gênero deve ter no máximo 255 caracteres.`

### GET /genres

Ordenados por nome (pt-BR). Resposta completa do seed:

```json
[
  { "id": 5, "name": "Distopia", "bookCount": 3 },
  { "id": 4, "name": "Fantasia", "bookCount": 2 },
  { "id": 2, "name": "Ficção Científica", "bookCount": 2 },
  { "id": 3, "name": "Literatura Brasileira", "bookCount": 5 },
  { "id": 6, "name": "Mistério e Suspense", "bookCount": 2 },
  { "id": 9, "name": "Poesia", "bookCount": 1 },
  { "id": 7, "name": "Realismo Mágico", "bookCount": 1 },
  { "id": 1, "name": "Romance", "bookCount": 3 },
  { "id": 8, "name": "Terror", "bookCount": 1 }
]
```

### POST, PUT e DELETE

Funcionam como em autores. Exemplo de `POST /genres` com `{ "name": "Crônica" }`: `201` e

```json
{ "id": 10, "name": "Crônica", "bookCount": 0 }
```

`DELETE` de gênero com livros devolve `409`: `Não é possível excluir o gênero, pois existem livros cadastrados nele.`

## Livros

### Campos

Requisição (`POST` e `PUT`):

| Campo | Tipo | Regras |
| --- | --- | --- |
| `title` | string | Obrigatório, de 2 a 255 caracteres (contados depois de remover os espaços das pontas) |
| `authorId` | número | Obrigatório; o autor precisa existir (senão `404`) |
| `genreId` | número | Obrigatório; o gênero precisa existir (senão `404`) |
| `publishDate` | data ISO | Obrigatória; não pode ser futura (hoje é aceito) |
| `publisher` | string | Obrigatória, até 255 caracteres, espaços das pontas removidos |

Resposta:

| Campo | Tipo | Descrição |
| --- | --- | --- |
| `id` | número | Id do livro |
| `title` | string | Título |
| `author` | objeto | `{ "id", "name" }` |
| `genre` | objeto | `{ "id", "name" }` |
| `publishDate` | data ISO | Data de publicação |
| `publisher` | string | Editora |
| `available` | booleano | `true` quando o livro não tem empréstimo em aberto (calculado) |

Mensagens de validação: `O título é obrigatório.`, `O título deve ter entre 2 e 255 caracteres.`, `O autor é obrigatório.`, `O gênero é obrigatório.`, `A data de publicação é obrigatória.`, `A data de publicação não pode ser futura.`, `A editora é obrigatória.`, `A editora deve ter no máximo 255 caracteres.`

### GET /books e GET /books/{id}

Ordenados por título (pt-BR). Exemplo de `GET /books/1` (livro emprestado):

```json
{
  "id": 1,
  "title": "Dom Casmurro",
  "author": { "id": 1, "name": "Machado de Assis" },
  "genre": { "id": 3, "name": "Literatura Brasileira" },
  "publishDate": "1899-01-01",
  "publisher": "Livraria Garnier",
  "available": false
}
```

### POST /books

```http
POST /books
Content-Type: application/json

{
  "title": "Ciranda de Pedra",
  "authorId": 13,
  "genreId": 1,
  "publishDate": "1954-01-01",
  "publisher": "O Cruzeiro"
}
```

Resposta `201` com `Location: http://localhost:8080/books/21`:

```json
{
  "id": 21,
  "title": "Ciranda de Pedra",
  "author": { "id": 13, "name": "Lygia Fagundes Telles" },
  "genre": { "id": 1, "name": "Romance" },
  "publishDate": "1954-01-01",
  "publisher": "O Cruzeiro",
  "available": true
}
```

Data fora do formato (`"publishDate": "01/02/1954"`) devolve `400`:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Data inválida. Use o formato AAAA-MM-DD.",
  "fields": { "publishDate": "Data inválida. Use o formato AAAA-MM-DD." },
  "timestamp": "2026-10-30T00:36:28.488779723Z"
}
```

### PUT /books/{id}

Mesmo corpo do `POST`. É permitido trocar autor e gênero, inclusive de livros com histórico. Devolve `200` com o livro atualizado.

### DELETE /books/{id}

`204` sem corpo. Livro com qualquer empréstimo registrado (mesmo já devolvido) devolve `409`: `Não é possível excluir o livro, pois ele possui empréstimos registrados.`

## Usuários

### Campos

Requisição (`POST` e `PUT`):

| Campo | Tipo | Regras |
| --- | --- | --- |
| `name` | string | Obrigatório, até 255 caracteres, espaços das pontas removidos |
| `cpf` | string | Obrigatório. Aceita 11 dígitos com ou sem máscara (`123.456.789-01` ou `12345678901`). Letras e outros símbolos são recusados. Os dígitos verificadores não são conferidos. É gravado só com dígitos e deve ser único |

Resposta:

| Campo | Tipo | Descrição |
| --- | --- | --- |
| `id` | número | Id do usuário |
| `name` | string | Nome |
| `cpf` | string | CPF só com dígitos |
| `debtFree` | booleano | `false` quando o usuário tem empréstimo em aberto com a devolução prevista vencida (calculado) |

Mensagens: `O nome é obrigatório.`, `O nome deve ter no máximo 255 caracteres.`, `O CPF é obrigatório.`, `O CPF deve conter exatamente 11 dígitos, com ou sem máscara (000.000.000-00).`, `Já existe um usuário cadastrado com este CPF.` (409)

### GET /users e GET /users/{id}

Ordenados por nome (pt-BR). Exemplo de `GET /users/2` (usuário com pendência):

```json
{ "id": 2, "name": "Bruno Oliveira", "cpf": "23456789012", "debtFree": false }
```

### POST /users

```http
POST /users
Content-Type: application/json

{ "name": "Marta Ribeiro", "cpf": "111.222.333-44" }
```

Resposta `201` com `Location: http://localhost:8080/users/11`; o CPF volta normalizado:

```json
{ "id": 11, "name": "Marta Ribeiro", "cpf": "11122233344", "debtFree": true }
```

Reenviar o mesmo CPF (com ou sem máscara) devolve `409`:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Já existe um usuário cadastrado com este CPF.",
  "fields": null,
  "timestamp": "2026-10-30T00:36:28.553488339Z"
}
```

CPF inválido (`"cpf": "abc"`) devolve `400` com `fields.cpf` igual à mensagem do CPF acima.

### PUT /users/{id}

Mesmo corpo do `POST`. Manter o próprio CPF é permitido; usar o CPF de outro usuário devolve `409`.

### DELETE /users/{id}

`204` sem corpo. Usuário com qualquer empréstimo registrado devolve `409`: `Não é possível excluir o usuário, pois ele possui empréstimos registrados.`

## Empréstimos

Regras detalhadas (pendência, status, devolução, datas) em [regras-de-negocio.md](../regras/regras-de-negocio.md).

### Campos

Requisição do `POST`:

| Campo | Tipo | Regras |
| --- | --- | --- |
| `userId` | número | Obrigatório; o usuário precisa existir |
| `bookId` | número | Obrigatório; o livro precisa existir |
| `startDate` | data ISO | Obrigatória; data do empréstimo (hoje ou passado) |
| `endDate` | data ISO | Obrigatória; data **prevista** da devolução (hoje ou futuro) |

Mensagens de campo obrigatório: `O usuário é obrigatório.`, `O livro é obrigatório.`, `A data do empréstimo é obrigatória.`, `A data prevista de devolução é obrigatória.`

Resposta:

| Campo | Tipo | Descrição |
| --- | --- | --- |
| `id` | número | Id do empréstimo |
| `startDate` | data ISO | Data do empréstimo |
| `endDate` | data ISO | Data prevista da devolução |
| `returnDate` | data ISO ou `null` | Data efetiva da devolução; `null` enquanto o livro não foi devolvido |
| `status` | string | `ATIVO`, `ATRASADO` ou `CONCLUIDO` (calculado, não é gravado) |
| `user` | objeto | Mesmo formato de `GET /users/{id}` (inclui `debtFree`) |
| `book` | objeto | Mesmo formato de `GET /books/{id}` (inclui `available`) |

Exemplo de `GET /lendings/1`:

```json
{
  "id": 1,
  "startDate": "2026-10-26",
  "endDate": "2026-11-09",
  "returnDate": null,
  "status": "ATIVO",
  "user": { "id": 1, "name": "Ana Souza", "cpf": "12345678901", "debtFree": true },
  "book": {
    "id": 1,
    "title": "Dom Casmurro",
    "author": { "id": 1, "name": "Machado de Assis" },
    "genre": { "id": 3, "name": "Literatura Brasileira" },
    "publishDate": "1899-01-01",
    "publisher": "Livraria Garnier",
    "available": false
  }
}
```

### GET /lendings

Do mais recente para o mais antigo (por `startDate` decrescente e, em empate, por `id` decrescente). Filtros opcionais, que podem ser combinados:

| Parâmetro | Tipo | Efeito |
| --- | --- | --- |
| `userId` | número | Só os empréstimos do usuário. Usuário inexistente: `404` (`Usuário não encontrado.`); valor não numérico: `400` |
| `status` | `ATIVO`, `ATRASADO` ou `CONCLUIDO` | Só os empréstimos naquele status. O valor diferencia maiúsculas de minúsculas (`ativo` devolve `400`). Parâmetro vazio (`?status=`) é ignorado |

Exemplos:

```http
GET /lendings?status=ATRASADO
GET /lendings?userId=2
GET /lendings?userId=2&status=ATRASADO
```

`GET /lendings?status=ATRASADO` no seed devolve 3 empréstimos (ids 8, 6 e 4: Bruno, Heitor e Elena). Valor inválido:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Valor inválido para o parâmetro 'status': 'XYZ'. Valores aceitos: ATIVO, ATRASADO, CONCLUIDO.",
  "fields": { "status": "Valor inválido para o parâmetro 'status': 'XYZ'. Valores aceitos: ATIVO, ATRASADO, CONCLUIDO." },
  "timestamp": "2026-10-30T00:36:28.953602550Z"
}
```

### POST /lendings

```http
POST /lendings
Content-Type: application/json

{ "userId": 3, "bookId": 11, "startDate": "2026-10-29", "endDate": "2026-11-05" }
```

Resposta `201` com `Location: http://localhost:8080/lendings/15`:

```json
{
  "id": 15,
  "startDate": "2026-10-29",
  "endDate": "2026-11-05",
  "returnDate": null,
  "status": "ATIVO",
  "user": { "id": 3, "name": "Carla Mendes", "cpf": "34567890123", "debtFree": true },
  "book": {
    "id": 11,
    "title": "Vidas Secas",
    "author": { "id": 9, "name": "Graciliano Ramos" },
    "genre": { "id": 3, "name": "Literatura Brasileira" },
    "publishDate": "1938-01-01",
    "publisher": "Livraria José Olympio Editora",
    "available": false
  }
}
```

Depois do `201` o livro passa a `available: false`.

As verificações acontecem nesta ordem; a primeira que falhar define a resposta (a validação de campos obrigatórios e de formato vem antes de todas):

| # | Condição | HTTP | Campo em `fields` | Mensagem |
| --- | --- | --- | --- | --- |
| 1 | `endDate` anterior a `startDate` | 400 | `endDate` | `A data prevista de devolução não pode ser anterior à data do empréstimo.` |
| 2 | `startDate` depois de hoje | 400 | `startDate` | `A data do empréstimo não pode ser futura.` |
| 3 | `endDate` anterior a hoje (o empréstimo já nasceria atrasado) | 400 | `endDate` | `A data prevista de devolução não pode ser anterior a hoje.` |
| 4 | Usuário inexistente | 404 | | `Usuário não encontrado.` |
| 5 | Livro inexistente | 404 | | `Livro não encontrado.` |
| 6 | Usuário com empréstimo em atraso | 409 | | `O usuário possui empréstimos em atraso e não pode realizar novos empréstimos.` |
| 7 | Livro com empréstimo em aberto | 409 | | `Este livro já está emprestado.` |
| 8 | `startDate` anterior à publicação do livro | 400 | `startDate` | `A data do empréstimo não pode ser anterior à data de publicação do livro.` |
| 9 | `startDate` anterior à última devolução do livro | 400 | `startDate` | `A data do empréstimo não pode ser anterior à última devolução deste livro.` |

Observações:

- Os itens 1 a 3 são conferidos antes de consultar o banco, então uma data inválida devolve 400 mesmo que o usuário ou o livro não exista.
- Empréstimo retroativo é aceito (`startDate` no passado), desde que respeite os itens 8 e 9. Começar no mesmo dia da última devolução do livro é permitido.
- `endDate` igual a hoje é aceito e o empréstimo nasce `ATIVO` (sem pendência).
- Dois `POST` simultâneos do mesmo livro: um recebe `201` e o outro `409` (`Este livro já está emprestado.`), porque a linha do livro é travada durante o registro.

Exemplos de resposta de erro:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "O usuário possui empréstimos em atraso e não pode realizar novos empréstimos.",
  "fields": null,
  "timestamp": "2026-10-30T00:36:28.660242739Z"
}
```

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "A data do empréstimo não pode ser futura.",
  "fields": { "startDate": "A data do empréstimo não pode ser futura." },
  "timestamp": "2026-10-30T00:36:28.702457245Z"
}
```

### PATCH /lendings/{id}/return

Registra a devolução. O corpo é opcional.

| Corpo | Efeito |
| --- | --- |
| Sem corpo, ou `{}` | Devolução com a data de hoje |
| `{ "returnDate": "2026-10-28" }` | Devolução na data informada |

Regras, na ordem:

| Condição | HTTP | Campo | Mensagem |
| --- | --- | --- | --- |
| Empréstimo inexistente | 404 | | `Empréstimo não encontrado.` |
| Empréstimo já devolvido | 409 | | `Este empréstimo já foi devolvido.` |
| `returnDate` anterior a `startDate` | 400 | `returnDate` | `A data de devolução não pode ser anterior à data do empréstimo.` |
| `returnDate` depois de hoje | 400 | `returnDate` | `A data de devolução não pode ser futura.` |

Devolver depois da data prevista (`returnDate` maior que `endDate`) é permitido: o empréstimo fica `CONCLUIDO`.

Exemplo: `PATCH /lendings/8/return` (empréstimo atrasado do Bruno, sem corpo, em 29/10/2026). O usuário deixa de ter pendência (`debtFree: true`) e o livro volta a ficar `available: true`:

```json
{
  "id": 8,
  "startDate": "2026-10-09",
  "endDate": "2026-10-23",
  "returnDate": "2026-10-29",
  "status": "CONCLUIDO",
  "user": { "id": 2, "name": "Bruno Oliveira", "cpf": "23456789012", "debtFree": true },
  "book": {
    "id": 6,
    "title": "Cem Anos de Solidão",
    "author": { "id": 6, "name": "Gabriel García Márquez" },
    "genre": { "id": 7, "name": "Realismo Mágico" },
    "publishDate": "1967-05-30",
    "publisher": "Editorial Sudamericana",
    "available": true
  }
}
```

Devolver de novo o mesmo empréstimo: `409`, `Este empréstimo já foi devolvido.`

## CORS

- Origens liberadas: propriedade `app.cors.allowed-origins` (lista separada por vírgula). Padrão: `http://localhost:5173` (Vite em desenvolvimento) e `http://localhost:4173` (`vite preview`).
- Para mudar, use a propriedade no `application.properties`, o argumento `--app.cors.allowed-origins=...` ou a variável de ambiente `APP_CORS_ALLOWED_ORIGINS`. No `docker-compose.yml` o padrão é o mesmo (`APP_CORS_ALLOWED_ORIGINS`). O frontend em container não precisa estar na lista, porque o Nginx dele repassa `/api` sem o header `Origin`.
- Métodos liberados: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`. Headers de requisição: qualquer um. Header exposto ao navegador: `Location`. Credenciais (cookies) não são liberadas.
- O preflight (`OPTIONS`) de uma origem liberada responde `200` com `Access-Control-Allow-Origin`, `Access-Control-Allow-Methods`, `Access-Control-Expose-Headers: Location` e `Access-Control-Max-Age: 1800`.
- Origem não liberada (no preflight ou na requisição real) recebe `403` com `Content-Type: application/json` e o corpo ApiError:

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Origem não permitida pelo CORS da API. Acesse por uma das origens liberadas.",
  "fields": null,
  "timestamp": "2026-10-30T00:36:29.024879692Z"
}
```

- Requisições sem o header `Origin` (curl, Postman, chamadas servidor a servidor) e requisições da própria origem da API (por exemplo o Swagger UI em `http://localhost:8080`) não passam pela checagem de CORS.
- Atrás do proxy do frontend (Nginx da stack Docker, ou o proxy do Vite), o header `Origin` é removido antes de chegar ao backend. Por isso o frontend funciona mesmo quando acessado por uma origem que não está na lista (`http://127.0.0.1:8081`, IP da rede...). Acessando a API diretamente, a origem do navegador precisa estar na lista.

## Header Location e proxy

Todo `201` devolve `Location` com a URL absoluta do recurso criado: URL da requisição + `/{id}`.

O backend usa `server.forward-headers-strategy=framework`, então respeita `X-Forwarded-Proto`, `X-Forwarded-Host` e `X-Forwarded-Prefix` enviados por um proxy:

| Acesso | Location |
| --- | --- |
| Direto na porta do backend | `http://localhost:8080/genres/10` |
| Pelo Nginx do frontend (`/api`) | `http://localhost:8081/api/genres/10` |

## Limitações conhecidas

- Uma requisição com `Accept: application/xml` (ou outro tipo que a API não produz) pode receber `500` sem corpo, porque o corpo de erro só é serializado em JSON. Navegadores e o frontend enviam `Accept` compatível com JSON, então não é afetado.
- Não há paginação nem busca textual; o filtro de empréstimos cobre apenas `userId` e `status`.
- O CPF é conferido pelo formato (11 dígitos), sem validar os dígitos verificadores, porque o seed e os testes usam CPFs fictícios.
