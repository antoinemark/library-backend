# Relatório de testes de QA (back-end)

Resultado da rodada de QA de 16/11/2026 sobre a API (`library-backend`, commit `c91c6ed`). Este arquivo traz só os **principais** casos. O catálogo completo tem 300 casos: 250 do back e 50 do front. A parte do front está em `biblioteca-web-frontend/docs/qa/relatorio-testes.md`.

## Sumário

- [Resumo](#resumo)
- [Como foi testado](#como-foi-testado)
- [Testes automáticos (Maven)](#testes-automáticos-maven)
- [Principais casos executados](#principais-casos-executados)
- [Casos que falharam e correções sugeridas](#casos-que-falharam-e-correções-sugeridas)
- [Divergências entre documentação e comportamento](#divergências-entre-documentação-e-comportamento)
- [Outras observações](#outras-observações)

## Resumo

| Item | Resultado |
| --- | --- |
| Testes automáticos (`mvn test`) | **77 de 77 passaram** (0 falhas, 0 erros, 0 ignorados) |
| Casos de QA da API (B001–B250) | **250 executados: 241 OK, 9 falharam** |
| Achados críticos | Nenhum |
| Achados médios | 3: nomes duplicados de autor e gênero (M1), prazo de empréstimo sem limite (M2) e links quebrados para `docs/specs/conformidade.md` (M3) |
| Achados baixos | 4 falhas (B068, B163, B165, B166), 2 divergências de documentação (D2, D3) e 3 observações |

As regras centrais funcionam como `docs/regras/regras-de-negocio.md` descreve: pendência bloqueia novos empréstimos, livro emprestado não pode sair de novo, as datas são validadas, exclusões com histórico são bloqueadas, o CPF é único e normalizado, e dois pedidos simultâneos do mesmo livro geram um 201 e um 409. As 9 falhas são **regras que a documentação não define** (unicidade de nomes, prazo máximo, tipo do campo e dígitos do CPF). Elas foram marcadas como falha porque o comportamento recomendado seria recusar.

## Como foi testado

- Stack Docker do README: `docker network create biblioteca-net` e `docker compose up -d --build`. A API ficou em `http://localhost:8080`, com o container `healthy` e o fuso `America/Sao_Paulo`.
- Cada caso é uma requisição HTTP real com status esperado ou uma verificação do corpo da resposta (contagens, ordem, campos). As datas são relativas ao dia do teste (hoje = 2026-11-16).
- O back foi reiniciado antes e depois da rodada (o H2 é em memória), então a base terminou de volta ao seed.

## Testes automáticos (Maven)

Comando: `docker run --rm -v "${PWD}:/app" -w /app maven:3.9-eclipse-temurin-21 mvn -B test`. Foi rodado numa cópia do repositório para não criar a pasta `target/` no checkout.

| Classe | Testes | Resultado |
| --- | --- | --- |
| ApiErrorAndCorsTest | 8 | ✅ |
| AuthorControllerTest | 10 | ✅ |
| BookControllerTest | 12 | ✅ |
| GenreControllerTest | 5 | ✅ |
| LendingControllerTest | 20 | ✅ |
| UserControllerTest | 9 | ✅ |
| LibraryApplicationTests | 1 | ✅ |
| SeedDataTest | 7 | ✅ |
| LendingServiceConcurrencyTest | 1 | ✅ |
| AlphabeticalOrderTest | 4 | ✅ |
| **Total** | **77** | **77 passaram** (BUILD SUCCESS, 1min37s) |

## Principais casos executados

### Seed e listagens

| # | Caso | Esperado | Obtido | |
| --- | --- | --- | --- | --- |
| B001–B005 | Quantidades do seed (autores, gêneros, livros, usuários, empréstimos) | 12 / 9 / 20 / 10 / 14 | 12 / 9 / 20 / 10 / 14 | ✅ |
| B006–B008 | Status dos empréstimos do seed | 4 ATIVO, 3 ATRASADO, 7 CONCLUIDO | igual | ✅ |
| B009 | Usuários com pendência | Bruno, Elena, Heitor | igual | ✅ |
| B012, B015 | Ordem pt-BR ("Cecília" junto dos "C"; "A Revolução..." antes de "Admirável...") | conforme a doc | igual | ✅ |
| B018 | Empréstimos do mais recente ao mais antigo | ids 11,5,1,3,8,6,4,13,12,10,2,14,9,7 | igual | ✅ |
| B023 | Todo livro com empréstimo em aberto aparece `available: false` | verdadeiro | verdadeiro | ✅ |
| B029 | Filtro combinado `userId=2&status=CONCLUIDO` | só o empréstimo 2 | igual | ✅ |
| B031–B035 | Filtros inválidos (`ativo`, `XYZ`, `userId` texto, inexistente, 0) | 400 / 400 / 400 / 404 / 404 | igual | ✅ |

### Cadastros (autores, gêneros, livros, usuários)

| # | Caso | Requisição | Esperado | Obtido | |
| --- | --- | --- | --- | --- | --- |
| B046–B047 | Criar autor e conferir `Location` | `POST /authors` | 201 + `Location` | 201 + `http://localhost:8080/authors/13` | ✅ |
| B050 | Espaços nas pontas são removidos | `{"name":"   Autor QA 2   "}` | "Autor QA 2" | igual | ✅ |
| B055 | Nome só com espaço não separável (U+00A0) | `POST /authors` | 400 | 400 | ✅ |
| B056–B057 | Limite de 255 caracteres | 255 / 256 | 201 / 400 | igual | ✅ |
| B059 | Acento e emoji na ida e na volta | "Érico Veríssimo 📚" | igual | igual | ✅ |
| B060–B061 | Texto com SQL (`'; DROP TABLE authors;--`) é gravado como texto | `POST /authors` | 201 e listagem ok | igual | ✅ |
| B075 | Excluir autor com livros | `DELETE /authors/1` | 409 | 409 | ✅ |
| B098 | Excluir gênero com livros | `DELETE /genres/3` | 409 | 409 | ✅ |
| B113 | Título " A " (1 caractere depois de aparar) | `POST /books` | 400 | 400 | ✅ |
| B123–B124 | Data de publicação futura e data de hoje | `POST /books` | 400 / 201 | igual | ✅ |
| B126 | Data inexistente `2023-02-30` | `POST /books` | 400 | 400 | ✅ |
| B132 | Vários campos inválidos: mensagem genérica + `fields` | `POST /books` | 4+ campos em `fields` | igual | ✅ |
| B139 | Editar livro com histórico não muda `available` | `PUT /books/1` | `false` | `false` | ✅ |
| B142 | Excluir livro que só tem empréstimo concluído | `DELETE /books/2` | 409 | 409 | ✅ |
| B150 | CPF com máscara gravado só com dígitos | `"111.222.333-44"` | `11122233344` | igual | ✅ |
| B153–B155 | CPF duplicado com e sem máscara | `POST /users` | 409 | 409 | ✅ |
| B156–B162 | CPF com letras, 10 ou 12 dígitos, espaços, barra, vazio ou ausente | `POST /users` | 400 | 400 | ✅ |
| B172 | Editar usuário para o CPF de outro | `PUT /users/{id}` | 409 | 409 | ✅ |
| B176 | Excluir usuário só com histórico concluído | `DELETE /users/4` | 409 | 409 | ✅ |
| B179 | CPF de usuário excluído pode ser reutilizado | `POST /users` | 201 | 201 | ✅ |

### Empréstimos e devolução

| # | Caso | Esperado | Obtido | |
| --- | --- | --- | --- | --- |
| B180–B182 | Empréstimo normal; livro fica indisponível | 201, ATIVO, `available: false` | igual | ✅ |
| B184–B185 | Livro já emprestado | 409 | 409 | ✅ |
| B186–B188 | Usuário com pendência (Bruno, Elena, Heitor) | 409 | 409 | ✅ |
| B189–B190 | Pendência é checada antes de livro emprestado | 409 com a mensagem de pendência | igual | ✅ |
| B191 | Usuário com empréstimo ATIVO (sem atraso) pode pegar outro livro | 201 | 201 | ✅ |
| B192–B194 | `endDate` < `startDate`, `startDate` futura, `endDate` no passado | 400 | 400 | ✅ |
| B195 | Data inválida dá 400 mesmo com usuário inexistente | 400 | 400 | ✅ |
| B202–B203 | Início antes da publicação e antes da última devolução do livro | 400 | 400 | ✅ |
| B204 | Empréstimo retroativo válido | 201 | 201 | ✅ |
| B205–B206 | `endDate` = hoje nasce ATIVO e sem pendência | 201 | 201 | ✅ |
| B213–B214 | `DELETE` e `PUT` em empréstimo | 405 | 405 | ✅ |
| B215 | Dois POST simultâneos do mesmo livro | um 201 e um 409 | igual | ✅ |
| B216–B219 | Devolução com data futura, antes do início, formato errado ou JSON malformado | 400 | 400 | ✅ |
| B220–B222 | Devolução sem corpo usa hoje; livro volta a ficar disponível | 200, CONCLUIDO | igual | ✅ |
| B223 | Devolver de novo | 409 | 409 | ✅ |
| B228–B230 | Devolver o atrasado do Heitor libera a pendência e ele pode emprestar de novo | 200 / `debtFree: true` / 201 | igual | ✅ |

### Contrato HTTP, CORS e erros

| # | Caso | Esperado | Obtido | |
| --- | --- | --- | --- | --- |
| B231, B246–B247 | Rota inexistente: 404 com `{status, error, message, fields, timestamp}` e timestamp em UTC | conforme a doc | igual | ✅ |
| B233–B236 | JSON malformado, corpo vazio, `text/plain`, lista no lugar de objeto | 400 / 400 / 415 / 400 | igual | ✅ |
| B080–B081 | Método não suportado traz o header `Allow` | 405 + `Allow` | igual | ✅ |
| B241, B244, B249 | CORS de origem não liberada (requisição e preflight), corpo em JSON | 403 | 403 | ✅ |
| B243, B248 | Preflight de origem liberada com `Expose-Headers: Location` e `Max-Age 1800` | 200 | igual | ✅ |
| B237 | `/h2-console` desativado | 404 | 404 | ✅ |
| B240 | `Accept: application/xml` (limitação conhecida) | 500 | 500 | ✅ |
| B250 | 50 listagens de livros seguidas | menos de 5 s | dentro do limite | ✅ |

## Casos que falharam e correções sugeridas

Todas as falhas abaixo são de regras que a documentação **não define**. A coluna "Esperado" traz o comportamento recomendado.

| # | Caso | Esperado | Obtido | Gravidade | Sugestão de correção |
| --- | --- | --- | --- | --- | --- |
| B064 | Criar autor com o nome de um autor que já existe ("Machado de Assis") | 409 | 201 | Média | Verificar nome duplicado em `AuthorService` (comparação sem diferenciar maiúsculas, acentos e espaços, usando o `TextNormalizer`), com restrição `UNIQUE` no banco. Hoje o front mostra dois "Machado de Assis" e o seletor de autor do cadastro de livro fica ambíguo. |
| B093 | Criar gênero "Romance" de novo | 409 | 201 | Média | Mesma regra em `GenreService`. |
| B094 | Criar gênero "romance" (só muda maiúscula) | 409 | 201 | Média | Mesma regra, sem diferenciar maiúsculas. |
| B207 | Empréstimo com `endDate` 9999-12-31 | 400 | 201 | Média | Definir um prazo máximo (por exemplo 30 dias, configurável por propriedade) em `LendingService.createLending`. Sem isso, um empréstimo pode nunca ficar atrasado e o usuário nunca ganha pendência. Isso esvazia a regra "o empréstimo ocorre somente sem pendências" da especificação. |
| B208 | Empréstimo com prazo de 1 ano | 400 | 201 | Média | Mesma regra de prazo máximo. Se o prazo longo for desejado, registrar isso na doc. |
| B068 | `{"name": 123}` (número no lugar de texto) | 400 | 201 | Baixa | O Jackson converte o número em texto. Para recusar, desligar a coerção de escalares (`spring.jackson.mapper.allow-coercion-of-scalars=false`) ou usar um deserializador estrito. |
| B163 | `{"cpf": 12345678909}` (número no lugar de texto) | 400 | 201 | Baixa | Mesma causa de B068. |
| B165 | CPF com dígitos verificadores errados (`123.456.789-00`) | 400 | 201 | Baixa | É proposital (o seed usa CPFs fictícios). Para produção, validar os dígitos e trocar o seed por CPFs válidos. Hoje está documentado em "Limitações conhecidas". |
| B166 | CPF com máscara misturada (`123.456789-55`) | 400 | 201 | Baixa | A expressão `\d{3}\.?\d{3}\.?\d{3}-?\d{2}` aceita cada ponto separadamente. Aceitar só o formato "todo com máscara" ou "todo sem máscara": `\d{3}\.\d{3}\.\d{3}-\d{2}\|\d{11}`. |

## Divergências entre documentação e comportamento

| # | Divergência | Onde | Gravidade | Sugestão |
| --- | --- | --- | --- | --- |
| M3 | O arquivo `docs/specs/conformidade.md` é citado 4 vezes, mas não existe no repositório | `docs/api/endpoints.md` (seção Autores), `docs/banco/modelo-e-seed.md`, `docs/regras/regras-de-negocio.md` (2 vezes) | Média | Criar o arquivo com os "desvios justificados" (por exemplo, autor sem livros) ou remover os links. |
| D2 | A seção 3 do Documento de Análise ainda tem o aviso "[A imagem do diagrama de classes ... deve ser inserida aqui]" | `docs/Documento_de_Análise_-_Sistema_de_Empréstimo.pdf` | Baixa | Inserir a imagem do diagrama de classes. |
| D3 | O README só mostra o clone normal, que falha no Windows quando a pasta tem caminho longo (`Filename too long`) | `README.md` | Baixa | Adicionar a dica `git -c core.longpaths=true clone ...` ou "clone numa pasta curta". |

Itens conferidos que **estão de acordo** com a doc: quantidades do seed, todas as mensagens de erro testadas, a ordem das validações do empréstimo, o `Location` direto e via proxy, CORS, o 404 do console H2, o 500 com `Accept: application/xml` e os 77 testes citados no README.

## Outras observações

- **Aviso do Mockito no `mvn test`:** o carregamento dinâmico de agent ainda funciona, mas versões futuras do Java vão bloquear. Sugestão: configurar o Mockito como `-javaagent` no `maven-surefire-plugin`.
- **Avisos no log de subida:** o `spring.jpa.database-platform` declara um `H2Dialect` desnecessário, o `spring.jpa.open-in-view` está ligado por padrão e o Swagger está ativo por padrão. Nenhum deles afeta o funcionamento.
- **OpenAPI com título genérico:** aparece "OpenAPI definition", versão `v0`. Sugestão: definir título e descrição do projeto no springdoc.
