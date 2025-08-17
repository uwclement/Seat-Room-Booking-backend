package com.auca.library.dto.response.analytics;

import java.util.List;
import java.util.Map;

public class ChartData {


    private String type; // "pie", "bar", "line", "doughnut"
    private String title;
    private List<String> labels;
    private List<Number> data;
    private Map<String, Object> options;
    
    public ChartData() {}
    
    public ChartData(String type, String title, List<String> labels, List<Number> data) {
        this.type = type;
        this.title = title;
        this.labels = labels;
        this.data = data;
    }
    
    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> labels) { this.labels = labels; }
    
    public List<Number> getData() { return data; }
    public void setData(List<Number> data) { this.data = data; }
    
    public Map<String, Object> getOptions() { return options; }
    public void setOptions(Map<String, Object> options) { this.options = options; }
    
}
