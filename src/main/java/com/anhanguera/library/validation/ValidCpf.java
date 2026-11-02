package com.anhanguera.library.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida um CPF com ou sem máscara: aceita 11 dígitos no formato 123.456.789-01 ou
 * 12345678901 (letras e outros símbolos são recusados). Valores nulos ou em branco são
 * ignorados (use @NotBlank para torná-lo obrigatório).
 */
@Documented
@Constraint(validatedBy = CpfValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCpf {

    String message() default "O CPF deve conter exatamente 11 dígitos, com ou sem máscara (000.000.000-00).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
