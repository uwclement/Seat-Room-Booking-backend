package com.auca.library.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.response.LibraryScheduleResponse;
import com.auca.library.dto.response.LibraryStatusResponse;
import com.auca.library.model.Location;
import com.auca.library.model.User;
import com.auca.library.service.LibraryScheduleService;
import com.auca.library.service.UserService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/library")
public class PublicLibraryController {

    @Autowired
    private LibraryScheduleService libraryScheduleService;
    
    @Autowired
    private UserService userService;


    // Get current library status (open/closed)
    // @GetMapping("/status")
    // public ResponseEntity<LibraryStatusResponse> getCurrentStatus(Authentication auth) {
    //     User currentUser = getCurrentUser(auth);
    //     return ResponseEntity.ok(libraryScheduleService.getCurrentLibraryStatus(currentUser.getLocation()));
    // }

    // Get current week's schedule
    @GetMapping("/schedule")
    public ResponseEntity<List<LibraryScheduleResponse>> getCurrentSchedule() {
        return ResponseEntity.ok(libraryScheduleService.getAllLibrarySchedules());
    }

     private User getCurrentUser(Authentication auth) {
        return userService.findByEmail(auth.getName()).orElseThrow();
    }

     @GetMapping("/status/{location}")
    public ResponseEntity<LibraryStatusResponse> getLibraryStatusByLocation(@PathVariable Location location) {
        return ResponseEntity.ok(libraryScheduleService.getCurrentLibraryStatus(location));
    }

     @GetMapping("/status")
    public ResponseEntity<LibraryStatusResponse> getCurrentLibraryStatus(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            // For unauthenticated users, you might want to default to one location
            // or return an error. For now, defaulting to GISHUSHU
            return ResponseEntity.ok(libraryScheduleService.getCurrentLibraryStatus(Location.GISHUSHU));
        }
        
        User currentUser = getCurrentUser(auth);
        return ResponseEntity.ok(libraryScheduleService.getCurrentLibraryStatus(currentUser.getLocation()));
    }
}