package com.auca.library.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.auca.library.dto.request.BulkSeatCreationRequest;
import com.auca.library.dto.request.BulkSeatUpdateRequest;
import com.auca.library.dto.request.SeatAvailabilityRequest;
import com.auca.library.dto.response.SeatDTO;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Booking;
import com.auca.library.model.Location;
import com.auca.library.model.Seat;
import com.auca.library.model.User;
import com.auca.library.repository.BookingRepository;
import com.auca.library.repository.QRCodeLogRepository;
import com.auca.library.repository.SeatRepository;
import com.auca.library.repository.UserRepository;
import com.auca.library.repository.WaitListRepository;

import jakarta.transaction.Transactional;

@Service
public class SeatService {

    @Autowired
    private SeatRepository seatRepository;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private WaitListRepository waitListRepository;

    @Autowired
    private QRCodeGenerationService qrGenerationService;

    @Autowired
    private QRCodeStorageService qrStorageService;

    @Autowired
    private QRCodeLogRepository qrCodeLogRepository;

    
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    public List<SeatDTO> getAllSeats() {
        LocalDateTime now = LocalDateTime.now();
        List<Seat> seats = seatRepository.findByIsDisabled(false);
        return mapSeatsToSeatDTOs(seats, now, now.plusHours(1));
    }

    public List<SeatDTO> getSeatsInMasoro() {
        LocalDateTime now = LocalDateTime.now();
        List<Seat> seats = seatRepository.findByIsDisabledFalseAndLocation(Location.MASORO);
        return mapSeatsToSeatDTOs(seats, now, now.plusHours(1));
    }


