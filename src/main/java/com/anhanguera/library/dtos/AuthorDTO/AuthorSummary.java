package com.anhanguera.library.dtos.AuthorDTO;

/**
 * Representação resumida do autor, usada dentro de BookResponse.
 */
public record AuthorSummary(
        Long id,
        String name
) {}
