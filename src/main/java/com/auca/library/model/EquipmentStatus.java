package com.auca.library.model;

public enum EquipmentStatus {
    AVAILABLE("Available"),
    UNDER_MAINTENANCE("Under Maintenance"),
    DAMAGED("Damaged"),
    LOST("Lost"),
    RESERVED("Reserved");

    private final String displayName;

    EquipmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}