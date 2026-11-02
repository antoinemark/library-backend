package com.anhanguera.library.service;

import com.anhanguera.library.dtos.AuthorDTO.AuthorRequest;
import com.anhanguera.library.dtos.AuthorDTO.AuthorResponse;
import com.anhanguera.library.exception.BusinessRuleException;
import com.anhanguera.library.exception.ResourceNotFoundException;
import com.anhanguera.library.model.Author;
import com.anhanguera.library.repository.AuthorRepository;
import com.anhanguera.library.repository.BookRepository;
import com.anhanguera.library.util.AlphabeticalOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public List<AuthorResponse> getAuthors() {
        return authorRepository.findAll()
                .stream()
                .sorted(AlphabeticalOrder.by(Author::getName).thenComparing(Author::getId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuthorResponse getAuthorById(Long id) {
        return toResponse(findAuthor(id));
    }

    @Transactional
    public AuthorResponse createAuthor(AuthorRequest request) {
        Author author = new Author();
        author.setName(request.name());
        return toResponse(authorRepository.save(author));
    }

    @Transactional
    public AuthorResponse updateAuthor(Long id, AuthorRequest request) {
        Author author = findAuthor(id);
        author.setName(request.name());
        return toResponse(authorRepository.save(author));
    }

    @Transactional
    public void deleteAuthor(Long id) {
        Author author = findAuthor(id);
        if (bookRepository.existsByAuthorId(id)) {
            throw new BusinessRuleException("Não é possível excluir o autor, pois ele possui livros cadastrados.");
        }
        authorRepository.delete(author);
    }

    /**
     * Busca a entidade ou lança 404. Também usada pelo BookService.
     */
    @Transactional(readOnly = true)
    public Author findAuthor(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autor não encontrado."));
    }

    private AuthorResponse toResponse(Author author) {
        return new AuthorResponse(
                author.getId(),
                author.getName(),
                bookRepository.countByAuthorId(author.getId())
        );
    }
}
