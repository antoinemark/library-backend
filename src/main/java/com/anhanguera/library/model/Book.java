package com.anhanguera.library.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    private Long id;
    private String title;
    private Author author;
    private Genre genre;
    private Date publishDate;
    private String publisher;
}
