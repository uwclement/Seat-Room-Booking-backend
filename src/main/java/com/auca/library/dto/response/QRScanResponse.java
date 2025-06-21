package com.auca.library.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class QRScanResponse {
    private boolean success;
    private String message;
    private String action; // CHECK_IN, VIEW_AVAILABILITY, TOO_EARLY, TOO_LATE, etc.   
    // Resource information
    private String resourceType; // SEAT or ROOM
    private Long resourceId;
    private String resourceIdentifier; // Seat number or room number
    private Object resourceDetails; // SeatDetailsResponse or RoomDetailsResponse   
    // User information
    private Long userId;
    private String userName;
    private boolean requiresAuthentication;    
    // Booking information
    private Object bookingDetails; // BookingDetailsResponse or RoomBookingDetailsResponse
    private LocalDateTime checkInTime;
    private boolean canCheckIn;
    private boolean canBook;   
    // Additional information
    private String availabilityInfo;
    private String warning;
    private String info;
    
    
    // Scan QRCode
    private String errorCode;
    private String alternativeAction;
    private Object alternativeDetails;
    private LocalDateTime checkInAvailableAt;
    private LocalDateTime bookingStartTime;
    private LocalDateTime bookingEndTime;
    private QRScanContext qrScanContext;
    private String actionButtonText;
    private String secondaryButtonText;

    
    // Helper methods to safely extract booking ID
    public Long getBookingId() {
        if (bookingDetails == null) {
            return null;
        }
        
        try {
            // Use reflection to call getBookingId() method
            java.lang.reflect.Method getBookingIdMethod = bookingDetails.getClass().getMethod("getBookingId");
            return (Long) getBookingIdMethod.invoke(bookingDetails);
        } catch (Exception e) {
            return null;
        }
    }
    
    // Helper method to check if booking details exist
    public boolean hasBookingDetails() {
        return bookingDetails != null;
    }
}