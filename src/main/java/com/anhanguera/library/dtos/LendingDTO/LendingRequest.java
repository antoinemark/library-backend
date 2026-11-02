package com.anhanguera.library.dtos.LendingDTO;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LendingRequest(
        @NotNull(message = "O usuário é obrigatório.")
        Long userId,

        @NotNull(message = "O livro é obrigatório.")
        Long bookId,

        @NotNull(message = "A data do empréstimo é obrigatória.")
        LocalDate startDate,

        // Data prevista para a devolução
        @NotNull(message = "A data prevista de devolução é obrigatória.")
        LocalDate endDate
) {}
