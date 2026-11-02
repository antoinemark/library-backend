package com.anhanguera.library.model;

import java.time.LocalDate;

/**
 * Situação de um empréstimo. Não é persistida: é calculada a partir das datas.
 */
public enum LendingStatus {
    ATIVO,
    ATRASADO,
    CONCLUIDO;

    /**
     * CONCLUIDO se o livro já foi devolvido; senão ATRASADO se a data prevista
     * de devolução já passou; senão ATIVO.
     */
    public static LendingStatus of(Lending lending, LocalDate today) {
        if (lending.getReturnDate() != null) {
            return CONCLUIDO;
        }
        if (today.isAfter(lending.getEndDate())) {
            return ATRASADO;
        }
        return ATIVO;
    }
}
