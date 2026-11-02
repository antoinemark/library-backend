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

class GenreControllerTest extends IntegrationTestSupport {

    @Test
    void listsGenresOrderedByNameWithBookCount() throws Exception {
        mockMvc.perform(get("/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Distopia"))
                .andExpect(jsonPath("$[0].bookCount").value(3))
                .andExpect(jsonPath("$[?(@.name == 'Literatura Brasileira')].bookCount").value(5));
    }

    @Test
    void createsUpdatesAndDeletesGenre() throws Exception {
        ResultActions created = postJson("/genres", """
                {"name": "Biografia"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Biografia"))
                .andExpect(jsonPath("$.bookCount").value(0));
        long id = idFrom(created);
        created.andExpect(header().string("Location", endsWith("/genres/" + id)));

        putJson("/genres/" + id, """
                {"name": "Biografias"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Biografias"));

        mockMvc.perform(delete("/genres/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/genres/{id}", id))
                .andExpect(apiError(404, "Not Found"))
                .andExpect(jsonPath("$.message").value("Gênero não encontrado."));
    }

    @Test
    void rejectsMissingName() throws Exception {
        postJson("/genres", "{}")
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.name").value("O nome do gênero é obrigatório."));
    }

    @Test
    void validatesNameLengthAfterRemovingEdgeSpaces() throws Exception {
        // O limite de 255 caracteres vale para o nome que será gravado, sem os espaços das pontas
        String name = "G".repeat(255);
        postJson("/genres", """
                {"name": " \\u00a0%s "}
                """.formatted(name))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name));

        postJson("/genres", """
                {"name": "%s"}
                """.formatted(name + "G"))
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.name").value("O nome do gênero deve ter no máximo 255 caracteres."));
    }

    @Test
    void doesNotDeleteGenreWithBooks() throws Exception {
        long id = idOf("/genres", "name", "Fantasia");
        mockMvc.perform(delete("/genres/{id}", id))
                .andExpect(apiError(409, "Conflict"));
    }
}
