package com.auca.library.util;

public class AppConstants {
    // Common constants
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String DEFAULT_SORT_BY = "id";
    public static final String DEFAULT_SORT_DIRECTION = "asc";
    
    // User roles
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    
    // Email constants
    public static final String EMAIL_VERIFICATION_SUBJECT = "Verify your AUCA Library Account";
    public static final String EMAIL_VERIFICATION_BASE_URL = "http://localhost:8080/api/auth/verify?token=";
    public static final long EMAIL_VERIFICATION_EXPIRATION_HOURS = 24;
    
    // Reservation constants
    public static final int RESERVATION_CHECKIN_MINUTES = 15;
    public static final int DEFAULT_MAX_BOOKING_HOURS = 2;
    
    // Message constants
    public static final String USER_REGISTERED_SUCCESS = "User registered successfully! Please check your email to verify your account.";
    public static final String EMAIL_VERIFIED_SUCCESS = "Email verified successfully!";
    public static final String EMAIL_VERIFICATION_FAILED = "Invalid verification token";
    public static final String EMAIL_ALREADY_VERIFIED = "Email is already verified";
    
    // Error messages
    public static final String EMAIL_ALREADY_EXISTS = "Email is already in use!";
    public static final String STUDENT_ID_ALREADY_EXISTS = "Student ID is already in use!";
    public static final String ROLE_NOT_FOUND = "Error: Role is not found.";
    public static final String USER_NOT_FOUND = "User not found";
}