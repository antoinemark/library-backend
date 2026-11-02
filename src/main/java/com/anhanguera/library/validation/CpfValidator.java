package com.anhanguera.library.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class CpfValidator implements ConstraintValidator<ValidCpf, String> {

    // 11 dígitos, com ou sem a máscara (123.456.789-01 ou 12345678901). Letras e outros
    // símbolos são recusados, em vez de descartados. Os dígitos verificadores não são
    // conferidos: os CPFs do seed e dos testes são fictícios.
    private static final Pattern CPF_FORMAT = Pattern.compile("\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}");

    /**
     * Remove tudo que não for dígito (ex.: "123.456.789-01" -> "12345678901").
     */
    public static String normalize(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return CPF_FORMAT.matcher(value).matches();
    }
}
