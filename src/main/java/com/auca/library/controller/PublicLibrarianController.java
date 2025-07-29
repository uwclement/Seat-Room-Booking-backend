package com.auca.library.controller;

import com.auca.library.dto.response.PublicLibrarianResponse;
import com.auca.library.model.Location;
import com.auca.library.service.LibrarianService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/public/librarians")
public class PublicLibrarianController {

    @Autowired
    private LibrarianService librarianService;

    // Get active librarians for today at specific location
    @GetMapping("/active/{location}")
    public ResponseEntity<List<PublicLibrarianResponse>> getActiveLibrariansToday(
            @PathVariable Location location) {
        return ResponseEntity.ok(librarianService.getActiveLibrariansToday(location));
    }

    // Get active librarians for today (all locations)
    @GetMapping("/active")
    public ResponseEntity<List<PublicLibrarianResponse>> getActiveLibrariansAllLocations() {
        List<PublicLibrarianResponse> allActiveLibrarians = new ArrayList<>();
        
        // Get active librarians from all locations
        for (Location location : Location.values()) {
            List<PublicLibrarianResponse> locationLibrarians = 
                librarianService.getActiveLibrariansToday(location);
            allActiveLibrarians.addAll(locationLibrarians);
        }
        
        return ResponseEntity.ok(allActiveLibrarians);
    }

    // Get available locations
    @GetMapping("/locations")
    public ResponseEntity<List<LocationInfo>> getAvailableLocations() {
        List<LocationInfo> locations = Arrays.stream(Location.values())
                .map(loc -> new LocationInfo(loc, loc.getDisplayName(), loc.getCode()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(locations);
    }

    // Inner class for location information
    public static class LocationInfo {
        private Location location;
        private String displayName;
        private String code;

        public LocationInfo(Location location, String displayName, String code) {
            this.location = location;
            this.displayName = displayName;
            this.code = code;
        }

        // Getters and setters
        public Location getLocation() { return location; }
        public void setLocation(Location location) { this.location = location; }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}