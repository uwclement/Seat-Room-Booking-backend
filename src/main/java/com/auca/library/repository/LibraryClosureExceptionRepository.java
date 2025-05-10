package com.auca.library.repository;

import com.auca.library.model.LibraryClosureException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LibraryClosureExceptionRepository extends JpaRepository<LibraryClosureException, Long> {
    Optional<LibraryClosureException> findByDate(LocalDate date);
    
    List<LibraryClosureException> findByDateBetween(LocalDate start, LocalDate end);
}