package com.anhanguera.library.dtos.GenreDTO;

import com.anhanguera.library.util.TextNormalizer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenreRequest(
        @NotBlank(message = "O nome do gênero é obrigatório.")
        @Size(max = 255, message = "O nome do gênero deve ter no máximo 255 caracteres.")
        String name
) {

    // Remove os espaços das pontas antes da validação, que passa a valer para o texto gravado
    public GenreRequest {
        name = TextNormalizer.strip(name);
    }
}
