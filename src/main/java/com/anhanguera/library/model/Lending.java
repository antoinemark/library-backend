package com.anhanguera.library.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "lendings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lending {

    private Long id;
    private Date startDate;
    private Date endDate;
    private User user;
    private Book book;
}
