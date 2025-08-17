package com.auca.library.dto.response.analytics;

import com.auca.library.dto.response.AnalyticsSummary;
import java.util.List;

public class UserAnalyticsSummary extends AnalyticsSummary{
    private List<AnalyticsCard> summaryCards;
    
    public UserAnalyticsSummary() {
        super();
    }
    
    public UserAnalyticsSummary(String location, String dateRange) {
        super("User Analytics", location, dateRange);
    }
    
    public List<AnalyticsCard> getSummaryCards() { return summaryCards; }
    public void setSummaryCards(List<AnalyticsCard> summaryCards) { this.summaryCards = summaryCards; }


}
