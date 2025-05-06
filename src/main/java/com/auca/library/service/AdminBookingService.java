package com.auca.library.service;

import com.auca.library.dto.response.BookingResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Booking;
import com.auca.library.model.Booking.BookingStatus;
import com.auca.library.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminBookingService {

    @Autowired
    private BookingRepository bookingRepository;

    public List<BookingResponse> getCurrentBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> currentBookings = bookingRepository.findByEndTimeAfterAndStartTimeBeforeAndStatusIn(
                now, now, List.of(BookingStatus.RESERVED, BookingStatus.CHECKED_IN));
        
        return currentBookings.stream()
                .map(this::mapBookingToResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        List<Booking> bookings = bookingRepository.findByStartTimeBetween(startOfDay, endOfDay);
        
        return bookings.stream()
                .map(this::mapBookingToResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsInDateRange(LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        
        List<Booking> bookings = bookingRepository.findByStartTimeBetween(startDateTime, endDateTime);
        
        return bookings.stream()
                .map(this::mapBookingToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationTime(LocalDateTime.now());
        booking.setCancellationReason("Cancelled by administrator");
        
        bookingRepository.save(booking);
        return new MessageResponse("Booking cancelled successfully");
    }

    public List<BookingResponse> getBookingsByUser(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        
        return bookings.stream()
                .map(this::mapBookingToResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsBySeat(Long seatId) {
        List<Booking> bookings = bookingRepository.findBySeatId(seatId);
        
        return bookings.stream()
                .map(this::mapBookingToResponse)
                .collect(Collectors.toList());
    }

    // Helper method for mapping Booking to BookingResponse
    private BookingResponse mapBookingToResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setUserId(booking.getUser().getId());
        response.setUserName(booking.getUser().getFullName());
        response.setSeatId(booking.getSeat().getId());
        response.setSeatNumber(booking.getSeat().getSeatNumber());
        response.setStartTime(booking.getStartTime());
        response.setEndTime(booking.getEndTime());
        response.setStatus(booking.getStatus().name());
        response.setCheckinTime(booking.getCheckinTime());
        response.setCheckoutTime(booking.getCheckoutTime());
        response.setCancellationTime(booking.getCancellationTime());
        response.setCancellationReason(booking.getCancellationReason());
        return response;
    }
}