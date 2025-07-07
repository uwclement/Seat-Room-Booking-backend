package com.auca.library.dto.request;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;

public class BulkCancellationRequest {
    @NotEmpty(message = "Booking IDs cannot be empty")
    private List<Long> bookingIds;
    
    private String reason;
    
    public List<Long> getBookingIds() { return bookingIds; }
    public void setBookingIds(List<Long> bookingIds) { this.bookingIds = bookingIds; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}