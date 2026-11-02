package com.anhanguera.library.dtos.BookDTO;

import com.anhanguera.library.util.TextNormalizer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BookRequest(
        @NotBlank(message = "O título é obrigatório.")
        @Size(min = 2, max = 255, message = "O título deve ter entre 2 e 255 caracteres.")
        String title,

        @NotNull(message = "O autor é obrigatório.")
        Long authorId,

        @NotNull(message = "O gênero é obrigatório.")
        Long genreId,

        @NotNull(message = "A data de publicação é obrigatória.")
        @PastOrPresent(message = "A data de publicação não pode ser futura.")
        LocalDate publishDate,

        @NotBlank(message = "A editora é obrigatória.")
        @Size(max = 255, message = "A editora deve ter no máximo 255 caracteres.")
        String publisher
) {

    // Remove os espaços das pontas antes da validação, para que o tamanho mínimo
    // do título seja conferido sobre o texto que de fato será gravado
    public BookRequest {
        title = TextNormalizer.strip(title);
        publisher = TextNormalizer.strip(publisher);
    }
}
