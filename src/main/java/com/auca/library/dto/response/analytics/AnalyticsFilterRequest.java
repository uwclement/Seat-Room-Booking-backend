package com.auca.library.dto.response.analytics;

import java.time.LocalDateTime;

public class AnalyticsFilterRequest {

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String location; // "MASORO", "GISHUSHU", "ALL"
    private String period; // "TODAY", "WEEK", "MONTH", "QUARTER", "YEAR", "CUSTOM"
    
    public AnalyticsFilterRequest() {}
    
    // Getters and Setters
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    
}
