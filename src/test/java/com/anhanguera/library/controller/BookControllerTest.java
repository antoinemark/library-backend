package com.anhanguera.library.controller;

import com.anhanguera.library.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookControllerTest extends IntegrationTestSupport {

    @Test
    void listsBooksOrderedByTitleWithIsoDates() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("1984"))
                .andExpect(jsonPath("$[0].publishDate").value("1949-06-08"))
                .andExpect(jsonPath("$[0].author.name").value("George Orwell"))
                .andExpect(jsonPath("$[0].genre.name").value("Distopia"))
                // 1984 está com empréstimo em aberto (atrasado) no seed
                .andExpect(jsonPath("$[0].available").value(false));
    }

    @Test
    void getsBookById() throws Exception {
        long id = idOf("/books", "title", "Vidas Secas");
        mockMvc.perform(get("/books/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Vidas Secas"))
                .andExpect(jsonPath("$.author.id").isNumber())
                .andExpect(jsonPath("$.author.name").value("Graciliano Ramos"))
                .andExpect(jsonPath("$.genre.name").value("Literatura Brasileira"))
                .andExpect(jsonPath("$.publishDate").value("1938-01-01"))
                .andExpect(jsonPath("$.publisher").value("Livraria José Olympio Editora"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void createsAndUpdatesBook() throws Exception {
        long authorId = idOf("/authors", "name", "Machado de Assis");
        long genreId = idOf("/genres", "name", "Literatura Brasileira");

        ResultActions created = postJson("/books", """
                {"title": "Quincas Borba", "authorId": %d, "genreId": %d,
                 "publishDate": "1891-01-01", "publisher": "Garnier"}
                """.formatted(authorId, genreId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Quincas Borba"))
                .andExpect(jsonPath("$.author.id").value(authorId))
                .andExpect(jsonPath("$.author.name").value("Machado de Assis"))
                .andExpect(jsonPath("$.genre.id").value(genreId))
                .andExpect(jsonPath("$.publishDate").value("1891-01-01"))
                .andExpect(jsonPath("$.available").value(true));
        long id = idFrom(created);
        created.andExpect(header().string("Location", endsWith("/books/" + id)));

        long otherGenreId = idOf("/genres", "name", "Romance");
        putJson("/books/" + id, """
                {"title": "Quincas Borba", "authorId": %d, "genreId": %d,
                 "publishDate": "1891-12-01", "publisher": "B. L. Garnier"}
                """.formatted(authorId, otherGenreId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genre.name").value("Romance"))
                .andExpect(jsonPath("$.publishDate").value("1891-12-01"))
                .andExpect(jsonPath("$.publisher").value("B. L. Garnier"));

        mockMvc.perform(get("/authors/{id}", authorId))
                .andExpect(jsonPath("$.bookCount").value(3));
    }

    @Test
    void returns404ForUnknownAuthorOrGenre() throws Exception {
        long authorId = idOf("/authors", "name", "Machado de Assis");
        long genreId = idOf("/genres", "name", "Romance");

        postJson("/books", """
                {"title": "Livro", "authorId": 999999, "genreId": %d,
                 "publishDate": "2000-01-01", "publisher": "Editora"}
                """.formatted(genreId))
                .andExpect(apiError(404, "Not Found"))
                .andExpect(jsonPath("$.message").value("Autor não encontrado."));

        postJson("/books", """
                {"title": "Livro", "authorId": %d, "genreId": 999999,
                 "publishDate": "2000-01-01", "publisher": "Editora"}
                """.formatted(authorId))
                .andExpect(apiError(404, "Not Found"))
                .andExpect(jsonPath("$.message").value("Gênero não encontrado."));

        mockMvc.perform(get("/books/{id}", 999999))
                .andExpect(apiError(404, "Not Found"))
                .andExpect(jsonPath("$.message").value("Livro não encontrado."));
    }

    @Test
    void validatesRequiredFieldsAndFuturePublishDate() throws Exception {
        postJson("/books", "{}")
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.title").exists())
                .andExpect(jsonPath("$.fields.authorId").exists())
                .andExpect(jsonPath("$.fields.genreId").exists())
                .andExpect(jsonPath("$.fields.publishDate").exists())
                .andExpect(jsonPath("$.fields.publisher").exists());

        long authorId = idOf("/authors", "name", "Machado de Assis");
        long genreId = idOf("/genres", "name", "Romance");
        postJson("/books", """
                {"title": "A", "authorId": %d, "genreId": %d,
                 "publishDate": "%s", "publisher": "Editora"}
                """.formatted(authorId, genreId, LocalDate.now().plusDays(1)))
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.title").value("O título deve ter entre 2 e 255 caracteres."))
                .andExpect(jsonPath("$.fields.publishDate").value("A data de publicação não pode ser futura."));
    }

    @Test
    void validatesTitleLengthAfterTrimming() throws Exception {
        long authorId = idOf("/authors", "name", "Machado de Assis");
        long genreId = idOf("/genres", "name", "Romance");
        // " A " tem 3 caracteres, mas só 1 depois de remover os espaços das pontas
        postJson("/books", """
                {"title": " A ", "authorId": %d, "genreId": %d,
                 "publishDate": "2000-01-01", "publisher": "Editora"}
                """.formatted(authorId, genreId))
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.title").value("O título deve ter entre 2 e 255 caracteres."));

        postJson("/books", """
                {"title": "  Iracema  ", "authorId": %d, "genreId": %d,
                 "publishDate": "1865-01-01", "publisher": "  Typographia de Vianna  "}
                """.formatted(authorId, genreId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Iracema"))
                .andExpect(jsonPath("$.publisher").value("Typographia de Vianna"));
    }

    @Test
    void listsBooksInPortugueseAlphabeticalOrder() throws Exception {
        long authorId = idOf("/authors", "name", "Machado de Assis");
        long genreId = idOf("/genres", "name", "Romance");
        for (String title : new String[]{"Ébano", "ilíada"}) {
            postJson("/books", """
                    {"title": "%s", "authorId": %d, "genreId": %d,
                     "publishDate": "2000-01-01", "publisher": "Editora"}
                    """.formatted(title, authorId, genreId))
                    .andExpect(status().isCreated());
        }

        List<String> titles = JsonPath.read(body(mockMvc.perform(get("/books")).andExpect(status().isOk())), "$[*].title");
        // Inicial acentuada fica junto da letra sem acento e minúscula não vai para o fim da lista
        assertThat(titles.indexOf("Ébano")).isEqualTo(titles.indexOf("E Não Sobrou Nenhum") + 1);
        assertThat(titles.indexOf("ilíada")).isEqualTo(titles.indexOf("Gabriela, Cravo e Canela") + 1);
    }

    @Test
    void rejectsDateOutsideIsoFormat() throws Exception {
        long authorId = idOf("/authors", "name", "Machado de Assis");
        long genreId = idOf("/genres", "name", "Romance");
        postJson("/books", """
                {"title": "Livro", "authorId": %d, "genreId": %d,
                 "publishDate": "01/01/2000", "publisher": "Editora"}
                """.formatted(authorId, genreId))
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.publishDate").value("Data inválida. Use o formato AAAA-MM-DD."));
    }

    @Test
    void rejectsNonNumericIdInBody() throws Exception {
        long genreId = idOf("/genres", "name", "Romance");
        postJson("/books", """
                {"title": "Livro", "authorId": "abc", "genreId": %d,
                 "publishDate": "2000-01-01", "publisher": "Editora"}
                """.formatted(genreId))
                .andExpect(apiError(400, "Bad Request"))
                .andExpect(jsonPath("$.fields.authorId").value("O valor informado deve ser numérico."));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        postJson("/books", "{\"title\": ")
                .andExpect(apiError(400, "Bad Request"));
    }

    @Test
    void doesNotDeleteBookWithLendingHistory() throws Exception {
        // Fundação só tem empréstimo concluído, mas o histórico deve ser preservado
        long id = idOf("/books", "title", "Fundação");
        mockMvc.perform(delete("/books/{id}", id))
                .andExpect(apiError(409, "Conflict"))
                .andExpect(jsonPath("$.message").value("Não é possível excluir o livro, pois ele possui empréstimos registrados."));
    }

    @Test
    void deletesBookWithoutLendings() throws Exception {
        long id = idOf("/books", "title", "São Bernardo");
        mockMvc.perform(delete("/books/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/books/{id}", id))
                .andExpect(status().isNotFound());
    }
}
