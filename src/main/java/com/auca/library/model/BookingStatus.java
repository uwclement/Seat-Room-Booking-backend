package com.auca.library.model;

public enum BookingStatus {
    PENDING,           // Waiting for approval (if required)
    CONFIRMED,         // Approved and active
    CANCELLED,         // Cancelled by user or admin
    NO_SHOW,          // User didn't check in
    COMPLETED,        // Successfully completed
    REJECTED          // Rejected by admin
}