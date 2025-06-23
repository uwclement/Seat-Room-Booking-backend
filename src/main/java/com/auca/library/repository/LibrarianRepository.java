package com.auca.library.repository;

import com.auca.library.model.Librarian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LibrarianRepository extends JpaRepository<Librarian, Long> {

    List<Librarian> findByWorkingDayAndActiveToday(LocalDate workingDay, boolean activeToday);

    Optional<Librarian> findByIsDefaultTrue();

    @Query("SELECT COUNT(l) FROM Librarian l WHERE l.workingDay = :day AND l.activeToday = true")
    long countActiveLibrarians(@Param("day") LocalDate day);
}
