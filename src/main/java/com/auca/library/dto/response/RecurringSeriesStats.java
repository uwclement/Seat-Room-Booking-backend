package com.auca.library.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecurringSeriesStats {
    private Long totalBookings;
    private Long completedBookings;
    private Long cancelledBookings;
    private Long noShowBookings;
    
}