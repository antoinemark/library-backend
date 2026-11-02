package com.anhanguera.library.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.time.Instant;
import java.util.Map;

/**
 * Corpo padrão das respostas de erro da API.
 *
 * @param status    código HTTP
 * @param error     descrição padrão do código HTTP (ex.: "Not Found")
 * @param message   mensagem legível ao usuário, em português
 * @param fields    erros por campo (somente em erros de validação; nulo nos demais casos)
 * @param timestamp momento do erro, em ISO-8601
 */
public record ApiError(
        int status,
        String error,
        String message,
        Map<String, String> fields,
        Instant timestamp
) {

    public static ApiError of(HttpStatusCode status, String message, Map<String, String> fields) {
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        String error = httpStatus != null ? httpStatus.getReasonPhrase() : String.valueOf(status.value());
        return new ApiError(status.value(), error, message, fields, Instant.now());
    }
}
