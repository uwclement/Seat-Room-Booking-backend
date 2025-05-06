package com.auca.library.service;

import com.auca.library.dto.request.BulkSeatUpdateRequest;
import com.auca.library.dto.request.SeatAvailabilityRequest;
import com.auca.library.dto.response.SeatDTO;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Booking;
import com.auca.library.model.Seat;
import com.auca.library.model.User;
import com.auca.library.repository.BookingRepository;
import com.auca.library.repository.SeatRepository;
import com.auca.library.repository.UserRepository;
import com.auca.library.repository.WaitListRepository;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    public List<SeatDTO> getAllSeats() {
        LocalDateTime now = LocalDateTime.now();
        List<Seat> seats = seatRepository.findByIsDisabled(false);
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
    @Transactional
    public SeatDTO createSeat(SeatDTO seatDTO) {
        Seat seat = new Seat();
        seat.setSeatNumber(seatDTO.getSeatNumber());
        seat.setZoneType(seatDTO.getZoneType());
        seat.setHasDesktop(seatDTO.isHasDesktop());
        seat.setDescription(seatDTO.getDescription());
        seat.setDisabled(false);
        
        seat = seatRepository.save(seat);
        LocalDateTime now = LocalDateTime.now();
        
        return mapSeatToDTO(seat, now, now.plusHours(1));
    }
    
    @Transactional
    public SeatDTO updateSeat(Long id, SeatDTO seatDTO) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));
        
        if (seatDTO.getSeatNumber() != null) {
            seat.setSeatNumber(seatDTO.getSeatNumber());
        }
        if (seatDTO.getZoneType() != null) {
            seat.setZoneType(seatDTO.getZoneType());
        }
        seat.setHasDesktop(seatDTO.isHasDesktop());
        if (seatDTO.getDescription() != null) {
            seat.setDescription(seatDTO.getDescription());
        }
        
        seat = seatRepository.save(seat);
        LocalDateTime now = LocalDateTime.now();
        
        return mapSeatToDTO(seat, now, now.plusHours(1));
    }
    
    @Transactional
    public void deleteSeat(Long id) {
        if (!seatRepository.existsById(id)) {
            throw new ResourceNotFoundException("Seat not found with id: " + id);
        }
        seatRepository.deleteById(id);
    }
    
    @Transactional
    public List<SeatDTO> bulkUpdateSeats(BulkSeatUpdateRequest bulkUpdateRequest) {
        List<Seat> seats = seatRepository.findAllById(bulkUpdateRequest.getSeatIds());
        
        if (seats.size() != bulkUpdateRequest.getSeatIds().size()) {
            Set<Long> foundIds = seats.stream().map(Seat::getId).collect(Collectors.toSet());
            Set<Long> missingIds = bulkUpdateRequest.getSeatIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new ResourceNotFoundException("Could not find seats with IDs: " + missingIds);
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
        });
        
        seats = seatRepository.saveAll(seats);
        LocalDateTime now = LocalDateTime.now();
        
        return seats.stream()
                .map(seat -> mapSeatToDTO(seat, now, now.plusHours(1)))
                .collect(Collectors.toList());
    }
    
    @Transactional
    public SeatDTO toggleDesktopProperty(Long id) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));
        
        seat.setHasDesktop(!seat.isHasDesktop());
        seat = seatRepository.save(seat);
        
        LocalDateTime now = LocalDateTime.now();
        return mapSeatToDTO(seat, now, now.plusHours(1));
    }
    
    @Transactional
    public List<SeatDTO> disableSeats(Set<Long> seatIds, boolean disabled) {
        List<Seat> seats = seatRepository.findAllById(seatIds);
        
        if (seats.size() != seatIds.size()) {
            Set<Long> foundIds = seats.stream().map(Seat::getId).collect(Collectors.toSet());
            Set<Long> missingIds = seatIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new ResourceNotFoundException("Could not find seats with IDs: " + missingIds);
        }
        
        seats.forEach(seat -> seat.setDisabled(disabled));
        seats = seatRepository.saveAll(seats);
        
        LocalDateTime now = LocalDateTime.now();
        return seats.stream()
                .map(seat -> mapSeatToDTO(seat, now, now.plusHours(1)))
                .collect(Collectors.toList());
    }
    
    public List<SeatDTO> getDisabledSeats() {
        LocalDateTime now = LocalDateTime.now();
        List<Seat> seats = seatRepository.findByIsDisabled(true);
        
        return seats.stream()
                .map(seat -> mapSeatToDTO(seat, now, now.plusHours(1)))
                .collect(Collectors.toList());
    }
    
    public List<SeatDTO> getAllSeatsForAdmin() {
        LocalDateTime now = LocalDateTime.now();
        List<Seat> seats = seatRepository.findAll();
        
        return seats.stream()
                .map(seat -> mapSeatToDTO(seat, now, now.plusHours(1)))
                .collect(Collectors.toList());
    }

}