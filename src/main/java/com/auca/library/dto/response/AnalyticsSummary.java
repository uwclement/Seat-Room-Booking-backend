package com.auca.library.dto.response;

import java.time.LocalDateTime;

public class AnalyticsSummary {
    private String title;
    private LocalDateTime generatedAt;
    private String location;
    private String dateRange;
    
    // Constructors, getters, setters
    public AnalyticsSummary() {
        this.generatedAt = LocalDateTime.now();
    }
    
    public AnalyticsSummary(String title, String location, String dateRange) {
        this.title = title;
        this.location = location;
        this.dateRange = dateRange;
        this.generatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getDateRange() { return dateRange; }
    public void setDateRange(String dateRange) { this.dateRange = dateRange; }
}