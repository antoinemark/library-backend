package com.anhanguera.library.util;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class AlphabeticalOrderTest {

    private final Comparator<String> order = AlphabeticalOrder.by(Function.identity());

    @Test
    void comparesWordByWord() {
        // o espaço vem antes das letras, como num índice
        assertThat(sorted("Admirável Mundo Novo", "A Hora da Estrela", "Ohio", "O Hobbit"))
                .containsExactly("A Hora da Estrela", "Admirável Mundo Novo", "O Hobbit", "Ohio");
    }

    @Test
    void ignoresAccentsAndCaseWhenPositioning() {
        assertThat(sorted("Mary Shelley", "Érico Veríssimo", "Clarice Lispector", "ana maria machado", "Aldous Huxley"))
                .containsExactly("Aldous Huxley", "ana maria machado", "Clarice Lispector", "Érico Veríssimo", "Mary Shelley");
    }

    @Test
    void putsNumbersBeforeLetters() {
        assertThat(sorted("Dom Casmurro", "1984", "A Hora da Estrela"))
                .containsExactly("1984", "A Hora da Estrela", "Dom Casmurro");
    }

    @Test
    void usesAccentsAndCaseOnlyAsTieBreaker() {
        assertThat(order.compare("Erico", "Érico")).isNegative();
        assertThat(order.compare("ana", "Ana")).isNotZero();
        assertThat(order.compare("Ana", "Ana")).isZero();
    }

    private List<String> sorted(String... values) {
        return List.of(values).stream().sorted(order).toList();
    }
}
