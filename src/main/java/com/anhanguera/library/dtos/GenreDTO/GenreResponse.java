package com.anhanguera.library.dtos.GenreDTO;

public record GenreResponse(
        Long id,
        String name,
        long bookCount
) {}
