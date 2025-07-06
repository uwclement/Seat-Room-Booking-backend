package com.auca.library.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auca.library.dto.request.ProfessorCourseRequest;
import com.auca.library.dto.response.CourseResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.ProfessorResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Course;
import com.auca.library.model.User;
import com.auca.library.repository.CourseRepository;
import com.auca.library.repository.UserRepository;

@Service
public class ProfessorService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private NotificationService notificationService;

    // Request course approval (Professor)
    @Transactional
    public MessageResponse requestCourseApproval(ProfessorCourseRequest request, String professorEmail) {
        User professor = findUserByEmail(professorEmail);
        
        // Validate professor status
        if (!professor.isProfessorApproved()) {
            throw new IllegalStateException("Professor account must be approved first");
        }
        
        Set<Course> requestedCourses = request.getCourseIds().stream()
                .map(this::findCourseById)
                .collect(Collectors.toSet());
        
        // Add courses to professor's pending list (for HOD approval)
        professor.getApprovedCourses().addAll(requestedCourses);
        userRepository.save(professor);
        
        // Notify HOD
        String courseNames = requestedCourses.stream()
                .map(Course::getCourseName)
                .collect(Collectors.joining(", "));
        
        String hodEmail = getHodEmail();
        if (hodEmail != null) {
            notificationService.addNotification(
                hodEmail,
                "Professor Course Approval Request",
                String.format("Professor %s has requested approval for courses: %s", 
                    professor.getFullName(), courseNames),
                "PROFESSOR_COURSE_REQUEST"
            );
        } else {
            // Log warning that no HOD is available
            System.out.println("Warning: Cannot notify HOD - no HOD user found in system");
        }
        
        return new MessageResponse("Course approval request submitted successfully");
    }

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
            "Your professor account has been approved. You can now select courses and make equipment requests.",
            "PROFESSOR_APPROVED"
        );
        
        return new MessageResponse("Professor account approved successfully");
    }

    // Approve professor-course association (HOD)
    @Transactional
    public MessageResponse approveProfessorCourses(Long professorId, List<Long> courseIds, String hodEmail) {
        User professor = findUserById(professorId);
        User hod = findUserByEmail(hodEmail);
        
        Set<Course> approvedCourses = courseIds.stream()
                .map(this::findCourseById)
                .collect(Collectors.toSet());
        
        professor.getApprovedCourses().clear();
        professor.getApprovedCourses().addAll(approvedCourses);
        userRepository.save(professor);
        
        String courseNames = approvedCourses.stream()
                .map(Course::getCourseName)
                .collect(Collectors.joining(", "));
        
        notificationService.addNotification(
            professor.getEmail(),
            "Courses Approved",
            String.format("Your courses have been approved: %s", courseNames),
            "PROFESSOR_COURSES_APPROVED"
        );
        
        return new MessageResponse("Professor courses approved successfully");
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private Course findCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    private String getHodEmail() {
        return userRepository.findHod()
                .map(User::getEmail)
                .orElse(null); // Return null if no HOD found
    }

    private ProfessorResponse mapToProfessorResponse(User professor) {
        ProfessorResponse response = new ProfessorResponse();
        response.setId(professor.getId());
        response.setFullName(professor.getFullName());
        response.setEmail(professor.getEmail());
        response.setProfessorApproved(professor.isProfessorApproved());
        response.setProfessorApprovedAt(professor.getProfessorApprovedAt());
        
        response.setCourseIds(professor.getApprovedCourses().stream()
                .map(Course::getId)
                .collect(Collectors.toList()));
        response.setCourseNames(professor.getApprovedCourses().stream()
                .map(Course::getCourseName)
                .collect(Collectors.toList()));
        
        return response;
    }
}