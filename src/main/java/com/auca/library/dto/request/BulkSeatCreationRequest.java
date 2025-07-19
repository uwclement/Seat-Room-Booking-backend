package com.auca.library.dto.request;


import com.auca.library.model.Location;

import lombok.Data;

@Data
public class BulkSeatCreationRequest {
    private String seatNumberPrefix;        // e.g., "GS" for Gishushu, "MS" for Masoro
    private Integer startNumber;            // e.g., 1
    private Integer endNumber;              // e.g., 50
    private String zoneType;                // e.g., "SILENT", "COLLABORATION"
    private Boolean hasDesktop;             // true/false
    private String description;             // Optional description
    private Location location;              // GISHUSHU, MASORO
    private Integer floar; 
}
