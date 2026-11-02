package com.anhanguera.library.util;

import java.util.regex.Pattern;

/**
 * Normalização dos textos recebidos nas requisições.
 */
public final class TextNormalizer {

    // Espaços e caracteres de controle nas pontas, incluindo os que o String.trim() e o
    // String.strip() não removem, como o espaço não separável (U+00A0), comum em texto
    // copiado de páginas web
    private static final Pattern EDGE_SPACES = Pattern.compile("^[\\p{Z}\\p{Cc}]+|[\\p{Z}\\p{Cc}]+$");

    private TextNormalizer() {
    }

    /**
     * Remove os espaços das pontas do texto (nulo continua nulo).
     */
    public static String strip(String text) {
        return text == null ? null : EDGE_SPACES.matcher(text).replaceAll("");
    }
}
