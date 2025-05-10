package com.auca.library.repository;

import com.auca.library.model.LibrarySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.Optional;

@Repository
public interface LibraryScheduleRepository extends JpaRepository<LibrarySchedule, Long> {
    Optional<LibrarySchedule> findByDayOfWeek(DayOfWeek dayOfWeek);
}