    public List<SeatDTO> getSeatsInGishushu() {
        LocalDateTime now = LocalDateTime.now();
        List<Seat> seats = seatRepository.findByIsDisabledFalseAndLocation(Location.GISHUSHU);
        return mapSeatsToSeatDTOs(seats, now, now.plusHours(1));
    }


    
    public SeatDTO getSeatById(Long id) {
        LocalDateTime now = LocalDateTime.now();
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));
        
        if (seat.isDisabled()) {
            throw new ResourceNotFoundException("Seat is currently unavailable for booking");
        }
        
        return mapSeatToDTO(seat, now, now.plusHours(1));
    }
    
    public List<SeatDTO> getAvailableSeats(SeatAvailabilityRequest request) {
        List<Seat> seats;
        
        // if (request.getIsAvailabe() )
        // Filter by zone and desktop availability if provided
        if (request.getZoneType() != null && request.getHasDesktop() != null) {
            seats = seatRepository.findByZoneTypeAndHasDesktop(request.getZoneType(), request.getHasDesktop());
        } else if (request.getZoneType() != null) {
            seats = seatRepository.findByZoneType(request.getZoneType());
        } else if (request.getHasDesktop() != null) {
            seats = seatRepository.findByHasDesktop(request.getHasDesktop());
        } else {
            seats = seatRepository.findAll();
        }
        
        // Filter out disabled seats
        seats = seats.stream()
                .filter(seat -> !seat.isDisabled())
                .collect(Collectors.toList());
        
        return mapSeatsToSeatDTOs(seats, request.getStartTime(), request.getEndTime());
    }
    
    public boolean toggleFavoriteSeat(Long seatId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));
        
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + seatId));
        
        boolean isFavorite = user.getFavoriteSeats().contains(seat);
        
        if (isFavorite) {
            user.getFavoriteSeats().remove(seat);
        } else {
            user.getFavoriteSeats().add(seat);
        }
        
        userRepository.save(user);
        
        return !isFavorite;
    }
    
    public List<SeatDTO> getFavoriteSeats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));
        
        LocalDateTime now = LocalDateTime.now();
        
        return user.getFavoriteSeats().stream()
                .filter(seat -> !seat.isDisabled())
                .map(seat -> mapSeatToDTO(seat, now, now.plusHours(1)))
                .collect(Collectors.toList());
    }
    
    private List<SeatDTO> mapSeatsToSeatDTOs(List<Seat> seats, LocalDateTime startTime, LocalDateTime endTime) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));
        
        return seats.stream()
                .map(seat -> mapSeatToDTO(seat, startTime, endTime))
                .collect(Collectors.toList());
    }
    
    private SeatDTO mapSeatToDTO(Seat seat, LocalDateTime startTime, LocalDateTime endTime) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));
        
        boolean isAvailable = isSeatAvailable(seat.getId(), startTime, endTime);
        boolean isFavorite = currentUser.getFavoriteSeats().contains(seat);
        
        // Get next available time if seat is booked
        String nextAvailableTime = "";
        if (!isAvailable) {
            nextAvailableTime = getNextAvailableTime(seat.getId(), startTime);
        }
        
        // Count waiting list entries
        int waitingCount = waitListRepository.countWaitingForSeat(seat.getId());
        
        SeatDTO dto = new SeatDTO();
        dto.setId(seat.getId());
        dto.setSeatNumber(seat.getSeatNumber());
        dto.setZoneType(seat.getZoneType());
        dto.setHasDesktop(seat.isHasDesktop());
        dto.setDescription(seat.getDescription());
        dto.setAvailable(isAvailable);
        dto.setFavorite(isFavorite);
        dto.setNextAvailableTime(nextAvailableTime);
        dto.setWaitingCount(waitingCount);
        dto.setLocation(seat.getLocation().name());
        dto.setFloar(seat.getFloar());
        
        return dto;
    }
    
    public boolean isSeatAvailable(Long seatId, LocalDateTime startTime, LocalDateTime endTime) {
        // Check if there are any overlapping bookings
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(
                seatId, startTime, endTime);
        
        return overlappingBookings.isEmpty();
    }
    
    private String getNextAvailableTime(Long seatId, LocalDateTime startTime) {
        // Find bookings for this seat that end after the requested start time
        List<Booking> bookings = bookingRepository.findBySeat(seatRepository.findById(seatId).get())
                .stream()
                .filter(b -> b.getEndTime().isAfter(startTime) && 
                             (b.getStatus() == Booking.BookingStatus.RESERVED || 
                              b.getStatus() == Booking.BookingStatus.CHECKED_IN))
                .sorted((b1, b2) -> b1.getEndTime().compareTo(b2.getEndTime()))
                .collect(Collectors.toList());
        
        if (bookings.isEmpty()) {
            return "Now";
        }
        
        // Return the end time of the earliest booking
        return bookings.get(0).getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

      // Admin capabilities
    // ================== READ OPERATIONS (Location-Aware) ==================
    
    public List<SeatDTO> getAllSeatsForAdmin(Location location) {
        LocalDateTime now = LocalDateTime.now();
        List<Seat> seats;
        
        if (location != null) {
            seats = seatRepository.findByLocation(location);
        } else {
            seats = seatRepository.findAll();
        }
        
        return seats.stream()
                .map(seat -> mapSeatToDTO(seat, now, now.plusHours(1)))
                .collect(Collectors.toList());
    }
    
    public List<SeatDTO> getDisabledSeats(Location location) {
        LocalDateTime now = LocalDateTime.now();
        List<Seat> seats;
        
        if (location != null) {
            seats = seatRepository.findByIsDisabledAndLocation(true, location);
        } else {
            seats = seatRepository.findByIsDisabled(true);
        }
        
        return seats.stream()
                .map(seat -> mapSeatToDTO(seat, now, now.plusHours(1)))
                .collect(Collectors.toList());
    }
    
    public SeatDTO getSeatById(Long id, Location userLocation) {
        LocalDateTime now = LocalDateTime.now();
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));
        
        // Location-based access control
        if (userLocation != null && !seat.belongsToLocation(userLocation)) {
            throw new ResourceNotFoundException("You don't have access to this seat");
        }
        
        return mapSeatToDTO(seat, now, now.plusHours(1));
    }

    // ================== CREATE OPERATION (Location-Aware) ==================
    
    @Transactional
    public SeatDTO createSeat(SeatDTO seatDTO, Location userLocation) {
        Seat seat = new Seat();
        seat.setSeatNumber(seatDTO.getSeatNumber());
        seat.setZoneType(seatDTO.getZoneType());
        seat.setHasDesktop(seatDTO.isHasDesktop());
        seat.setDescription(seatDTO.getDescription());
        
        // Set location - librarians can only create seats in their location
        if (userLocation != null) {
            seat.setLocation(userLocation);
        } else {
            // Admin can specify location, defaults to location in DTO
            seat.setLocation(Location.valueOf(seatDTO.getLocation()));
        }
        
        // Set floor if provided
        if (seatDTO.getFloar() != null) {
            seat.setFloar(seatDTO.getFloar());
        }
        
        seat.setDisabled(false);
        seat = seatRepository.save(seat);

        // Generate QR Code
        try {
            String token = qrGenerationService.generateUniqueToken();
            String qrUrl = qrGenerationService.generateSeatQRUrl(token);
            byte[] qrImage = qrGenerationService.generateQRCodeImage(qrUrl, seat.getSeatNumber());
            String filename = qrGenerationService.generateAndSaveQRCode(qrUrl, "SEAT", seat.getSeatNumber());
            String imagePath = qrStorageService.storeQRCode(qrImage, filename, "seat");
            
            seat.setQrCodeToken(token);
            seat.setQrCodeUrl(qrUrl);
            seat.setQrImagePath(imagePath);
            seat.setQrGeneratedAt(LocalDateTime.now());
            seat = seatRepository.save(seat);
            
        } catch (Exception e) {
            System.err.println("Failed to generate QR code for new seat: " + e.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        return mapSeatToDTO(seat, now, now.plusHours(1));
    }

    // ================== UPDATE OPERATIONS (Location-Aware) ==================
    
    @Transactional
    public SeatDTO updateSeat(Long id, SeatDTO seatDTO, Location userLocation) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));
        
        // Location-based access control
        if (userLocation != null && !seat.belongsToLocation(userLocation)) {
            throw new ResourceNotFoundException("You don't have access to this seat");
        }
        
        // Update fields
        if (seatDTO.getSeatNumber() != null) {
            seat.setSeatNumber(seatDTO.getSeatNumber());
        }
        if (seatDTO.getZoneType() != null) {
            seat.setZoneType(seatDTO.getZoneType());
        }
        // if (seatDTO.isHasDesktop() != null) {
        //     seat.setHasDesktop(seatDTO.isHasDesktop());
        // }
        if (seatDTO.getDescription() != null) {
            seat.setDescription(seatDTO.getDescription());
        }
        if (seatDTO.getFloar() != null) {
            seat.setFloar(seatDTO.getFloar());
        }
        // Only admins can change location
        if (userLocation == null && seatDTO.getLocation() != null) {
            seat.setLocation(Location.valueOf(seatDTO.getLocation()));
        }
        
        seat = seatRepository.save(seat);
        LocalDateTime now = LocalDateTime.now();
        
        return mapSeatToDTO(seat, now, now.plusHours(1));
    }
    
    @Transactional
    public List<SeatDTO> bulkUpdateSeats(BulkSeatUpdateRequest bulkUpdateRequest, Location userLocation) {
        List<Seat> seats = seatRepository.findAllById(bulkUpdateRequest.getSeatIds());
        
        if (seats.size() != bulkUpdateRequest.getSeatIds().size()) {
            Set<Long> foundIds = seats.stream().map(Seat::getId).collect(Collectors.toSet());
            Set<Long> missingIds = bulkUpdateRequest.getSeatIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new ResourceNotFoundException("Could not find seats with IDs: " + missingIds);
        }
        
        // Location-based access control for bulk operations
        if (userLocation != null) {
            List<Seat> unauthorizedSeats = seats.stream()
                    .filter(seat -> !seat.belongsToLocation(userLocation))
                    .collect(Collectors.toList());
            
            if (!unauthorizedSeats.isEmpty()) {
                throw new ResourceNotFoundException("You don't have access to some of the selected seats");
            }
        }
        
        seats.forEach(seat -> {
            if (bulkUpdateRequest.getZoneType() != null) {
                seat.setZoneType(bulkUpdateRequest.getZoneType());
            }
            if (bulkUpdateRequest.getHasDesktop() != null) {
                seat.setHasDesktop(bulkUpdateRequest.getHasDesktop());
            }
            if (bulkUpdateRequest.getIsDisabled() != null) {
                seat.setDisabled(bulkUpdateRequest.getIsDisabled());
            }
            if (bulkUpdateRequest.getDescription() != null) {
                seat.setDescription(bulkUpdateRequest.getDescription());
            }
            if (bulkUpdateRequest.getFloar() != null) {
                seat.setFloar(bulkUpdateRequest.getFloar());
            }
            // Only admins can change location in bulk
            if (userLocation == null && bulkUpdateRequest.getLocation() != null) {
                seat.setLocation(bulkUpdateRequest.getLocation());
            }
        });
        
        seats = seatRepository.saveAll(seats);
        LocalDateTime now = LocalDateTime.now();
        
        return seats.stream()
                .map(seat -> mapSeatToDTO(seat, now, now.plusHours(1)))
                .collect(Collectors.toList());
    }
    
    @Transactional
    public SeatDTO toggleDesktopProperty(Long id, Location userLocation) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));
        
        // Location-based access control
        if (userLocation != null && !seat.belongsToLocation(userLocation)) {
            throw new ResourceNotFoundException("You don't have access to this seat");
        }
        
        seat.setHasDesktop(!seat.isHasDesktop());
        seat = seatRepository.save(seat);
        
        LocalDateTime now = LocalDateTime.now();
        return mapSeatToDTO(seat, now, now.plusHours(1));
    }

    @Transactional
    public List<SeatDTO> bulkToggleDesktop(Set<Long> seatIds, Location userLocation) {
        List<Seat> seats = seatRepository.findAllById(seatIds);
        
        if (seats.size() != seatIds.size()) {
            Set<Long> foundIds = seats.stream().map(Seat::getId).collect(Collectors.toSet());
            Set<Long> missingIds = seatIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new ResourceNotFoundException("Could not find seats with IDs: " + missingIds);
        }
        
        // Location-based access control
        if (userLocation != null) {
            List<Seat> unauthorizedSeats = seats.stream()
                    .filter(seat -> !seat.belongsToLocation(userLocation))
                    .collect(Collectors.toList());
            
            if (!unauthorizedSeats.isEmpty()) {
                throw new ResourceNotFoundException("You don't have access to some of the selected seats");
            }
        }
        
        seats.forEach(seat -> seat.setHasDesktop(!seat.isHasDesktop()));
        seats = seatRepository.saveAll(seats);
        
        LocalDateTime now = LocalDateTime.now();
        return seats.stream()
                .map(seat -> mapSeatToDTO(seat, now, now.plusHours(1)))
                .collect(Collectors.toList());
    }
    
    @Transactional
    public List<SeatDTO> disableSeats(Set<Long> seatIds, boolean disabled, Location userLocation) {
        List<Seat> seats = seatRepository.findAllById(seatIds);
        
        if (seats.size() != seatIds.size()) {
            Set<Long> foundIds = seats.stream().map(Seat::getId).collect(Collectors.toSet());
            Set<Long> missingIds = seatIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new ResourceNotFoundException("Could not find seats with IDs: " + missingIds);
        }
        
        // Location-based access control
        if (userLocation != null) {
            List<Seat> unauthorizedSeats = seats.stream()
                    .filter(seat -> !seat.belongsToLocation(userLocation))
                    .collect(Collectors.toList());
            
            if (!unauthorizedSeats.isEmpty()) {
                throw new ResourceNotFoundException("You don't have access to some of the selected seats");
            }
        }
        
        seats.forEach(seat -> seat.setDisabled(disabled));
        seats = seatRepository.saveAll(seats);
        
        LocalDateTime now = LocalDateTime.now();
        return seats.stream()
                .map(seat -> mapSeatToDTO(seat, now, now.plusHours(1)))
                .collect(Collectors.toList());
    }

    // ================== DELETE OPERATION (Location-Aware) ==================
    
    @Transactional
    public void deleteSeat(Long id, Location userLocation) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));
        
        // Location-based access control
        if (userLocation != null && !seat.belongsToLocation(userLocation)) {
            throw new ResourceNotFoundException("You don't have access to this seat");
        }
        
        seatRepository.deleteById(id);
    }
    
    // Helper method to get current user's location from security context
    private Location getCurrentUserLocation() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return user.isLibrarian() ? user.getLocation() : null;
    }




    // Bulk seat creation operation

    @Transactional
