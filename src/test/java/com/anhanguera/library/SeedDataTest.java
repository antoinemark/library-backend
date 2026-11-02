package com.anhanguera.library;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que o data.sql respeita as regras combinadas para o seed.
 */
class SeedDataTest extends IntegrationTestSupport {

    @Test
    void seedHasExpectedAmounts() throws Exception {
        mockMvc.perform(get("/authors")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(12)));
        mockMvc.perform(get("/genres")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(9)));
        mockMvc.perform(get("/books")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(20)));
        mockMvc.perform(get("/users")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(10)));
        mockMvc.perform(get("/lendings")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(14)));
    }

    @Test
    void everyAuthorHasAtLeastOneBook() throws Exception {
        mockMvc.perform(get("/authors"))
                .andExpect(jsonPath("$[*].bookCount", everyItem(greaterThanOrEqualTo(1))));
    }

    @Test
    void lendingsCoverAllStatusesRelativeToToday() throws Exception {
        mockMvc.perform(get("/lendings"))
                .andExpect(jsonPath("$[*].status", hasItems("ATIVO", "ATRASADO", "CONCLUIDO")));
        mockMvc.perform(get("/lendings").param("status", "CONCLUIDO"))
                .andExpect(jsonPath("$[*].returnDate", everyItem(notNullValue())));
    }

    @Test
    void onlyMockUsersWithPendingLendingsAreNotDebtFree() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(jsonPath("$[?(@.debtFree == false)].name",
                        containsInAnyOrder("Bruno Oliveira", "Elena Rostova", "Heitor Costa")));
        mockMvc.perform(get("/lendings").param("status", "ATRASADO"))
                .andExpect(jsonPath("$[*].user.name",
                        containsInAnyOrder("Bruno Oliveira", "Elena Rostova", "Heitor Costa")));
    }

    @Test
    void usersWithPendingLendingsAlsoHaveFinishedHistory() throws Exception {
        for (String name : new String[]{"Bruno Oliveira", "Elena Rostova", "Heitor Costa"}) {
            long userId = idOf("/users", "name", name);
            mockMvc.perform(get("/lendings").param("userId", String.valueOf(userId)))
                    .andExpect(jsonPath("$[*].status", hasItems("ATRASADO", "CONCLUIDO")));
        }
    }

    @Test
    void unavailableBooksMatchOpenLendings() throws Exception {
        // Cada empréstimo em aberto (ATIVO ou ATRASADO) torna exatamente um livro indisponível
        mockMvc.perform(get("/books"))
                .andExpect(jsonPath("$[?(@.available == false)]", hasSize(7)));
        mockMvc.perform(get("/lendings").param("status", "ATIVO"))
                .andExpect(jsonPath("$", hasSize(4)));
        mockMvc.perform(get("/lendings").param("status", "ATRASADO"))
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void cpfsAreStoredWithOnlyDigits() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(jsonPath("$[*].cpf", everyItem(matchesPattern("\\d{11}"))));
    }
}
