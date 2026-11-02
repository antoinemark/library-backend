package com.anhanguera.library.dtos.AuthorDTO;

import com.anhanguera.library.util.TextNormalizer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthorRequest (
        @NotBlank(message = "O nome do autor é obrigatório.")
        @Size(max = 255, message = "O nome do autor deve ter no máximo 255 caracteres.")
        String name
){

    // Remove os espaços das pontas antes da validação, que passa a valer para o texto gravado
    public AuthorRequest {
        name = TextNormalizer.strip(name);
    }
}
