package com.anhanguera.library.exception;

import lombok.Getter;

/**
 * Erro de validação de um campo detectado pelo service (ex.: regras que envolvem
 * mais de um campo). Respondida com HTTP 400, com o campo preenchido em "fields".
 */
@Getter
public class FieldValidationException extends RuntimeException {

    private final String field;

    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }
}
