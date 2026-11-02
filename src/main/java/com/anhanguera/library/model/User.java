package com.anhanguera.library.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // CPF armazenado somente com dígitos (11 caracteres)
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;
}
