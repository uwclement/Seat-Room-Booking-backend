package com.auca.library.dto.response.analytics;

import com.auca.library.dto.response.AnalyticsSummary;
import java.util.List;

public class SeatAnalyticsSummary extends AnalyticsSummary {
    private List<AnalyticsCard> summaryCards;
    
    public SeatAnalyticsSummary() {
        super();
    }
    
    public SeatAnalyticsSummary(String location, String dateRange) {
        super("Seat Analytics", location, dateRange);
    }
    
    public List<AnalyticsCard> getSummaryCards() { return summaryCards; }
    public void setSummaryCards(List<AnalyticsCard> summaryCards) { this.summaryCards = summaryCards; }
    
    
}