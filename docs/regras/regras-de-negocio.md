# Regras de negócio

Regras que o backend aplica e que não podem ser quebradas por alterações futuras. Todas foram conferidas no código atual; o [quadro final](#onde-cada-regra-vive-e-qual-teste-a-cobre) indica a classe responsável e o teste que protege cada regra.

As mensagens citadas são as devolvidas pela API (veja o [contrato](../api/endpoints.md)). A origem de cada regra na especificação está em [conformidade.md](../specs/conformidade.md).

## Sumário

- [Glossário](#glossário)
- [Pendência e bloqueio de novos empréstimos](#pendência-e-bloqueio-de-novos-empréstimos)
- [Status do empréstimo](#status-do-empréstimo)
- [Disponibilidade do livro](#disponibilidade-do-livro)
- [Registrar um empréstimo](#registrar-um-empréstimo)
- [Devolução](#devolução)
- [Exclusões bloqueadas e histórico](#exclusões-bloqueadas-e-histórico)
- [Autor sem livros](#autor-sem-livros)
- [CPF](#cpf)
- [Validação e normalização de textos](#validação-e-normalização-de-textos)
- [Ordenação alfabética pt-BR](#ordenação-alfabética-pt-br)
- [Datas, fuso e relógio](#datas-fuso-e-relógio)
- [Concorrência](#concorrência)
- [Onde cada regra vive e qual teste a cobre](#onde-cada-regra-vive-e-qual-teste-a-cobre)

## Glossário

| Termo | Significado |
| --- | --- |
| Hoje | `LocalDate.now(clock)`, usando o bean `Clock` da aplicação (fuso da JVM) |
| `startDate` | Data em que o livro foi emprestado |
| `endDate` | Data **prevista** da devolução (o prazo) |
| `returnDate` | Data **efetiva** da devolução; `null` enquanto o livro não voltou |
| Empréstimo em aberto | Empréstimo com `returnDate` nulo |
| Pendência | Empréstimo em aberto com `endDate` anterior a hoje (devolução atrasada) |

## Pendência e bloqueio de novos empréstimos

A especificação diz que o empréstimo só ocorre se o usuário estiver sem pendências. O sistema interpreta **pendência** como ter ao menos um empréstimo em aberto cuja data prevista já passou:

```text
pendência = returnDate nulo  E  endDate < hoje   (comparação estrita)
```

Consequências:

- `endDate` igual a hoje **não** é pendência: o último dia do prazo ainda é `ATIVO`.
- Ter um empréstimo em aberto dentro do prazo não impede novos empréstimos (um usuário pode ter vários livros em casa).
- Devolver o livro atrasado remove a pendência na hora. Se o usuário tiver outro empréstimo atrasado, a pendência continua.
- `GET /users` e `GET /users/{id}` devolvem `debtFree: false` para quem tem pendência. O mesmo critério está em `GET /lendings` (campo `user.debtFree`).
- `POST /lendings` de usuário com pendência devolve `409`: `O usuário possui empréstimos em atraso e não pode realizar novos empréstimos.`
- A checagem de pendência vem **antes** da checagem de livro emprestado. Se o usuário tem pendência e o livro também está emprestado, a mensagem é a da pendência.
- Como `endDate` não pode ser anterior a hoje na criação (veja abaixo), nenhum empréstimo nasce atrasado: a pendência só surge com o passar dos dias.

No seed, os únicos usuários com pendência são Bruno Oliveira, Elena Rostova e Heitor Costa (veja [modelo-e-seed.md](../banco/modelo-e-seed.md#seed)).

## Status do empréstimo

O status **não é gravado**: é calculado a cada resposta a partir das datas (`LendingStatus.of`).

| Condição (avaliada nesta ordem) | Status |
| --- | --- |
| `returnDate` preenchido | `CONCLUIDO` (mesmo que tenha sido devolvido com atraso) |
| hoje depois de `endDate` | `ATRASADO` |
| demais casos | `ATIVO` |

Por ser calculado, o status muda sozinho com a passagem do tempo (um `ATIVO` vira `ATRASADO` no dia seguinte ao `endDate`) e o filtro `GET /lendings?status=` usa o mesmo cálculo.

## Disponibilidade do livro

- Um livro está **disponível** quando não tem empréstimo em aberto. O campo `available` das respostas é calculado; não existe coluna no banco.
- Cada livro representa um exemplar único: só pode haver **um empréstimo em aberto por livro**. Tentar emprestar um livro que já está emprestado devolve `409`: `Este livro já está emprestado.`
- O histórico de um livro não pode ter períodos sobrepostos: um novo empréstimo só pode começar no dia da última devolução do livro ou depois (veja a regra 9 abaixo).

## Registrar um empréstimo

`POST /lendings` aplica as regras nesta ordem (a primeira que falha encerra a requisição):

| # | Regra | Resposta |
| --- | --- | --- |
| 0 | Campos obrigatórios e formato das datas (Bean Validation / Jackson) | 400 |
| 1 | `endDate` não pode ser anterior a `startDate` | 400, campo `endDate` |
| 2 | `startDate` não pode ser futura | 400, campo `startDate` |
| 3 | `endDate` não pode ser anterior a hoje | 400, campo `endDate` |
| 4 | O usuário precisa existir | 404 |
| 5 | O livro precisa existir (a linha do livro é travada aqui, veja [Concorrência](#concorrência)) | 404 |
| 6 | O usuário não pode ter pendência | 409 |
| 7 | O livro não pode ter empréstimo em aberto | 409 |
| 8 | `startDate` não pode ser anterior à data de publicação do livro | 400, campo `startDate` |
| 9 | `startDate` não pode ser anterior a nenhuma devolução já registrada do livro | 400, campo `startDate` |

Por quê:

- **Regra 2:** um empréstimo futuro deixaria o livro indisponível sem como devolvê-lo antes da data (a devolução não aceita data futura e empréstimos não podem ser editados nem excluídos).
- **Regra 3:** um empréstimo que nasce atrasado criaria pendência para o usuário no mesmo instante.
- **Regras 8 e 9:** protegem a integridade do histórico (um livro não é emprestado antes de existir nem enquanto ainda estava com outra pessoa).
- Empréstimos **retroativos** são aceitos (`startDate` no passado) quando respeitam as regras 8 e 9 e o prazo `endDate` ainda não passou. Começar exatamente no dia da última devolução é permitido.

Mensagens completas na [tabela do contrato](../api/endpoints.md#post-lendings).

## Devolução

`PATCH /lendings/{id}/return`, corpo opcional `{ "returnDate": "YYYY-MM-DD" }`.

| Regra | Resposta |
| --- | --- |
| O empréstimo precisa existir | 404 |
| Não pode já estar devolvido | 409, `Este empréstimo já foi devolvido.` |
| Sem `returnDate` no corpo (ou sem corpo), vale a data de hoje | |
| `returnDate` não pode ser anterior a `startDate` | 400, campo `returnDate` |
| `returnDate` não pode ser futura | 400, campo `returnDate` |

- Devolver no mesmo dia do empréstimo (`returnDate` igual a `startDate`) é permitido.
- Devolver depois do prazo (`returnDate` maior que `endDate`) é permitido: o empréstimo fica `CONCLUIDO`, o livro volta a ficar disponível e a pendência desaparece.
- A devolução é a única alteração possível em um empréstimo existente.

## Exclusões bloqueadas e histórico

O histórico de empréstimos nunca é apagado. Para isso:

| Recurso | Quando a exclusão é bloqueada | Resposta |
| --- | --- | --- |
| Empréstimo | Sempre: não existe `DELETE` (nem `PUT`) em `/lendings` | 405 |
| Livro | Possui qualquer empréstimo, aberto ou devolvido | 409, `Não é possível excluir o livro, pois ele possui empréstimos registrados.` |
| Usuário | Possui qualquer empréstimo | 409, `Não é possível excluir o usuário, pois ele possui empréstimos registrados.` |
| Autor | Possui livros | 409, `Não é possível excluir o autor, pois ele possui livros cadastrados.` |
| Gênero | Possui livros | 409, `Não é possível excluir o gênero, pois existem livros cadastrados nele.` |

As chaves estrangeiras do banco são todas `NOT NULL` e sem `ON DELETE CASCADE`; um `DataIntegrityViolationException` que escapasse dessas checagens vira `409` com `A operação não pode ser concluída porque viola uma restrição de integridade dos dados.`

Livros e usuários com histórico podem ser **editados** (`PUT`): isso é manutenção cadastral e não apaga o histórico.

## Autor sem livros

A especificação (seção 4) define Livro `1..*` — `1` Autor: todo autor deve ter pelo menos um livro. A API **permite** autor sem livros, por decisão do dono do projeto:

- É preciso cadastrar o autor antes do primeiro livro (o livro exige `authorId`), então `POST /authors` precisa aceitar um autor ainda sem livros.
- Excluir um livro ou trocar o autor dele pode deixar o autor anterior com 0 livros. Bloquear isso com `409` criaria um impasse (o livro não poderia ser excluído nem o autor); excluir o autor automaticamente seria destrutivo e surpreendente.
- A regra continua visível: `bookCount` vem em todas as respostas de autor, o seed cumpre a regra (todo autor com ao menos um livro, verificado por `SeedDataTest.everyAuthorHasAtLeastOneBook`) e o frontend sinaliza autores com 0 livros ("Sem livros").

Esse é um desvio controlado, registrado em [conformidade.md](../specs/conformidade.md#desvios-justificados).

## CPF

- Aceito com ou sem máscara: `123.456.789-01` ou `12345678901` (expressão `\d{3}\.?\d{3}\.?\d{3}-?\d{2}`). Letras, outros símbolos e quantidade de dígitos diferente de 11 resultam em `400`: `O CPF deve conter exatamente 11 dígitos, com ou sem máscara (000.000.000-00).`
- Os **dígitos verificadores não são conferidos**, de propósito: o seed e os testes usam CPFs fictícios (`12345678901`...).
- É gravado **somente com dígitos** (coluna `cpf varchar(11)`), e é isso que a API devolve. Quem exibe a máscara é o frontend.
- É **único**: tentar cadastrar (ou atualizar para) um CPF já usado por outro usuário devolve `409`: `Já existe um usuário cadastrado com este CPF.` A comparação é feita sobre os dígitos, então `111.222.333-44` e `11122233344` são o mesmo CPF. Atualizar um usuário mantendo o próprio CPF é permitido. Há também uma restrição `UNIQUE` no banco.

## Validação e normalização de textos

Os records de requisição (`AuthorRequest`, `GenreRequest`, `BookRequest`, `UserRequest`) aplicam `TextNormalizer.strip` no construtor compacto, **antes** da Bean Validation. Assim a validação vale para o texto que será gravado.

- Remove espaços e caracteres de controle das pontas, incluindo o espaço não separável (U+00A0), que `String.trim()` e `String.strip()` não removem e é comum em texto copiado da web.
- Um nome só com espaços (inclusive U+00A0) é recusado como vazio (`400`).
- Espaços no meio do texto não são alterados.
- O limite do título (mínimo 2) é conferido depois de aparar.

| Campo | Regra |
| --- | --- |
| Nome de autor, gênero e usuário | Obrigatório, até 255 caracteres |
| Título do livro | Obrigatório, de 2 a 255 caracteres |
| Editora | Obrigatória, até 255 caracteres |
| Data de publicação | Obrigatória, não pode ser futura (hoje vale) |

## Ordenação alfabética pt-BR

As listagens de autores, gêneros, livros (por título) e usuários são ordenadas em memória por `AlphabeticalOrder`, não pelo `ORDER BY` do banco:

- O `ORDER BY` do H2 compara pelo código Unicode e jogaria iniciais acentuadas e minúsculas para o fim.
- O `Collator` do Java sozinho ignora espaços (colocaria "Ohio" antes de "O Hobbit").
- A comparação é palavra por palavra, como num índice: o espaço vem antes das letras ("A Hora da Estrela" antes de "Admirável Mundo Novo"); números vêm antes das letras; acentos e maiúsculas não mudam a posição ("Érico Veríssimo" fica junto dos "E"), servindo só de desempate. Empate total é resolvido pelo `id`.
- Empréstimos não usam essa ordem: vêm de `startDate` decrescente e `id` decrescente, direto do banco.

Novas listagens alfabéticas devem usar `AlphabeticalOrder.by(...)`.

## Datas, fuso e relógio

- Todas as datas são `LocalDate` (sem hora e sem fuso), trafegam como `YYYY-MM-DD` e são gravadas em colunas `date`.
- Os services obtêm "hoje" **sempre** de `LocalDate.now(clock)` com o bean `Clock` (`ClockConfig`, que usa `Clock.systemDefaultZone()`). Nunca use `LocalDate.now()` direto nos services: o bean existe para os testes congelarem a data.
- `@PastOrPresent` da data de publicação usa o relógio padrão do Bean Validation (fuso da JVM).
- O fuso é o da JVM. Na máquina do desenvolvedor, é o do sistema. No container, o `Dockerfile` define `TZ=America/Sao_Paulo`; sem isso o container roda em UTC e, entre 21h e 24h (Brasília), o "hoje" da aplicação já seria o dia seguinte (status dos empréstimos, devolução e datas do seed). O `docker-compose.yml` permite trocar o fuso com `BACKEND_TZ` (veja [imagens-e-stack.md](../docker/imagens-e-stack.md#variáveis-do-compose)).
- O H2 avalia `CURRENT_DATE` do seed no mesmo fuso da JVM, então seed e services concordam sobre o dia.
- Nos testes, o `Clock` é substituído por um relógio fixo (`FixedClockTestConfig`), para a suíte não falhar se atravessar a meia-noite. Os testes pegam a data por `today()`, nunca por `LocalDate.now()`.

## Concorrência

Registrar um empréstimo é "verificar e depois inserir", o que permitiria dois empréstimos em aberto do mesmo livro se duas requisições chegassem juntas. Para evitar isso, `LendingService.createLending` busca o livro com `BookRepository.findByIdForUpdate` (`@Lock(PESSIMISTIC_WRITE)`, que gera `SELECT ... FOR UPDATE`) dentro da transação. A segunda requisição espera a primeira confirmar e então encontra o empréstimo em aberto, recebendo `409`.

`BookService.findBookForUpdate` exige transação ativa (`Propagation.MANDATORY`); chamá-lo fora de uma transação falha de propósito. `LendingServiceConcurrencyTest` roda 5 livros × 2 threads simultâneas e confere que sobra exatamente um empréstimo por livro.

## Onde cada regra vive e qual teste a cobre

| Regra | Classe / método | Testes |
| --- | --- | --- |
| Bloqueio por pendência | `LendingService.createLending`, `UserService.hasOverdueLendings`, `LendRepository.existsByUserIdAndReturnDateIsNullAndEndDateBefore` | `LendingControllerTest.blocksUserWithOverdueLendings`, `checksOverdueUserBeforeBookAvailability` |
| `debtFree` | `UserService.toResponse` | `UserControllerTest.listsUsersOrderedByNameWithDebtStatus`, `SeedDataTest.onlyMockUsersWithPendingLendingsAreNotDebtFree` |
| Livro já emprestado / `available` | `LendRepository.existsByBookIdAndReturnDateIsNull`, `BookService.toResponse` | `LendingControllerTest.blocksBookAlreadyLent`, `createsLendingAndBookBecomesUnavailable`, `SeedDataTest.unavailableBooksMatchOpenLendings` |
| Datas do empréstimo | `LendingService.createLending`, `LendRepository.existsByBookIdAndReturnDateAfter` | `LendingControllerTest.rejectsEndDateBeforeStartDate`, `rejectsFutureStartDate`, `rejectsLendingThatWouldBeBornOverdue`, `acceptsRetroactiveLendingDueToday`, `rejectsStartDateBeforeBookPublication`, `rejectsStartDateBeforeLastReturnOfTheBook` |
| Status derivado e filtros | `LendingStatus.of`, `LendingService.getLendings` | `LendingControllerTest.filtersByUserAndStatus`, `rejectsInvalidFilters`, `SeedDataTest.lendingsCoverAllStatusesRelativeToToday` |
| Devolução | `LendingService.returnLending` | `LendingControllerTest.returnsBookWithoutBodyUsingToday`, `returnsBookWithInformedDateAndUserIsReleased`, `acceptsEmptyJsonBodyOnReturn`, `rejectsInvalidReturnDates` |
| Sem excluir nem editar empréstimo | `LendingController` (sem `DELETE`/`PUT`) | `LendingControllerTest.lendingsCannotBeDeleted` |
| Exclusões bloqueadas | `AuthorService`, `GenreService`, `BookService`, `UserService` (`delete*`) | `AuthorControllerTest.doesNotDeleteAuthorWithBooks`, `GenreControllerTest.doesNotDeleteGenreWithBooks`, `BookControllerTest.doesNotDeleteBookWithLendingHistory`, `UserControllerTest.doesNotDeleteUserWithLendings` |
| CPF | `CpfValidator`, `UserService` | `UserControllerTest.createsUserNormalizingMaskedCpf`, `rejectsDuplicatedCpfEvenWhenMasked`, `rejectsCpfWithoutElevenDigits`, `rejectsCpfWithLettersOrOtherSymbols`, `updatesUserAndChecksCpfOfOtherUsers` |
| Textos aparados | `TextNormalizer`, records `*Request` | `AuthorControllerTest.rejectsNameMadeOnlyOfNonBreakingSpaces`, `BookControllerTest.validatesTitleLengthAfterTrimming`, `GenreControllerTest.validatesNameLengthAfterRemovingEdgeSpaces` |
| Ordenação pt-BR | `AlphabeticalOrder` | `AlphabeticalOrderTest`, `AuthorControllerTest.listsAuthorsInPortugueseAlphabeticalOrder`, `BookControllerTest.listsBooksInPortugueseAlphabeticalOrder` |
| Concorrência | `BookRepository.findByIdForUpdate`, `BookService.findBookForUpdate` | `LendingServiceConcurrencyTest.concurrentLendingsOfTheSameBookRegisterOnlyOne` |
| Autor com pelo menos um livro no seed | `data.sql` | `SeedDataTest.everyAuthorHasAtLeastOneBook` |
| Formato de erro, CORS e `Location` | `GlobalExceptionHandler`, `CorsConfig`, `ApiErrorCorsProcessor` | `ApiErrorAndCorsTest` |
