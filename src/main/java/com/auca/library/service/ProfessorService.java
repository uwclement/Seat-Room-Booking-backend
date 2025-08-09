package com.auca.library.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auca.library.dto.response.CourseResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.ProfessorResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Course;
import com.auca.library.model.User;
import com.auca.library.repository.CourseRepository;
import com.auca.library.repository.UserRepository;
import com.auca.library.util.NotificationConstants;

@Service
public class ProfessorService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private NotificationService notificationService;

    // Get professor's approved courses
    public List<CourseResponse> getProfessorApprovedCourses(String professorEmail) {
    User professor = findUserByEmail(professorEmail);
    return professor.getApprovedCourses().stream()
            .filter(Course::isActive)
            .map(course -> {
                CourseResponse response = new CourseResponse();
                response.setId(course.getId());
                response.setCourseCode(course.getCourseCode());
                response.setCourseName(course.getCourseName());
                response.setCreditHours(course.getCreditHours());
                response.setActive(course.isActive());
                response.setProfessorCount(course.getProfessors().size());
                return response;
            })
            .collect(Collectors.toList());
}

    // Get pending professor approvals (HOD)
    public List<ProfessorResponse> getPendingProfessorApprovals() {
        List<User> pendingProfessors = userRepository.findPendingProfessors();
        return pendingProfessors.stream()
                .map(this::mapToProfessorResponse)
                .collect(Collectors.toList());
    }

    // Approve professor account (HOD)
    @Transactional
    public MessageResponse approveProfessorAccount(Long professorId, String hodEmail) {
        User professor = findUserById(professorId);
        User hod = findUserByEmail(hodEmail);
        
        professor.setProfessorApproved(true);
        professor.setProfessorApprovedAt(LocalDateTime.now());
        professor.setApprovedByHod(hod);
        
        userRepository.save(professor);
        
        notificationService.addNotification(
            professor.getEmail(),
            "Professor Account Approved",
            "Your professor account has been approved. You can now access your assigned courses and make equipment requests.",
            NotificationConstants.TYPE_LIBRARY_INFO
);
        
        return new MessageResponse("Professor account approved successfully");
    }



@Transactional
public MessageResponse rejectProfessorAccount(Long professorId, String rejectionReason, String hodEmail) {
    User professor = findUserById(professorId);
    User hod = findUserByEmail(hodEmail);
    Optional<User> admin = userRepository.findEquipmentAdmin();
    
    // Keep professor unapproved
    professor.setProfessorApproved(false);
    professor.setRejectionReason(rejectionReason);
    professor.setRejectedAt(LocalDateTime.now());
    professor.setRejectedByHod(hod);
    
    userRepository.save(professor);
    
    // Notify admin
    notificationService.addNotification(
        getAdminEmail(), // get equipment admin email
        "Professor Account Rejected",
        String.format("Professor %s account rejected by HOD. Reason: %s", 
            professor.getFullName(), rejectionReason),
        "PROFESSOR_REJECTED"
    );
    
    // Notify professor
        notificationService.addNotification(
            getAdminEmail(),
             "Professor Account Rejected",
             String.format("Professor %s account rejected by HOD. Reason: %s", 
                professor.getFullName(), rejectionReason),
            NotificationConstants.TYPE_LIBRARY_INFO
);
    
    return new MessageResponse("Professor account rejected successfully");
}

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    // private Course findCourseById(Long id) {
    //     return courseRepository.findById(id)
    //             .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    // }

    // private String getHodEmail() {
    //     return userRepository.findHod()
    //             .map(User::getEmail)
    //             .orElse(null); 
    // }

    private String getAdminEmail() {
    try {
        return userRepository.findAdmin()
                .map(User::getEmail)
                .orElse(null);
    } catch (IncorrectResultSizeDataAccessException e) {
        // Fallback: get first equipment admin
        List<User> admins = userRepository.findAllAdmins();
        return admins.isEmpty() ? null : admins.get(0).getEmail();
    }
}

    private ProfessorResponse mapToProfessorResponse(User professor) {
    ProfessorResponse response = new ProfessorResponse();
    response.setId(professor.getId());
    response.setFullName(professor.getFullName());
    response.setEmail(professor.getEmail());
    response.setEmployeeId(professor.getEmployeeId());
    response.setProfessorApproved(professor.isProfessorApproved());
    response.setProfessorApprovedAt(professor.getProfessorApprovedAt());
    
    
    response.setAssignedCourses(professor.getApprovedCourses().stream()
            .map(course -> {
                CourseResponse courseResponse = new CourseResponse();
                courseResponse.setId(course.getId());
                courseResponse.setCourseCode(course.getCourseCode());
                courseResponse.setCourseName(course.getCourseName());
                courseResponse.setCreditHours(course.getCreditHours());
                courseResponse.setActive(course.isActive());
                return courseResponse;
            })
            .collect(Collectors.toList()));
    
    // Keep the old format for backward compatibility if needed
    response.setCourseIds(professor.getApprovedCourses().stream()
            .map(Course::getId)
            .collect(Collectors.toList()));
    response.setCourseNames(professor.getApprovedCourses().stream()
            .map(Course::getCourseName)
            .collect(Collectors.toList()));
    
    return response;
}
}