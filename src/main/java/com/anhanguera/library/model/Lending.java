package com.anhanguera.library.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "lendings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lending {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Data em que o livro foi emprestado
    @Column(nullable = false)
    private LocalDate startDate;

    // Data prevista para a devolução
    @Column(nullable = false)
    private LocalDate endDate;

    // Data efetiva da devolução (nula enquanto o empréstimo estiver em aberto)
    @Column
    private LocalDate returnDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
}
