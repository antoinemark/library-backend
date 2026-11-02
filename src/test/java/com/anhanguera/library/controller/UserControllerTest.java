package com.anhanguera.library.controller;

import com.anhanguera.library.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends IntegrationTestSupport {

    private static final String INVALID_CPF_MESSAGE =
            "O CPF deve conter exatamente 11 dígitos, com ou sem máscara (000.000.000-00).";

    @Test
    void listsUsersOrderedByNameWithDebtStatus() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ana Souza"))
                .andExpect(jsonPath("$[0].cpf").value("12345678901"))
                .andExpect(jsonPath("$[0].debtFree").value(true))
                .andExpect(jsonPath("$[1].name").value("Bruno Oliveira"))
                .andExpect(jsonPath("$[1].debtFree").value(false));
    }

    @Test
    void createsUserNormalizingMaskedCpf() throws Exception {
        ResultActions created = postJson("/users", """
                {"name": "Karina Alves", "cpf": "111.222.333-44"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Karina Alves"))
                .andExpect(jsonPath("$.cpf").value("11122233344"))
                .andExpect(jsonPath("$.debtFree").value(true));
        long id = idFrom(created);
        created.andExpect(header().string("Location", endsWith("/users/" + id)));

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("11122233344"));
    }

    @Test
    void rejectsDuplicatedCpfEvenWhenMasked() throws Exception {
        // 123.456.789-01 já pertence à Ana Souza (gravado como 12345678901)
        postJson("/users", """
                {"name": "Outra Pessoa", "cpf": "123.456.789-01"}
                """)
                .andExpect(apiError(409, "Conflict"))
                .andExpect(jsonPath("$.message").value("Já existe um usuário cadastrado com este CPF."));
    }

    @Test
    void rejectsCpfWithoutElevenDigits() throws Exception {
        postJson("/users", """
                {"name": "Fulano", "cpf": "123.456.789"}
                """)
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.cpf").value(INVALID_CPF_MESSAGE));

        postJson("/users", """
                {"name": "", "cpf": ""}
                """)
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.name").value("O nome é obrigatório."))
                .andExpect(jsonPath("$.fields.cpf").value("O CPF é obrigatório."));
    }

    @Test
    void rejectsCpfWithLettersOrOtherSymbols() throws Exception {
        // Só são aceitos os formatos 123.456.789-01 e 12345678901 (com espaços nas pontas removidos)
        for (String cpf : new String[]{"abc98765432109xyz", "CPF 987.654.321-09", "987 654 321 09", "987/654/321-09"}) {
            postJson("/users", """
                    {"name": "Fulano", "cpf": "%s"}
                    """.formatted(cpf))
                    .andExpect(apiError(400, "Bad Request"))
                    .andExpect(jsonPath("$.fields.cpf").value(INVALID_CPF_MESSAGE));
        }

        postJson("/users", """
                {"name": "Fulano", "cpf": " 987.654.321-09 "}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value("98765432109"));
    }

    @Test
    void updatesUserAndChecksCpfOfOtherUsers() throws Exception {
        long id = idOf("/users", "name", "Isabela Rocha");

        // Mantém o próprio CPF (com máscara): permitido
        putJson("/users/" + id, """
                {"name": "Isabela Rocha Lima", "cpf": "901.234.567-89"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Isabela Rocha Lima"))
                .andExpect(jsonPath("$.cpf").value("90123456789"));

        // CPF de outro usuário (Bruno Oliveira): conflito
        putJson("/users/" + id, """
                {"name": "Isabela Rocha Lima", "cpf": "23456789012"}
                """)
                .andExpect(apiError(409, "Conflict"));
    }

    @Test
    void returns404ForUnknownUser() throws Exception {
        mockMvc.perform(get("/users/{id}", 999999))
                .andExpect(apiError(404, "Not Found"))
                .andExpect(jsonPath("$.message").value("Usuário não encontrado."));
        putJson("/users/999999", """
                {"name": "Ninguém", "cpf": "99999999999"}
                """)
                .andExpect(apiError(404, "Not Found"));
    }

    @Test
    void doesNotDeleteUserWithLendings() throws Exception {
        long id = idOf("/users", "name", "Diego Ferreira");
        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(apiError(409, "Conflict"))
                .andExpect(jsonPath("$.message").value("Não é possível excluir o usuário, pois ele possui empréstimos registrados."));
    }

    @Test
    void deletesUserWithoutLendings() throws Exception {
        long id = idOf("/users", "name", "João Pereira");
        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isNotFound());
    }
}
