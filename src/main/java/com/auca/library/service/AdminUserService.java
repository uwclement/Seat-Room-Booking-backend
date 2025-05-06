package com.auca.library.service;

import com.auca.library.dto.response.UserResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Booking.BookingStatus;
import com.auca.library.model.User;
import com.auca.library.repository.BookingRepository;
import com.auca.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BookingRepository bookingRepository;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapUserToResponse)
                .collect(Collectors.toList());
    }
    
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        return mapUserToResponse(user);
    }
    
    @Transactional
    public UserResponse setUserEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        user.setEmailVerified(enabled); // Assuming emailVerified is used for enabling/disabling account
        user = userRepository.save(user);
        
        return mapUserToResponse(user);
    }
    
    public List<UserResponse> getUsersWithActiveBookings() {
        LocalDateTime now = LocalDateTime.now();
        Set<Long> userIdsWithActiveBookings = bookingRepository
                .findByEndTimeAfterAndStartTimeBeforeAndStatusIn(
                        now, now, List.of(BookingStatus.RESERVED, BookingStatus.CHECKED_IN))
                .stream()
                .map(booking -> booking.getUser().getId())
                .collect(Collectors.toSet());
        
        List<User> usersWithActiveBookings = userRepository.findAllById(userIdsWithActiveBookings);
        
        return usersWithActiveBookings.stream()
                .map(this::mapUserToResponse)
                .collect(Collectors.toList());
    }
    
    private UserResponse mapUserToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setStudentId(user.getStudentId());
        response.setEmailVerified(user.isEmailVerified());
        
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
        
        response.setRoles(roles);
        
        return response;
    }
}