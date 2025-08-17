package com.auca.library.dto.response.analytics;

import com.auca.library.dto.response.AnalyticsSummary;
import java.util.List;

public class RoomAnalyticsSummary extends AnalyticsSummary {
     private List<AnalyticsCard> summaryCards;
    
    public RoomAnalyticsSummary() {
        super();
    }
    
    public RoomAnalyticsSummary(String location, String dateRange) {
        super("Room Analytics", location, dateRange);
    }
    
    public List<AnalyticsCard> getSummaryCards() { return summaryCards; }
    public void setSummaryCards(List<AnalyticsCard> summaryCards) { this.summaryCards = summaryCards; }

   
}
