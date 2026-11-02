package com.anhanguera.library.dtos.UserDTO;

import com.anhanguera.library.util.TextNormalizer;
import com.anhanguera.library.validation.ValidCpf;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
        String name,

        // Aceita com ou sem máscara (123.456.789-01 ou 12345678901)
        @NotBlank(message = "O CPF é obrigatório.")
        @ValidCpf
        String cpf
) {

    // Remove os espaços das pontas antes da validação, que passa a valer para o texto gravado
    public UserRequest {
        name = TextNormalizer.strip(name);
        cpf = TextNormalizer.strip(cpf);
    }
}
