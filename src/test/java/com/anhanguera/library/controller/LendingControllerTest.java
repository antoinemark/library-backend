package com.anhanguera.library.controller;

import com.anhanguera.library.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LendingControllerTest extends IntegrationTestSupport {

    private static final String OVERDUE_MESSAGE =
            "O usuário possui empréstimos em atraso e não pode realizar novos empréstimos.";
    private static final String UNAVAILABLE_MESSAGE = "Este livro já está emprestado.";

    private LocalDate today;

    @BeforeEach
    void setUpToday() {
        today = today();
    }

    private String lendingJson(long userId, long bookId, LocalDate startDate, LocalDate endDate) {
        return """
                {"userId": %d, "bookId": %d, "startDate": "%s", "endDate": "%s"}
                """.formatted(userId, bookId, startDate, endDate);
    }

    /**
     * Id do empréstimo em aberto do livro informado (no seed há no máximo um por livro).
     */
    private long openLendingIdOfBook(String title) throws Exception {
        String json = body(mockMvc.perform(get("/lendings")));
        List<Number> ids = JsonPath.read(json,
                "$[?(@.book.title == '" + title + "' && @.returnDate == null)].id");
        return ids.getFirst().longValue();
    }

    @Test
    void listsLendingsMostRecentFirst() throws Exception {
        mockMvc.perform(get("/lendings"))
                .andExpect(status().isOk())
                // Gabriela Lima pegou "Admirável Mundo Novo" hoje: é o empréstimo mais recente do seed
                .andExpect(jsonPath("$[0].startDate").value(today.toString()))
                .andExpect(jsonPath("$[0].endDate").value(today.plusDays(14).toString()))
                .andExpect(jsonPath("$[0].returnDate").value(nullValue()))
                .andExpect(jsonPath("$[0].status").value("ATIVO"))
                .andExpect(jsonPath("$[0].user.name").value("Gabriela Lima"))
                .andExpect(jsonPath("$[0].user.debtFree").value(true))
                .andExpect(jsonPath("$[0].book.title").value("Admirável Mundo Novo"))
                .andExpect(jsonPath("$[0].book.available").value(false));
    }

    @Test
    void filtersByUserAndStatus() throws Exception {
        long elenaId = idOf("/users", "name", "Elena Rostova");
        mockMvc.perform(get("/lendings").param("userId", String.valueOf(elenaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].user.id", everyItem(is((int) elenaId))));

        mockMvc.perform(get("/lendings")
                        .param("userId", String.valueOf(elenaId))
                        .param("status", "ATRASADO"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].book.title").value("1984"))
                .andExpect(jsonPath("$[0].user.debtFree").value(false));

        mockMvc.perform(get("/lendings").param("status", "CONCLUIDO"))
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[*].status", everyItem(is("CONCLUIDO"))));
    }

    @Test
    void rejectsInvalidFilters() throws Exception {
        mockMvc.perform(get("/lendings").param("status", "PERDIDO"))
                .andExpect(apiError(400, "Bad Request"));
        mockMvc.perform(get("/lendings").param("userId", "999999"))
                .andExpect(apiError(404, "Not Found"));
    }

    @Test
    void createsLendingAndBookBecomesUnavailable() throws Exception {
        long userId = idOf("/users", "name", "Isabela Rocha");
        long bookId = idOf("/books", "title", "Vidas Secas");

        ResultActions created = postJson("/lendings", lendingJson(userId, bookId, today, today.plusDays(14)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startDate").value(today.toString()))
                .andExpect(jsonPath("$.endDate").value(today.plusDays(14).toString()))
                .andExpect(jsonPath("$.returnDate").value(nullValue()))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.user.id").value(userId))
                .andExpect(jsonPath("$.book.id").value(bookId))
                .andExpect(jsonPath("$.book.available").value(false));
        long id = idFrom(created);
        created.andExpect(header().string("Location", endsWith("/lendings/" + id)));

        mockMvc.perform(get("/lendings/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mockMvc.perform(get("/books/{id}", bookId))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void blocksUserWithOverdueLendings() throws Exception {
        long brunoId = idOf("/users", "name", "Bruno Oliveira");
        long bookId = idOf("/books", "title", "Vidas Secas");

        postJson("/lendings", lendingJson(brunoId, bookId, today, today.plusDays(7)))
                .andExpect(apiError(409, "Conflict"))
                .andExpect(jsonPath("$.message").value(OVERDUE_MESSAGE));
    }

    @Test
    void checksOverdueUserBeforeBookAvailability() throws Exception {
        long brunoId = idOf("/users", "name", "Bruno Oliveira");
        long lentBookId = idOf("/books", "title", "Dom Casmurro");

        postJson("/lendings", lendingJson(brunoId, lentBookId, today, today.plusDays(7)))
                .andExpect(apiError(409, "Conflict"))
                .andExpect(jsonPath("$.message").value(OVERDUE_MESSAGE));
    }

    @Test
    void blocksBookAlreadyLent() throws Exception {
        long carlaId = idOf("/users", "name", "Carla Mendes");
        long lentBookId = idOf("/books", "title", "Dom Casmurro");

        postJson("/lendings", lendingJson(carlaId, lentBookId, today, today.plusDays(7)))
                .andExpect(apiError(409, "Conflict"))
                .andExpect(jsonPath("$.message").value(UNAVAILABLE_MESSAGE));
    }

    @Test
    void rejectsEndDateBeforeStartDate() throws Exception {
        long userId = idOf("/users", "name", "Isabela Rocha");
        long bookId = idOf("/books", "title", "Vidas Secas");

        postJson("/lendings", lendingJson(userId, bookId, today, today.minusDays(1)))
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.endDate").exists());
    }

    @Test
    void rejectsFutureStartDate() throws Exception {
        // Um empréstimo futuro deixaria o livro indisponível sem poder ser devolvido antes da data
        long userId = idOf("/users", "name", "Isabela Rocha");
        long bookId = idOf("/books", "title", "Vidas Secas");

        postJson("/lendings", lendingJson(userId, bookId, today.plusDays(1), today.plusDays(8)))
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.startDate").value("A data do empréstimo não pode ser futura."));

        mockMvc.perform(get("/books/{id}", bookId))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void rejectsLendingThatWouldBeBornOverdue() throws Exception {
        // Retroativo com o prazo já vencido: nasceria ATRASADO e bloquearia o usuário na hora
        long userId = idOf("/users", "name", "Isabela Rocha");
        long bookId = idOf("/books", "title", "Vidas Secas");

        postJson("/lendings", lendingJson(userId, bookId, today.minusDays(10), today.minusDays(1)))
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.endDate")
                        .value("A data prevista de devolução não pode ser anterior a hoje."));

        mockMvc.perform(get("/users/{id}", userId))
                .andExpect(jsonPath("$.debtFree").value(true));
    }

    @Test
    void acceptsRetroactiveLendingDueToday() throws Exception {
        // Limite: devolução prevista para hoje ainda é ATIVO e não gera pendência
        long userId = idOf("/users", "name", "Isabela Rocha");
        long bookId = idOf("/books", "title", "Vidas Secas");

        postJson("/lendings", lendingJson(userId, bookId, today.minusDays(7), today))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startDate").value(today.minusDays(7).toString()))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.user.debtFree").value(true));
    }

    @Test
    void rejectsStartDateBeforeLastReturnOfTheBook() throws Exception {
        // "A Hora da Estrela" esteve com Bruno Oliveira de 60 a 50 dias atrás (devolvido)
        long anaId = idOf("/users", "name", "Ana Souza");
        long bookId = idOf("/books", "title", "A Hora da Estrela");

        postJson("/lendings", lendingJson(anaId, bookId, today.minusDays(55), today.plusDays(7)))
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.startDate")
                        .value("A data do empréstimo não pode ser anterior à última devolução deste livro."));

        // A partir do dia da última devolução o livro já pode ser emprestado de novo
        postJson("/lendings", lendingJson(anaId, bookId, today.minusDays(50), today.plusDays(7)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ATIVO"));
    }

    @Test
    void rejectsStartDateBeforeBookPublication() throws Exception {
        // "Vidas Secas" foi publicado em 1938
        long userId = idOf("/users", "name", "Isabela Rocha");
        long bookId = idOf("/books", "title", "Vidas Secas");

        postJson("/lendings", lendingJson(userId, bookId, LocalDate.of(1900, 1, 1), today.plusDays(7)))
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.startDate")
                        .value("A data do empréstimo não pode ser anterior à data de publicação do livro."));
    }

    @Test
    void validatesRequiredFields() throws Exception {
        postJson("/lendings", "{}")
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.userId").value("O usuário é obrigatório."))
                .andExpect(jsonPath("$.fields.bookId").value("O livro é obrigatório."))
                .andExpect(jsonPath("$.fields.startDate").exists())
                .andExpect(jsonPath("$.fields.endDate").exists());
    }

    @Test
    void returns404ForUnknownUserOrBook() throws Exception {
        long userId = idOf("/users", "name", "Isabela Rocha");
        long bookId = idOf("/books", "title", "Vidas Secas");

        postJson("/lendings", lendingJson(999999, bookId, today, today.plusDays(7)))
                .andExpect(apiError(404, "Not Found"))
                .andExpect(jsonPath("$.message").value("Usuário não encontrado."));
        postJson("/lendings", lendingJson(userId, 999999, today, today.plusDays(7)))
                .andExpect(apiError(404, "Not Found"))
                .andExpect(jsonPath("$.message").value("Livro não encontrado."));
        mockMvc.perform(get("/lendings/{id}", 999999))
                .andExpect(apiError(404, "Not Found"))
                .andExpect(jsonPath("$.message").value("Empréstimo não encontrado."));
        mockMvc.perform(patch("/lendings/{id}/return", 999999))
                .andExpect(apiError(404, "Not Found"));
    }

    @Test
    void returnsBookWithoutBodyUsingToday() throws Exception {
        long lendingId = openLendingIdOfBook("Dom Casmurro");
        long bookId = idOf("/books", "title", "Dom Casmurro");

        mockMvc.perform(patch("/lendings/{id}/return", lendingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.returnDate").value(today.toString()))
                .andExpect(jsonPath("$.book.available").value(true));

        mockMvc.perform(get("/books/{id}", bookId))
                .andExpect(jsonPath("$.available").value(true));

        // Devolver duas vezes não é permitido
        mockMvc.perform(patch("/lendings/{id}/return", lendingId))
                .andExpect(apiError(409, "Conflict"))
                .andExpect(jsonPath("$.message").value("Este empréstimo já foi devolvido."));
    }

    @Test
    void returnsBookWithInformedDateAndUserIsReleased() throws Exception {
        // Bruno só tem um empréstimo atrasado; depois de devolvê-lo, volta a poder pegar livros
        long lendingId = openLendingIdOfBook("Cem Anos de Solidão");
        LocalDate yesterday = today.minusDays(1);

        patchJson("/lendings/" + lendingId + "/return", """
                {"returnDate": "%s"}
                """.formatted(yesterday))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.returnDate").value(yesterday.toString()))
                .andExpect(jsonPath("$.user.debtFree").value(true));

        long brunoId = idOf("/users", "name", "Bruno Oliveira");
        long bookId = idOf("/books", "title", "Vidas Secas");
        postJson("/lendings", lendingJson(brunoId, bookId, today, today.plusDays(7)))
                .andExpect(status().isCreated());
    }

    @Test
    void acceptsEmptyJsonBodyOnReturn() throws Exception {
        long lendingId = openLendingIdOfBook("O Hobbit");
        mockMvc.perform(patch("/lendings/{id}/return", lendingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnDate").value(today.toString()));
    }

    @Test
    void rejectsInvalidReturnDates() throws Exception {
        // Empréstimo da Ana Souza começou há 3 dias
        long lendingId = openLendingIdOfBook("Dom Casmurro");

        patchJson("/lendings/" + lendingId + "/return", """
                {"returnDate": "%s"}
                """.formatted(today.plusDays(1)))
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.returnDate").value("A data de devolução não pode ser futura."));

        patchJson("/lendings/" + lendingId + "/return", """
                {"returnDate": "%s"}
                """.formatted(today.minusDays(10)))
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.returnDate")
                        .value("A data de devolução não pode ser anterior à data do empréstimo."));
    }

    @Test
    void lendingsCannotBeDeleted() throws Exception {
        long lendingId = openLendingIdOfBook("Dom Casmurro");
        mockMvc.perform(delete("/lendings/{id}", lendingId))
                .andExpect(apiError(405, "Method Not Allowed"));
    }
}
