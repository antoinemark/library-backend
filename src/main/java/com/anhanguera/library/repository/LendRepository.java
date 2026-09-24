package com.anhanguera.library.repository;

import com.anhanguera.library.model.Lending;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LendRepository extends JpaRepository<Lending, Long> {
}
