package com.auca.library.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auca.library.dto.request.CreateLibrarianRequest;
import com.auca.library.dto.request.LibrarianRequest;
import com.auca.library.dto.response.LibrarianResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.PublicLibrarianResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Location;
import com.auca.library.model.Role;
import com.auca.library.model.User;
import com.auca.library.repository.RoleRepository;
import com.auca.library.repository.UserRepository;

@Service
public class LibrarianService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Get all librarians (Admin view)
    public List<LibrarianResponse> getAllLibrarians() {
        List<User> librarians = userRepository.findByRole(Role.ERole.ROLE_LIBRARIAN);
        return librarians.stream()
                .map(this::mapToLibrarianResponse)
                .collect(Collectors.toList());
    }

    // Get librarians by location (Admin/Librarian view)
    public List<LibrarianResponse> getLibrariansByLocation(Location location) {
        List<User> librarians = userRepository.findLibrariansByLocation(location);
        return librarians.stream()
                .map(this::mapToLibrarianResponse)
                .collect(Collectors.toList());
    }

    // Get active librarians for today (Public view)
    // public List<PublicLibrarianResponse> getActiveLibrariansToday(Location location) {
    //     DayOfWeek today = LocalDate.now().getDayOfWeek();
    //     List<User> activeLibrarians = userRepository.findActiveLibrariansByDayAndLocation(today, location);
        
    //     if (activeLibrarians.isEmpty()) {
    //         // Return default librarian if no active librarians
    //         return userRepository.findDefaultLibrarianByLocation(location)
    //                 .map(user -> List.of(mapToPublicResponse(user)))
    //                 .orElse(List.of());
    //     }
        
    //     return activeLibrarians.stream()
    //             .map(this::mapToPublicResponse)
    //             .collect(Collectors.toList());
    // }

    // Get librarians scheduled for a specific day
    public List<LibrarianResponse> getLibrariansForDay(DayOfWeek dayOfWeek, Location location) {
        List<User> librarians = userRepository.findLibrariansByDayAndLocation(dayOfWeek, location);
        return librarians.stream()
                .map(this::mapToLibrarianResponse)
                .collect(Collectors.toList());
    }

    // Create new librarian user (Admin only)
    @Transactional
    public LibrarianResponse createLibrarian(CreateLibrarianRequest request) {
        // Validate working days capacity
        validateWorkingDaysCapacity(request.getWorkingDays(), request.getLocation(), null, request.isActiveThisWeek());
        
        // Handle default librarian logic
        if (request.isDefaultLibrarian()) {
            clearExistingDefaultLibrarian(request.getLocation());
        }

        // Create new user
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setEmployeeId(request.getEmployeeId());
        user.setPhone(request.getPhone());
        user.setLocation(request.getLocation());
        user.setPassword(passwordEncoder.encode("defaultPassword123")); // Should be changed on first login
        user.setMustChangePassword(true);
        user.setEmailVerified(true);
        
        // Set librarian role
        Role librarianRole = roleRepository.findByName(Role.ERole.ROLE_LIBRARIAN)
                .orElseThrow(() -> new RuntimeException("Librarian role not found"));
        user.getRoles().add(librarianRole);
        
        // Set librarian-specific fields
        user.setWorkingDays(request.getWorkingDays());
        user.setActiveThisWeek(request.isActiveThisWeek());
        user.setDefaultLibrarian(request.isDefaultLibrarian());

        User savedUser = userRepository.save(user);
        return mapToLibrarianResponse(savedUser);
    }

    // Update librarian schedule (Admin/Librarian)
    @Transactional
    public LibrarianResponse updateLibrarianSchedule(LibrarianRequest request) {
        User librarian = findLibrarianById(request.getUserId());
        
        // Validate working days capacity
        validateWorkingDaysCapacity(request.getWorkingDays(), request.getLocation(), 
                                   librarian.getId(), request.isActiveThisWeek());
        
        // Handle default librarian logic
        if (request.isDefaultLibrarian() && !librarian.isDefaultLibrarian()) {
            clearExistingDefaultLibrarian(request.getLocation());
        }

        // Update librarian fields
        librarian.setWorkingDays(request.getWorkingDays());
        librarian.setActiveThisWeek(request.isActiveThisWeek());
        librarian.setDefaultLibrarian(request.isDefaultLibrarian());
        librarian.setLocation(request.getLocation());

        User savedUser = userRepository.save(librarian);
        return mapToLibrarianResponse(savedUser);
    }

    // Toggle librarian active status (Librarian self-service)
    @Transactional
    public MessageResponse toggleLibrarianActiveStatus(Long librarianId) {
        User librarian = findLibrarianById(librarianId);
        librarian.setActiveThisWeek(!librarian.isActiveThisWeek());
        userRepository.save(librarian);
        
        String status = librarian.isActiveThisWeek() ? "active" : "inactive";
        return new MessageResponse("Librarian status updated to " + status);
    }

    // Get weekly schedule overview
    public List<LibrarianResponse> getWeeklySchedule(Location location) {
        List<User> librarians = userRepository.findLibrariansByLocation(location);
        return librarians.stream()
                .map(this::mapToLibrarianResponse)
                .collect(Collectors.toList());
    }

    // Private helper methods
    private void validateWorkingDaysCapacity(Set<DayOfWeek> workingDays, Location location, 
                                           Long excludeUserId, boolean isActive) {
        if (!isActive) return;
        
        for (DayOfWeek day : workingDays) {
            long activeCount = userRepository.countActiveLibrariansByDayAndLocation(day, location);
            
            // If updating existing librarian, don't count them in the limit
            if (excludeUserId != null) {
                User existingLibrarian = userRepository.findById(excludeUserId).orElse(null);
                if (existingLibrarian != null && existingLibrarian.isActiveThisWeek() && 
                    existingLibrarian.worksOnDay(day)) {
                    activeCount--;
                }
            }
            
            if (activeCount >= 2) {
                throw new IllegalStateException(
                    "Maximum 2 librarians can be active on " + day + " at " + location.getDisplayName());
            }
        }
    }

    private void clearExistingDefaultLibrarian(Location location) {
        List<User> existingDefaults = userRepository.findDefaultLibrariansByLocation(location);
        for (User existing : existingDefaults) {
            existing.setDefaultLibrarian(false);
            userRepository.save(existing);
        }
    }

    private User findLibrarianById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        if (!user.isLibrarian()) {
            throw new IllegalArgumentException("User is not a librarian");
        }
        
        return user;
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
        response.setActiveToday(user.isActiveLibrarianToday());
        return response;
    }

    private PublicLibrarianResponse mapToPublicResponse(User user) {
        PublicLibrarianResponse response = new PublicLibrarianResponse();
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setLocation(user.getLocation());
        response.setLocationDisplayName(user.getLocationDisplayName());
        response.setDefaultLibrarian(user.isDefaultLibrarian());
        return response;
    }
}