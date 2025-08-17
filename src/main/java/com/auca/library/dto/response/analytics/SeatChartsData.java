package com.auca.library.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatChartsData {
    
    private ChartData hourlyUsageChart;
    private ChartData zoneDistributionChart;
    private ChartData weeklyTrendChart;
}