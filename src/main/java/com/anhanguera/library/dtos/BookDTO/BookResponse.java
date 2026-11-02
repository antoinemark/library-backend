package com.anhanguera.library.dtos.BookDTO;

import com.anhanguera.library.dtos.AuthorDTO.AuthorSummary;
import com.anhanguera.library.dtos.GenreDTO.GenreSummary;

import java.time.LocalDate;

public record BookResponse(
        Long id,
        String title,
        AuthorSummary author,
        GenreSummary genre,
        LocalDate publishDate,
        String publisher,
        // true quando o livro não possui empréstimo em aberto
        boolean available
) {}
