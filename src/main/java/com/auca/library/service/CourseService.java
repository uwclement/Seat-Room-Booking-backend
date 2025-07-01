package com.auca.library.service;

import com.auca.library.dto.request.CourseRequest;
import com.auca.library.dto.response.CourseResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Course;
import com.auca.library.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CourseResponse> getActiveCourses() {
        return courseRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public CourseResponse getCourseById(Long id) {
        Course course = findCourseById(id);
        return mapToResponse(course);
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        if (courseRepository.existsByCourseCode(request.getCourseCode())) {
            throw new IllegalArgumentException("Course with code '" + request.getCourseCode() + "' already exists");
        }

        Course course = new Course(request.getCourseCode(), request.getCourseName(), request.getCreditHours());
        course = courseRepository.save(course);
        return mapToResponse(course);
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        Course course = findCourseById(id);

        // Check if course code is being changed and conflicts
        if (!course.getCourseCode().equals(request.getCourseCode()) && 
            courseRepository.existsByCourseCode(request.getCourseCode())) {
            throw new IllegalArgumentException("Course with code '" + request.getCourseCode() + "' already exists");
        }

        course.setCourseCode(request.getCourseCode());
        course.setCourseName(request.getCourseName());
        course.setCreditHours(request.getCreditHours());

        course = courseRepository.save(course);
        return mapToResponse(course);
    }

    @Transactional
    public MessageResponse deleteCourse(Long id) {
        Course course = findCourseById(id);
        courseRepository.delete(course);
        return new MessageResponse("Course deleted successfully");
    }

    @Transactional
    public CourseResponse toggleCourseStatus(Long id) {
        Course course = findCourseById(id);
        course.setActive(!course.isActive());
        course = courseRepository.save(course);
        return mapToResponse(course);
    }

    public List<CourseResponse> searchCourses(String keyword) {
        return courseRepository.searchCourses(keyword).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Course findCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    private CourseResponse mapToResponse(Course course) {
        CourseResponse response = new CourseResponse();
        response.setId(course.getId());
        response.setCourseCode(course.getCourseCode());
        response.setCourseName(course.getCourseName());
        response.setCreditHours(course.getCreditHours());
        response.setActive(course.isActive());
        response.setProfessorCount(course.getProfessors().size());
        return response;
    }
}