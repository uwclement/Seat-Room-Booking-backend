package com.auca.library.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Authentication methods
    Optional<User> findByEmail(String email);
    Optional<User> findByStudentId(String studentId);
    Optional<User> findByEmployeeId(String employeeId);
    
    // flexible login (email, studentId, or employeeId) identifiers 
    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.studentId = :identifier OR u.employeeId = :identifier")
    Optional<User> findByIdentifier(@Param("identifier") String identifier);
    
    Optional<User> findByVerificationToken(String token);
    
    Boolean existsByEmail(String email);
    Boolean existsByStudentId(String studentId);
    Boolean existsByEmployeeId(String employeeId);

    // User type queries
    @Query("SELECT u FROM User u WHERE u.studentId IS NOT NULL")
    List<User> findAllStudents();
    
    @Query("SELECT u FROM User u WHERE u.employeeId IS NOT NULL")
    List<User> findAllStaff();

    @Query("SELECT u FROM User u WHERE u.emailVerified = true")
    List<User> findActiveUsers();

    // Role-based queries
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_PROFESSOR' AND u.professorApproved = false")
    List<User> findPendingProfessors();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_PROFESSOR' AND u.professorApproved = true")
    List<User> findApprovedProfessors();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_EQUIPMENT_ADMIN'")
    Optional<User> findEquipmentAdmin();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_HOD'")
    Optional<User> findHod();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_ADMIN'")
    List<User> findAllAdmins();

    // Librarian based queries
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_LIBRARIAN'")
    List<User> findAllLibrarians();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_LIBRARIAN' AND u.workingDay = :day AND u.activeToday = true")
    List<User> findActiveLibrariansForDay(@Param("day") LocalDate day);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_LIBRARIAN' AND u.isDefault = true")
    Optional<User> findDefaultLibrarian();

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_LIBRARIAN' AND u.workingDay = :day AND u.activeToday = true")
    long countActiveLibrariansForDay(@Param("day") LocalDate day);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_PROFESSOR' " +
           "AND EXISTS (SELECT er FROM EquipmentRequest er WHERE er.user = u AND er.createdAt >= :since)")
    List<User> findActiveProfessors(@Param("since") LocalDateTime since);

    // Staff with default passwords
    @Query("SELECT u FROM User u WHERE u.employeeId IS NOT NULL AND u.mustChangePassword = true")
    List<User> findStaffWithDefaultPasswords();
}