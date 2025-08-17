package com.auca.library.dto.response.analytics;


public class AnalyticsCard {
    private String title;
    private String value;
    private String trend; // "up", "down", "stable"
    private String trendValue;
    private String icon;
    private String color; // "green", "red", "blue", "yellow"
    
    public AnalyticsCard() {}
    
    public AnalyticsCard(String title, String value, String trend, String trendValue) {
        this.title = title;
        this.value = value;
        this.trend = trend;
        this.trendValue = trendValue;
    }
    
    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    
    public String getTrend() { return trend; }
    public void setTrend(String trend) { this.trend = trend; }
    
    public String getTrendValue() { return trendValue; }
    public void setTrendValue(String trendValue) { this.trendValue = trendValue; }
    
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
}
