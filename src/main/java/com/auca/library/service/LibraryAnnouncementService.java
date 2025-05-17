package com.auca.library.service;

import com.auca.library.dto.request.LibraryAnnouncementRequest;
import com.auca.library.dto.response.LibraryAnnouncementResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.LibraryAnnouncement;
import com.auca.library.model.User;
import com.auca.library.repository.LibraryAnnouncementRepository;
import com.auca.library.repository.UserRepository;
import com.auca.library.util.NotificationConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibraryAnnouncementService {

    @Autowired
    private LibraryAnnouncementRepository announcementRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public LibraryAnnouncementResponse createAnnouncement(LibraryAnnouncementRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LibraryAnnouncement announcement = new LibraryAnnouncement(
                request.getTitle(),
                request.getMessage(),
                request.getStartDateTime(),
                request.getEndDateTime(),
                request.isUIVisible(),
                request.isNotificationEnabled(),
                user);

        announcement = announcementRepository.save(announcement);

        // Send notifications if enabled and announcement is currently active
        if (request.isNotificationEnabled() && announcement.isActive()) {
            sendAnnouncementNotification(announcement);
        }

        return mapToResponse(announcement);
    }

    @Transactional
    public LibraryAnnouncementResponse updateAnnouncement(Long id, LibraryAnnouncementRequest request) {
        LibraryAnnouncement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));

        announcement.setTitle(request.getTitle());
        announcement.setMessage(request.getMessage());
        announcement.setStartDateTime(request.getStartDateTime());
        announcement.setEndDateTime(request.getEndDateTime());
        announcement.setUIVisible(request.isUIVisible());
        announcement.setNotificationEnabled(request.isNotificationEnabled());
        announcement.setUpdatedAt(LocalDateTime.now());

        announcement = announcementRepository.save(announcement);

        // Send notifications if enabled and announcement is currently active
        if (request.isNotificationEnabled() && announcement.isActive()) {
            sendAnnouncementNotification(announcement);
        }

        return mapToResponse(announcement);
    }

    public List<LibraryAnnouncementResponse> getAllAnnouncements() {
        return announcementRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<LibraryAnnouncementResponse> getActiveUIAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        return announcementRepository.findActiveVisibleAnnouncements(now).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public LibraryAnnouncementResponse getAnnouncementById(Long id) {
        LibraryAnnouncement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));
        return mapToResponse(announcement);
    }

    @Transactional
    public MessageResponse deleteAnnouncement(Long id) {
        if (!announcementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Announcement not found with id: " + id);
        }

        announcementRepository.deleteById(id);
        return new MessageResponse("Announcement deleted successfully");
    }

    // Process active announcements to send notifications
    @Transactional
    public void processActiveAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        List<LibraryAnnouncement> notifiableAnnouncements = announcementRepository
                .findActiveNotifiableAnnouncements(now);

        for (LibraryAnnouncement announcement : notifiableAnnouncements) {
            // Check if this is the first time this announcement is being processed
            // You might want to add a field to track if notification was already sent
            sendAnnouncementNotification(announcement);
        }
    }

    private void sendAnnouncementNotification(LibraryAnnouncement announcement) {
        // Send notification to all active users
        notificationService.sendLibraryInfoNotification(
                announcement.getTitle(),
                announcement.getMessage());
    }

    private LibraryAnnouncementResponse mapToResponse(LibraryAnnouncement announcement) {
        LibraryAnnouncementResponse response = new LibraryAnnouncementResponse();
        response.setId(announcement.getId());
        response.setTitle(announcement.getTitle());
        response.setMessage(announcement.getMessage());
        response.setStartDateTime(announcement.getStartDateTime());
        response.setEndDateTime(announcement.getEndDateTime());
        response.setUIVisible(announcement.isUIVisible());
        response.setNotificationEnabled(announcement.isNotificationEnabled());
        response.setCreatedByName(announcement.getCreatedBy().getFullName());
        response.setCreatedAt(announcement.getCreatedAt());
        response.setUpdatedAt(announcement.getUpdatedAt());
        response.setActive(announcement.isActive());
        return response;
    }
}