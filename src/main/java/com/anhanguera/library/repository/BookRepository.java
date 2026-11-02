package com.anhanguera.library.repository;

import com.anhanguera.library.model.Book;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // SELECT ... FOR UPDATE: a linha do livro fica travada até o fim da transação
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);

    boolean existsByAuthorId(Long authorId);

    long countByAuthorId(Long authorId);

    boolean existsByGenreId(Long genreId);

    long countByGenreId(Long genreId);
}
