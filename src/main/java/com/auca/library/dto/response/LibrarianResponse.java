package com.auca.library.dto.response;

import java.time.LocalDate;

import lombok.Data;

@Data
public class LibrarianResponse {

    private Long id;
    private String name;
    private String phone;
    private boolean activeToday;
    private boolean isDefault;
    private LocalDate workingDay;

    public boolean isDefault() {
    return isDefault;
 }

    public void setIsDefault(boolean isDefault) {
    this.isDefault = isDefault;
 }

}
