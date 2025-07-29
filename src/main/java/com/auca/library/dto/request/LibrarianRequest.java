package com.auca.library.dto.request;

import com.auca.library.model.Location;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.util.Set;

@Data
public class LibrarianRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotEmpty(message = "At least one working day must be specified")
    private Set<DayOfWeek> workingDays;
    
    @NotNull(message = "Location is required")
    private Location location;
    
    private boolean activeThisWeek = false;
    
    private boolean isDefaultLibrarian = false;
}