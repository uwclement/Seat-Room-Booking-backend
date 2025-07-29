package com.auca.library.dto.response;

import com.auca.library.model.Location;
import lombok.Data;

import java.time.DayOfWeek;
import java.util.Set;

@Data
public class LibrarianResponse {
    
    private Long id;
    private String fullName;
    private String email;
    private String employeeId;
    private String phone;
    private Location location;
    private String locationDisplayName;
    private Set<DayOfWeek> workingDays;
    private String workingDaysString;
    private boolean activeThisWeek;
    private boolean isDefaultLibrarian;
    private boolean isActiveToday;

    public void setId(Long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setDefaultLibrarian(boolean isDefaultLibrarian) {
    this.isDefaultLibrarian = isDefaultLibrarian;
}

public void setActiveToday(boolean isActiveToday) {
    this.isActiveToday = isActiveToday;
}
    
}