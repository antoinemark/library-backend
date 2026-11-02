package com.anhanguera.library.util;

import java.text.Collator;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

/**
 * Ordem alfabética das listagens (autores, gêneros, livros e usuários).
 * <p>
 * Compara palavra por palavra, como num índice: o espaço vem antes das letras, então
 * "A Hora da Estrela" fica antes de "Admirável Mundo Novo". Acentos e maiúsculas/minúsculas
 * não mudam a posição ("Érico Veríssimo" fica junto dos nomes com "E" e "ana" junto dos
 * nomes com "A"); eles só desempatam textos que, sem eles, seriam iguais.
 * <p>
 * A ordenação é feita em memória porque o ORDER BY do H2 compara pelo código Unicode,
 * o que joga iniciais acentuadas e minúsculas para o fim da lista. O Collator do Java
 * sozinho também não serve: ele ignora os espaços ("Ohio" antes de "O Hobbit").
 */
public final class AlphabeticalOrder {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private static final Comparator<String> TEXT = Comparator
            .comparing(AlphabeticalOrder::baseForm)
            .thenComparing(Collator.getInstance(PT_BR))
            .thenComparing(Comparator.naturalOrder());

    private AlphabeticalOrder() {
    }

    /**
     * Comparador que ordena os objetos pelo texto extraído (ex.: {@code by(Author::getName)}).
     */
    public static <T> Comparator<T> by(Function<? super T, String> keyExtractor) {
        return Comparator.comparing(keyExtractor, TEXT);
    }

    // Texto sem acentos e em minúsculas (ex.: "Érico Veríssimo" -> "erico verissimo")
    private static String baseForm(String text) {
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(PT_BR);
    }
}
