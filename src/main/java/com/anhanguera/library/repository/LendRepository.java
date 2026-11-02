package com.anhanguera.library.repository;

import com.anhanguera.library.model.Lending;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LendRepository extends JpaRepository<Lending, Long> {

    List<Lending> findAllByOrderByStartDateDescIdDesc();

    List<Lending> findByUserIdOrderByStartDateDescIdDesc(Long userId);

    // Livro emprestado = possui empréstimo ainda não devolvido
    boolean existsByBookIdAndReturnDateIsNull(Long bookId);

    // Livro devolvido depois da data informada (um empréstimo iniciado nessa data se sobreporia a ele)
    boolean existsByBookIdAndReturnDateAfter(Long bookId, LocalDate date);

    // Usuário com pendência = possui empréstimo em aberto com data prevista anterior a hoje
    boolean existsByUserIdAndReturnDateIsNullAndEndDateBefore(Long userId, LocalDate date);

    boolean existsByUserId(Long userId);

    boolean existsByBookId(Long bookId);
}
