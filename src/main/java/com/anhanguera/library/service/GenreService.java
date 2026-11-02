package com.anhanguera.library.service;

import com.anhanguera.library.dtos.GenreDTO.GenreRequest;
import com.anhanguera.library.dtos.GenreDTO.GenreResponse;
import com.anhanguera.library.exception.BusinessRuleException;
import com.anhanguera.library.exception.ResourceNotFoundException;
import com.anhanguera.library.model.Genre;
import com.anhanguera.library.repository.BookRepository;
import com.anhanguera.library.repository.GenreRepository;
import com.anhanguera.library.util.AlphabeticalOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;
    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public List<GenreResponse> getGenres() {
        return genreRepository.findAll()
                .stream()
                .sorted(AlphabeticalOrder.by(Genre::getName).thenComparing(Genre::getId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GenreResponse getGenreById(Long id) {
        return toResponse(findGenre(id));
    }

    @Transactional
    public GenreResponse createGenre(GenreRequest request) {
        Genre genre = Genre.builder()
                .name(request.name())
                .build();

        Genre savedGenre = genreRepository.save(genre);

        return toResponse(savedGenre);
    }

    @Transactional
    public GenreResponse updateGenre(Long id, GenreRequest request) {
        Genre existingGenre = findGenre(id);
        existingGenre.setName(request.name());
        return toResponse(genreRepository.save(existingGenre));
    }

    @Transactional
    public void deleteGenre(Long id) {
        Genre existingGenre = findGenre(id);
        if (bookRepository.existsByGenreId(id)) {
            throw new BusinessRuleException("Não é possível excluir o gênero, pois existem livros cadastrados nele.");
        }
        genreRepository.delete(existingGenre);
    }

    /**
     * Busca a entidade ou lança 404. Também usada pelo BookService.
     */
    @Transactional(readOnly = true)
    public Genre findGenre(Long id) {
        return genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gênero não encontrado."));
    }

    private GenreResponse toResponse(Genre genre) {
        return new GenreResponse(
                genre.getId(),
                genre.getName(),
                bookRepository.countByGenreId(genre.getId())
        );
    }
}
