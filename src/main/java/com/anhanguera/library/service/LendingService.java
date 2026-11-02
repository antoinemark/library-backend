package com.anhanguera.library.service;

import com.anhanguera.library.dtos.LendingDTO.LendingRequest;
import com.anhanguera.library.dtos.LendingDTO.LendingResponse;
import com.anhanguera.library.dtos.LendingDTO.LendingReturnRequest;
import com.anhanguera.library.exception.BusinessRuleException;
import com.anhanguera.library.exception.FieldValidationException;
import com.anhanguera.library.exception.ResourceNotFoundException;
import com.anhanguera.library.model.Book;
import com.anhanguera.library.model.Lending;
import com.anhanguera.library.model.LendingStatus;
import com.anhanguera.library.model.User;
import com.anhanguera.library.repository.LendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LendingService {

    private final LendRepository lendRepository;
    private final UserService userService;
    private final BookService bookService;
    private final Clock clock;

    /**
     * Lista os empréstimos (mais recentes primeiro), com filtros opcionais por usuário e por status.
     */
    @Transactional(readOnly = true)
    public List<LendingResponse> getLendings(Long userId, LendingStatus status) {
        List<Lending> lendings;
        if (userId != null) {
            userService.findUser(userId);
            lendings = lendRepository.findByUserIdOrderByStartDateDescIdDesc(userId);
        } else {
            lendings = lendRepository.findAllByOrderByStartDateDescIdDesc();
        }

        LocalDate today = today();
        return lendings.stream()
                .filter(lending -> status == null || LendingStatus.of(lending, today) == status)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LendingResponse getLendingById(Long id) {
        return toResponse(findLending(id));
    }

    /**
     * Registra um empréstimo no ato ou de forma retroativa (startDate até hoje), nunca no futuro:
     * um empréstimo futuro deixaria o livro indisponível sem poder ser devolvido antes da data.
     */
    @Transactional
    public LendingResponse createLending(LendingRequest request) {
        LocalDate today = today();
        if (request.endDate().isBefore(request.startDate())) {
            throw new FieldValidationException("endDate",
                    "A data prevista de devolução não pode ser anterior à data do empréstimo.");
        }
        if (request.startDate().isAfter(today)) {
            throw new FieldValidationException("startDate", "A data do empréstimo não pode ser futura.");
        }
        // Um empréstimo não pode nascer atrasado (o usuário ficaria com pendência na hora)
        if (request.endDate().isBefore(today)) {
            throw new FieldValidationException("endDate",
                    "A data prevista de devolução não pode ser anterior a hoje.");
        }

        User user = userService.findUser(request.userId());
        // Trava o livro até o fim da transação: pedidos simultâneos do mesmo livro são atendidos
        // um de cada vez, e o segundo já encontra o empréstimo do primeiro (409)
        Book book = bookService.findBookForUpdate(request.bookId());

        if (userService.hasOverdueLendings(user.getId())) {
            throw new BusinessRuleException("O usuário possui empréstimos em atraso e não pode realizar novos empréstimos.");
        }
        if (lendRepository.existsByBookIdAndReturnDateIsNull(book.getId())) {
            throw new BusinessRuleException("Este livro já está emprestado.");
        }
        if (request.startDate().isBefore(book.getPublishDate())) {
            throw new FieldValidationException("startDate",
                    "A data do empréstimo não pode ser anterior à data de publicação do livro.");
        }
        // O histórico do livro não pode ter dois empréstimos ao mesmo tempo: o novo empréstimo
        // começa no dia da última devolução ou depois
        if (lendRepository.existsByBookIdAndReturnDateAfter(book.getId(), request.startDate())) {
            throw new FieldValidationException("startDate",
                    "A data do empréstimo não pode ser anterior à última devolução deste livro.");
        }

        Lending lending = new Lending();
        lending.setUser(user);
        lending.setBook(book);
        lending.setStartDate(request.startDate());
        lending.setEndDate(request.endDate());
        return toResponse(lendRepository.save(lending));
    }

    /**
     * Registra a devolução do livro. Sem data informada, considera a data de hoje.
     */
    @Transactional
    public LendingResponse returnLending(Long id, LendingReturnRequest request) {
        Lending lending = findLending(id);
        if (lending.getReturnDate() != null) {
            throw new BusinessRuleException("Este empréstimo já foi devolvido.");
        }

        LocalDate today = today();
        LocalDate returnDate = request != null && request.returnDate() != null ? request.returnDate() : today;
        if (returnDate.isBefore(lending.getStartDate())) {
            throw new FieldValidationException("returnDate",
                    "A data de devolução não pode ser anterior à data do empréstimo.");
        }
        if (returnDate.isAfter(today)) {
            throw new FieldValidationException("returnDate", "A data de devolução não pode ser futura.");
        }

        lending.setReturnDate(returnDate);
        return toResponse(lendRepository.save(lending));
    }

    private Lending findLending(Long id) {
        return lendRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo não encontrado."));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private LendingResponse toResponse(Lending lending) {
        return new LendingResponse(
                lending.getId(),
                lending.getStartDate(),
                lending.getEndDate(),
                lending.getReturnDate(),
                LendingStatus.of(lending, today()),
                userService.toResponse(lending.getUser()),
                bookService.toResponse(lending.getBook())
        );
    }
}
