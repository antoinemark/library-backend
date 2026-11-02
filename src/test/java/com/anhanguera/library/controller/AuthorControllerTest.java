package com.anhanguera.library.controller;

import com.anhanguera.library.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthorControllerTest extends IntegrationTestSupport {

    @Test
    void listsAuthorsOrderedByNameWithBookCount() throws Exception {
        mockMvc.perform(get("/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Agatha Christie"))
                .andExpect(jsonPath("$[0].bookCount").value(2))
                .andExpect(jsonPath("$[?(@.name == 'Machado de Assis')].bookCount").value(2));
    }

    @Test
    void listsAuthorsInPortugueseAlphabeticalOrder() throws Exception {
        postJson("/authors", """
                {"name": "Érico Veríssimo"}
                """).andExpect(status().isCreated());
        postJson("/authors", """
                {"name": "ana maria machado"}
                """).andExpect(status().isCreated());

        List<String> names = JsonPath.read(body(mockMvc.perform(get("/authors")).andExpect(status().isOk())), "$[*].name");
        // Inicial acentuada e inicial minúscula não vão para o fim da lista
        assertThat(names.indexOf("Érico Veríssimo")).isEqualTo(names.indexOf("Clarice Lispector") + 1);
        assertThat(names.indexOf("ana maria machado")).isEqualTo(names.indexOf("Aldous Huxley") + 1);
    }

    @Test
    void createsAuthorReturningLocation() throws Exception {
        ResultActions created = postJson("/authors", """
                {"name": "  Lygia Fagundes Telles  "}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Lygia Fagundes Telles"))
                .andExpect(jsonPath("$.bookCount").value(0));
        long id = idFrom(created);
        created.andExpect(header().string("Location", endsWith("/authors/" + id)));

        mockMvc.perform(get("/authors/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Lygia Fagundes Telles"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        postJson("/authors", """
                {"name": "   "}
                """)
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.name").value("O nome do autor é obrigatório."));
    }

    @Test
    void rejectsNameMadeOnlyOfNonBreakingSpaces() throws Exception {
        // U+00A0 (espaço não separável, comum em texto copiado da web) não é removido pelo trim()
        postJson("/authors", """
                {"name": "\\u00a0\\u00a0"}
                """)
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.name").value("O nome do autor é obrigatório."));

        postJson("/authors", """
                {"name": "\\u00a0Lygia Fagundes Telles\\u00a0"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Lygia Fagundes Telles"));
    }

    @Test
    void updatesAuthor() throws Exception {
        long id = idOf("/authors", "name", "Mary Shelley");
        putJson("/authors/" + id, """
                {"name": "Mary Wollstonecraft Shelley"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Mary Wollstonecraft Shelley"))
                .andExpect(jsonPath("$.bookCount").value(1));
    }

    @Test
    void returns404ForUnknownAuthor() throws Exception {
        mockMvc.perform(get("/authors/{id}", 999999))
                .andExpect(apiError(404, "Not Found"))
                .andExpect(jsonPath("$.message").value("Autor não encontrado."))
                .andExpect(jsonPath("$.fields").value(nullValue()));
        putJson("/authors/999999", """
                {"name": "Ninguém"}
                """)
                .andExpect(apiError(404, "Not Found"));
        mockMvc.perform(delete("/authors/{id}", 999999))
                .andExpect(apiError(404, "Not Found"));
    }

    @Test
    void returns400ForNonNumericId() throws Exception {
        mockMvc.perform(get("/authors/abc"))
                .andExpect(apiError(400, "Bad Request"));
    }

    @Test
    void doesNotDeleteAuthorWithBooks() throws Exception {
        long id = idOf("/authors", "name", "Machado de Assis");
        mockMvc.perform(delete("/authors/{id}", id))
                .andExpect(apiError(409, "Conflict"))
                .andExpect(jsonPath("$.message").value("Não é possível excluir o autor, pois ele possui livros cadastrados."));
    }

    @Test
    void deletesAuthorWithoutBooks() throws Exception {
        long id = idFrom(postJson("/authors", """
                {"name": "Autor Sem Livros"}
                """).andExpect(status().isCreated()));

        mockMvc.perform(delete("/authors/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/authors/{id}", id))
                .andExpect(status().isNotFound());
    }
}
