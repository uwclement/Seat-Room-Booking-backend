package com.auca.library.dto.response.analytics;

import com.auca.library.dto.response.AnalyticsSummary;
import java.util.List;

public class EquipmentAnalyticsSummary extends AnalyticsSummary{
    private List<AnalyticsCard> summaryCards;
    
    public EquipmentAnalyticsSummary() {
        super();
    }
    
    public EquipmentAnalyticsSummary(String location, String dateRange) {
        super("Equipment Analytics", location, dateRange);
    }
    
    public List<AnalyticsCard> getSummaryCards() { return summaryCards; }
    public void setSummaryCards(List<AnalyticsCard> summaryCards) { this.summaryCards = summaryCards; }

}
