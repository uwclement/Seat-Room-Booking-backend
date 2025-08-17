package com.auca.library.repository;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.model.Location;
import com.auca.library.model.Role;
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

    List<User> findAllByEmail(String email);
    
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
     List<User> findAllEquipmentAdmins();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_HOD'")
    List<User> findAllHods();

    default Optional<User> findEquipmentAdmin() {
        List<User> admins = findAllEquipmentAdmins();
        return admins.isEmpty() ? Optional.empty() : Optional.of(admins.get(0));
    }

        default Optional<User> findAdmin() {
        List<User> admins = findAllAdmins();
        return admins.isEmpty() ? Optional.empty() : Optional.of(admins.get(0));
    }
    
    default Optional<User> findHod() {
        List<User> hods = findAllHods();
        return hods.isEmpty() ? Optional.empty() : Optional.of(hods.get(0));
    }

//     @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_HOD'")
//     Optional<User> findHod();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_ADMIN'")
    List<User> findAllAdmins();

    // Librarian based queries
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_LIBRARIAN'")
    List<User> findAllLibrarians();

    // Fixed: Use activeThisWeek instead of activeToday and proper day checking
    @Query("SELECT u FROM User u JOIN u.roles r JOIN u.workingDays wd " +
           "WHERE r.name = 'ROLE_LIBRARIAN' AND wd = :dayOfWeek AND u.activeThisWeek = true")
    List<User> findActiveLibrariansForDay(@Param("dayOfWeek") DayOfWeek dayOfWeek);

    // Fixed: Use isDefaultLibrarian instead of defaultLibrarian
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_LIBRARIAN' AND u.DefaultLibrarian = true")
    Optional<User> findDefaultLibrarian();

    // Fixed: Count active librarians for a day
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r JOIN u.workingDays wd " +
           "WHERE r.name = 'ROLE_LIBRARIAN' AND wd = :dayOfWeek AND u.activeThisWeek = true")
    long countActiveLibrariansForDay(@Param("dayOfWeek") DayOfWeek dayOfWeek);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_PROFESSOR' " +
           "AND EXISTS (SELECT er FROM EquipmentRequest er WHERE er.user = u AND er.createdAt >= :since)")
    List<User> findActiveProfessors(@Param("since") LocalDateTime since);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRole(@Param("roleName") Role.ERole roleName);
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_LIBRARIAN' AND u.location = :location")
    List<User> findLibrariansByLocation(@Param("location") Location location);
    
    @Query("SELECT u FROM User u JOIN u.roles r JOIN u.workingDays wd " +
           "WHERE r.name = 'ROLE_LIBRARIAN' AND wd = :dayOfWeek AND u.location = :location")
    List<User> findLibrariansByDayAndLocation(@Param("dayOfWeek") DayOfWeek dayOfWeek, 
                                              @Param("location") Location location);

    
    @Query("SELECT u FROM User u JOIN u.roles r " +
           "WHERE r.name = 'ROLE_LIBRARIAN' AND u.location = :location AND u.DefaultLibrarian = true")
    Optional<User> findDefaultLibrarianByLocation(@Param("location") Location location);
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r JOIN u.workingDays wd " +
           "WHERE r.name = 'ROLE_LIBRARIAN' AND wd = :dayOfWeek AND u.location = :location " +
           "AND u.activeThisWeek = true")
    long countActiveLibrariansByDayAndLocation(@Param("dayOfWeek") DayOfWeek dayOfWeek, 
                                              @Param("location") Location location);
    
    @Query("SELECT u FROM User u JOIN u.roles r " +
           "WHERE r.name = 'ROLE_LIBRARIAN' AND u.location = :location AND u.DefaultLibrarian = true")
    List<User> findDefaultLibrariansByLocation(@Param("location") Location location);

    // Staff with default passwords
    @Query("SELECT u FROM User u WHERE u.employeeId IS NOT NULL AND u.mustChangePassword = true")
    List<User> findStaffWithDefaultPasswords();


    // Add to UserRepository
    @Query("SELECT u.email, COUNT(u) FROM User u GROUP BY u.email HAVING COUNT(u) > 1")
    List<Object[]> findDuplicateEmails();

//     @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_PROFESSOR' " +
//        "AND u.professorApproved = true AND SIZE(u.pendingCourses) > 0")
//      List<User> findProfessorsWithPendingCourses();


     @Query("SELECT u FROM User u JOIN u.roles r JOIN u.workingDays wd " +
       "WHERE r.name = 'ROLE_LIBRARIAN' AND wd = :dayOfWeek AND u.location = :location " +
       "AND u.activeThisWeek = true")
     List<User> findActiveLibrariansForDay(@Param("dayOfWeek") DayOfWeek dayOfWeek, 
                                      @Param("location") Location location);


     @Query("SELECT COUNT(u) FROM User u WHERE u.location = :location")
    int countByLocation(@Param("location") Location location);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.location = :location AND " +
           "u.createdAt >= :startDate AND u.createdAt <= :endDate")
    int countByLocationAndCreatedAtBetween(@Param("location") Location location,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate AND u.createdAt <= :endDate")
    int countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.location = :location AND u.createdAt <= :cutoffDate")
    int countByLocationAndCreatedAtBefore(@Param("location") Location location,
                                         @Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt <= :cutoffDate")
    int countByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // User type counts by location
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE u.location = :location AND " +
           "r.name = 'ROLE_USER' ")
    int countStudentsByLocation(@Param("location") Location location);
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE u.location = :location AND " +
           "r.name = 'ROLE_PROFESSOR'")
    int countProfessorsByLocation(@Param("location") Location location);
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE u.location = :location AND " +
           "r.name = 'ROLE_LIBRARIAN'")
    int countLibrariansByLocation(@Param("location") Location location);
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE u.location = :location AND " +
           "r.name = 'ROLE_ADMIN'")
    int countAdminsByLocation(@Param("location") Location location);
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE u.location = :location AND " +
           "r.name = 'ROLE_EQUIPMENT_ADMIN'")
    int countEquipmentAdminsByLocation(@Param("location") Location location);
    
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE u.location = :location AND r.name != 'ROLE_USER'")
    int countStaffByLocation(@Param("location") Location location);
    
    // Global user type counts
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_USER' ")
    int countAllStudents();
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_PROFESSOR'")
    int countAllProfessors();
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_LIBRARIAN'")
    int countAllLibrarians();
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_ADMIN'")
    int countAllAdmins();
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_EQUIPMENT_ADMIN'")
    int countAllEquipmentAdmins();
    
   @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.name IN ('ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_LIBRARIAN')")
   int countAllStaff();
    
    // User lists by location
   @Query("SELECT u FROM User u JOIN u.roles r WHERE u.location = :location AND r.name != 'ROLE_USER'")
   List<User> findStaffByLocation(@Param("location") Location location);
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE u.location = :location AND r.name = 'ROLE_PROFESSOR'")
    List<User> findProfessorsByLocation(@Param("location") Location location);
                                     
}