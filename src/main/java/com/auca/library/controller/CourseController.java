package com.auca.library.controller;

import com.auca.library.dto.request.CourseRequest;
import com.auca.library.dto.response.CourseResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/equipment-admin/courses")
@PreAuthorize("hasRole('EQUIPMENT_ADMIN')")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAllCourses() {
        List<CourseResponse> courses = courseService.getAllCourses();
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/active")
    public ResponseEntity<List<CourseResponse>> getActiveCourses() {
        List<CourseResponse> courses = courseService.getActiveCourses();
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourseById(@PathVariable Long id) {
        CourseResponse course = courseService.getCourseById(id);
        return ResponseEntity.ok(course);
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseRequest request) {
        CourseResponse course = courseService.createCourse(request);
        return ResponseEntity.ok(course);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        CourseResponse course = courseService.updateCourse(id, request);
        return ResponseEntity.ok(course);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteCourse(@PathVariable Long id) {
        MessageResponse response = courseService.deleteCourse(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<CourseResponse> toggleCourseStatus(@PathVariable Long id) {
        CourseResponse course = courseService.toggleCourseStatus(id);
        return ResponseEntity.ok(course);
    }

    @GetMapping("/search")
    public ResponseEntity<List<CourseResponse>> searchCourses(@RequestParam String keyword) {
        List<CourseResponse> courses = courseService.searchCourses(keyword);
        return ResponseEntity.ok(courses);
    }
}