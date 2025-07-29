package com.auca.library.dto.request;

import java.util.HashSet;
import java.util.List;

import com.auca.library.model.Location;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.DayOfWeek;
import java.util.Set;

@Data
public class MultiRoleStaffCreationRequest {
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

    @NotEmpty
    private List<String> roles; // Multiple roles like ["LIBRARIAN", "ADMIN"]

    // Librarian-specific fields (if one of the roles is LIBRARIAN)
    private Set<DayOfWeek> workingDays = new HashSet<>();
    private String workingDaysString; // "Monday, Wednesday, Friday"
    private boolean activeThisWeek;
    private boolean isDefaultLibrarian;
    private boolean isActiveToday;
    public List<String> getRoles() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}