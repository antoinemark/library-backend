package com.anhanguera.library;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integração: sobe a aplicação inteira (com o seed do data.sql)
 * e executa cada teste dentro de uma transação que é desfeita ao final, então os
 * dados criados/alterados por um teste não afetam os outros.
 * <p>
 * Todas as classes de teste devem estender esta base para compartilharem o mesmo
 * contexto do Spring (e, portanto, o mesmo banco H2 em memória, populado uma única vez).
 * O relógio da aplicação fica congelado no dia em que o seed foi gerado ({@link FixedClockTestConfig}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockTestConfig.class)
@Transactional
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected Clock clock;

    /**
     * Data de hoje segundo o relógio da aplicação (a mesma usada pelos services e pelo seed).
     */
    protected LocalDate today() {
        return LocalDate.now(clock);
    }

    protected ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions putJson(String url, String json) throws Exception {
        return mockMvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions patchJson(String url, String json) throws Exception {
        return mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * Lê o campo "id" do corpo da resposta.
     */
    protected static long idFrom(ResultActions result) throws Exception {
        Number id = JsonPath.read(body(result), "$.id");
        return id.longValue();
    }

    /**
     * Busca, na listagem de um recurso, o id do item cujo campo tem o valor informado.
     */
    protected long idOf(String collectionUrl, String field, String value) throws Exception {
        String json = body(mockMvc.perform(get(collectionUrl)).andExpect(status().isOk()));
        List<Number> ids = JsonPath.read(json, "$[?(@." + field + " == '" + value + "')].id");
        if (ids.isEmpty()) {
            throw new IllegalStateException("Nenhum item com " + field + " = " + value + " em " + collectionUrl);
        }
        return ids.getFirst().longValue();
    }

    /**
     * Verifica o formato padrão do corpo de erro da API.
     */
    protected static ResultMatcher apiError(int status, String error) {
        return result -> {
            status().is(status).match(result);
            jsonPath("$.status").value(status).match(result);
            jsonPath("$.error").value(error).match(result);
            jsonPath("$.message").value(not(emptyOrNullString())).match(result);
            jsonPath("$.timestamp").isString().match(result);
        };
    }
}
