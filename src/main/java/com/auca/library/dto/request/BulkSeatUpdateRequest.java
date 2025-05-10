package com.auca.library.dto.request;

import lombok.Data;
import java.util.Set;

@Data
public class BulkSeatUpdateRequest {
    private Set<Long> seatIds;
    private String zoneType;
    private Boolean hasDesktop;
    private Boolean isDisabled;
    private String description;
}