package com.auca.library.dto.request;

import lombok.Data;

@Data
public class BookingExportRequest {
    private AdminBookingFilterRequest filterRequest;
    private String format = "CSV"; // CSV, EXCEL, PDF
    private Boolean includeParticipants = false;
    private Boolean includeEquipment = false;
}