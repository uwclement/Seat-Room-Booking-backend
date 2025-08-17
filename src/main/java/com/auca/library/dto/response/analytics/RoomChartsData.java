package com.auca.library.dto.response.analytics;

public class RoomChartsData {

    private ChartData bookingStatusChart;
    private ChartData roomUtilizationChart;
    private ChartData monthlyTrendsChart;
    
    public RoomChartsData() {}
    
    // Getters and Setters
    public ChartData getBookingStatusChart() { return bookingStatusChart; }
    public void setBookingStatusChart(ChartData bookingStatusChart) { this.bookingStatusChart = bookingStatusChart; }
    
    public ChartData getRoomUtilizationChart() { return roomUtilizationChart; }
    public void setRoomUtilizationChart(ChartData roomUtilizationChart) { this.roomUtilizationChart = roomUtilizationChart; }
    
    public ChartData getMonthlyTrendsChart() { return monthlyTrendsChart; }
    public void setMonthlyTrendsChart(ChartData monthlyTrendsChart) { this.monthlyTrendsChart = monthlyTrendsChart; }
    
}
