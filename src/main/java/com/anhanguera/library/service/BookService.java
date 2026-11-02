package com.anhanguera.library.service;

import com.anhanguera.library.dtos.AuthorDTO.AuthorSummary;
import com.anhanguera.library.dtos.BookDTO.BookRequest;
import com.anhanguera.library.dtos.BookDTO.BookResponse;
import com.anhanguera.library.dtos.GenreDTO.GenreSummary;
import com.anhanguera.library.exception.BusinessRuleException;
import com.anhanguera.library.exception.ResourceNotFoundException;
import com.anhanguera.library.model.Book;
import com.anhanguera.library.repository.BookRepository;
import com.anhanguera.library.repository.LendRepository;
import com.anhanguera.library.util.AlphabeticalOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final LendRepository lendRepository;
    private final AuthorService authorService;
    private final GenreService genreService;

    @Transactional(readOnly = true)
    public List<BookResponse> getBooks() {
        return bookRepository.findAll()
                .stream()
                .sorted(AlphabeticalOrder.by(Book::getTitle).thenComparing(Book::getId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookResponse getBookById(Long id) {
        return toResponse(findBook(id));
    }

    @Transactional
    public BookResponse createBook(BookRequest request) {
        Book book = new Book();
        applyRequest(book, request);
        return toResponse(bookRepository.save(book));
    }

    @Transactional
    public BookResponse updateBook(Long id, BookRequest request) {
        Book book = findBook(id);
        applyRequest(book, request);
        return toResponse(bookRepository.save(book));
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = findBook(id);
        // O histórico de empréstimos é preservado, então livros já emprestados não podem ser excluídos
        if (lendRepository.existsByBookId(id)) {
            throw new BusinessRuleException("Não é possível excluir o livro, pois ele possui empréstimos registrados.");
        }
        bookRepository.delete(book);
    }

    /**
     * Busca a entidade ou lança 404. Também usada pelo LendingService.
     */
    @Transactional(readOnly = true)
    public Book findBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado."));
    }

    /**
     * Busca o livro travando a linha até o fim da transação atual (lock pessimista) ou lança 404.
     * Usada pelo LendingService para que dois empréstimos simultâneos do mesmo livro não passem
     * juntos pela verificação de disponibilidade: o segundo espera o primeiro terminar.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Book findBookForUpdate(Long id) {
        return bookRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado."));
    }

    /**
     * Converte a entidade para o DTO de resposta. Também usada pelo LendingService.
     */
    public BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                new AuthorSummary(book.getAuthor().getId(), book.getAuthor().getName()),
                new GenreSummary(book.getGenre().getId(), book.getGenre().getName()),
                book.getPublishDate(),
                book.getPublisher(),
                !lendRepository.existsByBookIdAndReturnDateIsNull(book.getId())
        );
    }

    private void applyRequest(Book book, BookRequest request) {
        book.setTitle(request.title());
        book.setAuthor(authorService.findAuthor(request.authorId()));
        book.setGenre(genreService.findGenre(request.genreId()));
        book.setPublishDate(request.publishDate());
        book.setPublisher(request.publisher());
    }
}
