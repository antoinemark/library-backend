package com.anhanguera.library.config;

import com.anhanguera.library.exception.ApiError;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.cors.DefaultCorsProcessor;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Processador de CORS do Spring MVC que responde às requisições de origens não liberadas
 * com o corpo de erro padrão da API ({@link ApiError}), em vez do texto "Invalid CORS request".
 * As requisições permitidas seguem exatamente o comportamento do {@link DefaultCorsProcessor}.
 */
public class ApiErrorCorsProcessor extends DefaultCorsProcessor {

    private static final String MESSAGE = "Origem não permitida pelo CORS da API. Acesse por uma das origens liberadas.";

    private final ObjectProvider<JsonMapper> jsonMapper;

    public ApiErrorCorsProcessor(ObjectProvider<JsonMapper> jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected void rejectRequest(ServerHttpResponse response) throws IOException {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ApiError body = ApiError.of(HttpStatus.FORBIDDEN, MESSAGE, null);
        response.getBody().write(jsonMapper.getObject().writeValueAsBytes(body));
        response.flush();
    }
}
