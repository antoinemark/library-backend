package com.anhanguera.library.dtos.LendingDTO;

import com.anhanguera.library.model.Book;
import com.anhanguera.library.model.User;

import java.util.Date;

public record LendingResponse(
        Long id,
        Date startDate,
        Date endDate,
        User user,
        Book book
) {}
