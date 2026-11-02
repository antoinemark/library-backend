package com.anhanguera.library.dtos.LendingDTO;

import java.time.LocalDate;

/**
 * Corpo opcional da devolução. Sem returnDate, a devolução é registrada com a data de hoje.
 */
public record LendingReturnRequest(
        LocalDate returnDate
) {}
