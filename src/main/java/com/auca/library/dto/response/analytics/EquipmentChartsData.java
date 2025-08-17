package com.auca.library.dto.response.analytics;

public class EquipmentChartsData {
    private ChartData unitStatusChart;
    private ChartData mostRequestedChart;
    private ChartData assignmentTrendsChart;
    
    public EquipmentChartsData() {}
    
    // Getters and Setters
    public ChartData getUnitStatusChart() { return unitStatusChart; }
    public void setUnitStatusChart(ChartData unitStatusChart) { this.unitStatusChart = unitStatusChart; }
    
    public ChartData getMostRequestedChart() { return mostRequestedChart; }
    public void setMostRequestedChart(ChartData mostRequestedChart) { this.mostRequestedChart = mostRequestedChart; }
    
    public ChartData getAssignmentTrendsChart() { return assignmentTrendsChart; }
    public void setAssignmentTrendsChart(ChartData assignmentTrendsChart) { this.assignmentTrendsChart = assignmentTrendsChart; }
    
}
