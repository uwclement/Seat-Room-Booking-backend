package com.auca.library.repository;

import com.auca.library.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    
    Optional<Course> findByCourseCode(String courseCode);
    
    boolean existsByCourseCode(String courseCode);
    
    List<Course> findByActiveTrue();
    
    @Query("SELECT c FROM Course c WHERE LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Course> searchCourses(@Param("keyword") String keyword);
    
    @Query("SELECT c FROM Course c JOIN c.professors p WHERE p.id = :professorId")
    List<Course> findByProfessorId(@Param("professorId") Long professorId);
}