public List<SeatDTO> bulkCreateSeats(BulkSeatCreationRequest request, Location userLocation) {
    // Location-based access control
    Location targetLocation = userLocation != null ? userLocation : request.getLocation();
    
    if (userLocation != null && !userLocation.equals(request.getLocation())) {
        throw new ResourceNotFoundException("You can only create seats in your location: " + userLocation);
    }
    
    List<Seat> seatsToCreate = new ArrayList<>();
    List<String> failedSeats = new ArrayList<>();
    
    // Generate seats from startNumber to endNumber
    for (int i = request.getStartNumber(); i <= request.getEndNumber(); i++) {
        String seatNumber = request.getSeatNumberPrefix() + String.format("%03d", i); // e.g., GS001, GS002
        
        // Check if seat number already exists
        if (seatRepository.existsBySeatNumber(seatNumber)) {
            failedSeats.add(seatNumber);
            continue;
        }
        
        Seat seat = new Seat();
        seat.setSeatNumber(seatNumber);
        seat.setZoneType(request.getZoneType());
        seat.setHasDesktop(request.getHasDesktop());
        seat.setDescription(request.getDescription());
        seat.setLocation(targetLocation);
        seat.setFloar(request.getFloar());
        seat.setDisabled(false);
        
        seatsToCreate.add(seat);
    }
    
    if (!failedSeats.isEmpty()) {
        throw new IllegalArgumentException("Some seat numbers already exist: " + failedSeats);
    }
    
    // Save all seats
    List<Seat> savedSeats = seatRepository.saveAll(seatsToCreate);
    
    // Generate QR codes for all seats (optional - can be done later)
    generateQRCodesForSeats(savedSeats);
    
    // Convert to DTOs
    LocalDateTime now = LocalDateTime.now();
    return savedSeats.stream()
            .map(seat -> mapSeatToDTO(seat, now, now.plusHours(1)))
            .collect(Collectors.toList());
}

private void generateQRCodesForSeats(List<Seat> seats) {
    seats.parallelStream().forEach(seat -> {
        try {
            String token = qrGenerationService.generateUniqueToken();
            String qrUrl = qrGenerationService.generateSeatQRUrl(token);
            byte[] qrImage = qrGenerationService.generateQRCodeImage(qrUrl, seat.getSeatNumber());
            String filename = qrGenerationService.generateAndSaveQRCode(qrUrl, "SEAT", seat.getSeatNumber());
            String imagePath = qrStorageService.storeQRCode(qrImage, filename, "seat");
            
            seat.setQrCodeToken(token);
            seat.setQrCodeUrl(qrUrl);
            seat.setQrImagePath(imagePath);
            seat.setQrGeneratedAt(LocalDateTime.now());
            
            seatRepository.save(seat);
        } catch (Exception e) {
            System.err.println("Failed to generate QR code for seat " + seat.getSeatNumber() + ": " + e.getMessage());
        }
    });
}
}