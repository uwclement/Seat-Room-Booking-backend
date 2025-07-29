package com.auca.library.model;

public enum Location {
    GISHUSHU("GISHUSHU", "GSH"),
    MASORO("MASORO", "MSR");
    
    private final String displayName;
    private final String code;
    
    Location(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getCode() {
        return code;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
