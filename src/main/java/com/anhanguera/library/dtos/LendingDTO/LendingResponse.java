package com.anhanguera.library.dtos.LendingDTO;

import com.anhanguera.library.dtos.BookDTO.BookResponse;
import com.anhanguera.library.dtos.UserDTO.UserResponse;
import com.anhanguera.library.model.LendingStatus;

import java.time.LocalDate;

public record LendingResponse(
        Long id,
        LocalDate startDate,
        LocalDate endDate,
        // Nulo enquanto o livro não for devolvido
        LocalDate returnDate,
        LendingStatus status,
        UserResponse user,
        BookResponse book
) {}
