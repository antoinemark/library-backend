package com.anhanguera.library.exception;

/**
 * Violação de regra de negócio ou conflito com o estado atual dos dados. Respondida com HTTP 409.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
