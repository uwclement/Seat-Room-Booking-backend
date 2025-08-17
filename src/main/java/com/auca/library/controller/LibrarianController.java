package com.auca.library.controller;

import com.auca.library.dto.request.LibrarianRequest;
import com.auca.library.dto.response.LibrarianResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.model.User;
import com.auca.library.service.LibrarianService;
import com.auca.library.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/librarian")
public class LibrarianController {

    @Autowired
    private LibrarianService librarianService;
    
    @Autowired
    private UserService userService;

    // Get my librarian profile and schedule
    @GetMapping("/profile")
    public ResponseEntity<LibrarianResponse> getMyProfile(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        LibrarianResponse response = mapToLibrarianResponse(currentUser);
        return ResponseEntity.ok(response);
    }

    // Get other librarians at my location
    @GetMapping("/colleagues")
    public ResponseEntity<List<LibrarianResponse>> getColleagues(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        return ResponseEntity.ok(librarianService.getLibrariansByLocation(currentUser.getLocation()));
    }

    // Toggle my active status for this week
    @PutMapping("/toggle-active")
    public ResponseEntity<MessageResponse> toggleMyActiveStatus(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        return ResponseEntity.ok(librarianService.toggleLibrarianActiveStatus(currentUser.getId()));
    }

    // Update my schedule (working days only, not location)
    @PutMapping("/schedule")
    public ResponseEntity<LibrarianResponse> updateMySchedule(
            @Valid @RequestBody LibrarianRequest request,
            Authentication authentication) {
        
        User currentUser = userService.getCurrentUser(authentication);
        
        // Librarians can only update their own schedule and can't change location
        request.setUserId(currentUser.getId());
        request.setLocation(currentUser.getLocation());
        
        return ResponseEntity.ok(librarianService.updateLibrarianSchedule(request));
    }

    // Get weekly schedule for my location
    @GetMapping("/schedule/weekly")
    public ResponseEntity<List<LibrarianResponse>> getWeeklySchedule(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        return ResponseEntity.ok(librarianService.getWeeklySchedule(currentUser.getLocation()));
    }

    private LibrarianResponse mapToLibrarianResponse(User user) {
        LibrarianResponse response = new LibrarianResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setEmployeeId(user.getEmployeeId());
        response.setPhone(user.getPhone());
        response.setLocation(user.getLocation());
        response.setLocationDisplayName(user.getLocationDisplayName());
        response.setWorkingDays(user.getWorkingDays());
        response.setWorkingDaysString(user.getWorkingDaysString());
        response.setActiveThisWeek(user.isActiveThisWeek());
        response.setDefaultLibrarian(user.isDefaultLibrarian());
        // response.setIsActiveToday(user.isActiveLibrarianToday());
        return response;
    }
}