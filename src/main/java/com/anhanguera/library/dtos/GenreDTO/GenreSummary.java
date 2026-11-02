package com.anhanguera.library.dtos.GenreDTO;

/**
 * Representação resumida do gênero, usada dentro de BookResponse.
 */
public record GenreSummary(
        Long id,
        String name
) {}
