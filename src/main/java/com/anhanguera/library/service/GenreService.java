package com.anhanguera.library.service;

import com.anhanguera.library.dtos.GenreDTO.GenreRequest;
import com.anhanguera.library.dtos.GenreDTO.GenreResponse;
import com.anhanguera.library.model.Genre;
import com.anhanguera.library.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;

    public List<GenreResponse> getGenre() {
        return genreRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public GenreResponse getGenreById(Long id){
        Genre genre = genreRepository.findById(id).orElseThrow(() -> new RuntimeException("Gênero não encontrado"));
        return toResponse(genre);
    }

    public GenreResponse createGenre(GenreRequest request){
        Genre genre = Genre.builder()
                .name(request.name())
                .build();

        Genre savedGenre = genreRepository.save(genre);

        return toResponse(savedGenre);
    }

    public Genre updateGenre(Long id, Genre genre) {
        Genre existingGenre = genreRepository.findById(id).orElseThrow(() -> new RuntimeException("Gênero não encontrado"));
        existingGenre.setName(genre.getName());
        return genreRepository.save(existingGenre);
    }

    public void deleteGenre(Long id) {
        Genre existingGenre = genreRepository.findById(id).orElseThrow(() -> new RuntimeException("Gênero não encontrado"));
        genreRepository.delete(existingGenre);
    }

    private GenreResponse toResponse(Genre genre) {
        return new GenreResponse(
                genre.getId(),
                genre.getName()
        );
    }
}
