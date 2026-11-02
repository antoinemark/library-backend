package com.anhanguera.library.controller;

import com.anhanguera.library.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiErrorAndCorsTest extends IntegrationTestSupport {

    private static final String CORS_REJECTED_MESSAGE =
            "Origem não permitida pelo CORS da API. Acesse por uma das origens liberadas.";

    @Test
    void unknownRouteUsesApiErrorFormat() throws Exception {
        mockMvc.perform(get("/rota-que-nao-existe"))
                .andExpect(apiError(404, "Not Found"));
    }

    @Test
    void unsupportedContentTypeUsesApiErrorFormat() throws Exception {
        mockMvc.perform(post("/authors").contentType(MediaType.TEXT_PLAIN).content("Fulano"))
                .andExpect(apiError(415, "Unsupported Media Type"));
    }

    @Test
    void missingBodyIsBadRequest() throws Exception {
        mockMvc.perform(post("/authors").contentType(MediaType.APPLICATION_JSON))
                .andExpect(apiError(400, "Bad Request"));
    }

    @Test
    void openApiDocumentIsAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/lendings/{id}/return']").exists())
                .andExpect(jsonPath("$.tags[*].name", hasItem("Empréstimos")));
    }

    @Test
    void allowsFrontendOriginOnPreflight() throws Exception {
        mockMvc.perform(options("/lendings/1/return")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PATCH")));
    }

    @Test
    void rejectsUnknownOriginOnPreflight() throws Exception {
        mockMvc.perform(options("/authors")
                        .header(HttpHeaders.ORIGIN, "http://exemplo-malicioso.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
                .andExpect(apiError(403, "Forbidden"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(CORS_REJECTED_MESSAGE));
    }

    @Test
    void rejectsUnknownOriginOnActualRequestUsingApiErrorFormat() throws Exception {
        mockMvc.perform(post("/genres")
                        .header(HttpHeaders.ORIGIN, "http://exemplo-malicioso.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Crônica\"}"))
                .andExpect(apiError(403, "Forbidden"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(CORS_REJECTED_MESSAGE));
    }

    @Test
    void locationHeaderUsesForwardedHeadersFromProxy() throws Exception {
        // Atrás de um proxy que publica a API em /api (Nginx do frontend, proxy do Vite),
        // o Location do 201 deve apontar para a URL pública, com o prefixo
        mockMvc.perform(post("/genres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Crônica\"}")
                        .header("X-Forwarded-Proto", "http")
                        .header("X-Forwarded-Host", "localhost:8081")
                        .header("X-Forwarded-Prefix", "/api"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("http://localhost:8081/api/genres/")));
    }
}
