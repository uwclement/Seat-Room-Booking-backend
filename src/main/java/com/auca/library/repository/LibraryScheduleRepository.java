package com.auca.library.repository;

import com.auca.library.model.LibrarySchedule;
import com.auca.library.model.Location;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface LibraryScheduleRepository extends JpaRepository<LibrarySchedule, Long> {
    Optional<LibrarySchedule> findByDayOfWeek(DayOfWeek dayOfWeek);

     Optional<LibrarySchedule> findByDayOfWeekAndLocation(DayOfWeek dayOfWeek, Location location);
    
    List<LibrarySchedule> findByLocation(Location location);
    
    List<LibrarySchedule> findByLocationOrderByDayOfWeek(Location location);
}