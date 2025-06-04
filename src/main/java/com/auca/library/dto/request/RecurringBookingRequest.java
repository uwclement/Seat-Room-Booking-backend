package com.auca.library.dto.request;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecurringBookingRequest {
    @NotNull(message = "Recurrence type is required")
    private RecurrenceType recurrenceType = RecurrenceType.WEEKLY;

    @Min(value = 1, message = "Recurrence interval must be at least 1")
    private Integer recurrenceInterval = 1;

    private Set<DayOfWeek> daysOfWeek;

    @NotNull(message = "Series start date is required")
    private LocalDateTime seriesStartDate;

    private LocalDateTime seriesEndDate;

    public enum RecurrenceType {
        DAILY, WEEKLY, MONTHLY, CUSTOM
    }
}

