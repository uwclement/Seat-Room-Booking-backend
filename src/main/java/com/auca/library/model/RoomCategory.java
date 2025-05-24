package com.auca.library.model;

public enum RoomCategory {
    LIBRARY_ROOM,    // Requires booking, follows library schedule
    STUDY_ROOM,      // Requires booking, can request equipment
    CLASS_ROOM       // Open access, admin controls availability
}