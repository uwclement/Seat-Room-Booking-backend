package com.auca.library.dto.response;

import java.time.DayOfWeek;
import java.time.LocalDate;
import lombok.Data;

@Data
public class RecurringClosureDTO {
    private DayOfWeek dayOfWeek; // If recurring on specific day of week
    private Integer dayOfMonth; // If recurring on specific day of month
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean closedAllDay;
    private String openTime;
    private String closeTime;
    private String reason;
}