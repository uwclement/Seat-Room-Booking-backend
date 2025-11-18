package com.auca.library.dto.request;

import java.util.Set;

import com.auca.library.model.Location;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class BulkSeatUpdateRequest {
    private Set<Long> seatIds;
    private String zoneType;
    private Boolean hasDesktop;
    private Boolean isDisabled;
    private String description;
    private Integer floar; 
    @NotNull(message = "Location is required")
    private Location location;
}