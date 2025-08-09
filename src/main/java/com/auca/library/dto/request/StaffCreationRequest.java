package com.auca.library.dto.request;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

import com.auca.library.model.Location;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StaffCreationRequest {
    @NotBlank
    @Size(min = 3, max = 100)
    private String fullName;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    @NotBlank
    @Size(min = 3, max = 20)
    private String employeeId;

    @NotBlank
    @Size(max = 15)
    private String phone;

    @NotNull
    private Location location;

    @NotBlank
    private String role; // LIBRARIAN, ADMIN, EQUIPMENT_ADMIN, HOD

    // Librarian based fields
    private Set<DayOfWeek> workingDays;
    private boolean activeThisWeek = false;
    private boolean defaultLibrarian = false;

    private List<Long> courseIds;
     
       public boolean isDefaultLibrarian() {
        return defaultLibrarian;
    }
    
    public void setDefaultLibrarian(boolean defaultLibrarian) {
        this.defaultLibrarian = defaultLibrarian;
    }
    
    // For backward compatibility, if needed
    public boolean isDefault() {
        return defaultLibrarian;
    }
    
    public void setDefault(boolean isDefault) {
        this.defaultLibrarian = isDefault;
    }
    
    public boolean isActiveThisWeek() {
        return activeThisWeek;
    }

    public void setActiveThisWeek(boolean activeThisWeek) {
        this.activeThisWeek = activeThisWeek;
    }
    
}