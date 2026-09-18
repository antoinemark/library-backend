package com.anhanguera.library.dtos.BookDTO;

import com.anhanguera.library.model.Author;
import com.anhanguera.library.model.Genre;

import java.util.Date;

public record BookResponse(
        Long id,
        String title,
        Author author,
        Genre genre,
        Date publishDate,
        String publisher
) {}
