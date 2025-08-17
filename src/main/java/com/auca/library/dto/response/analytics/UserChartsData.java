package com.auca.library.dto.response.analytics;

public class UserChartsData {
    private ChartData userTypeChart;
    private ChartData dailyActiveChart;
    private ChartData departmentUsageChart;
    
    public UserChartsData() {}
    
    // Getters and Setters
    public ChartData getUserTypeChart() { return userTypeChart; }
    public void setUserTypeChart(ChartData userTypeChart) { this.userTypeChart = userTypeChart; }
    
    public ChartData getDailyActiveChart() { return dailyActiveChart; }
    public void setDailyActiveChart(ChartData dailyActiveChart) { this.dailyActiveChart = dailyActiveChart; }
    
    public ChartData getDepartmentUsageChart() { return departmentUsageChart; }
    public void setDepartmentUsageChart(ChartData departmentUsageChart) { this.departmentUsageChart = departmentUsageChart; }
}
