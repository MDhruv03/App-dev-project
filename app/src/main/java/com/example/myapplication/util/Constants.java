package com.example.myapplication.util;

/**
 * Application-wide constants
 */
public class Constants {
    
    // App Info
    public static final String APP_NAME = "OpportunityHub";
    public static final String APP_VERSION = "1.0.0";
    public static final String PACKAGE_NAME = "com.example.myapplication";
    
    // SharedPreferences Keys
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_IS_LOGGED_IN = "is_logged_in";
    public static final String PREF_FIRST_LAUNCH = "first_launch";
    public static final String PREF_LAST_SYNC = "last_sync";
    public static final String PREF_DARK_MODE = "dark_mode";
    
    // Database
    public static final String DATABASE_NAME = "opportunity_hub_db";
    public static final int DATABASE_VERSION = 1;
    
    // Notification Channels
    public static final String CHANNEL_DEADLINES = "deadlines_channel";
    public static final String CHANNEL_INTERVIEWS = "interviews_channel";
    public static final String CHANNEL_GENERAL = "general_channel";
    
    // Request Codes
    public static final int REQUEST_PERMISSION_NOTIFICATIONS = 1001;
    public static final int REQUEST_PERMISSION_STORAGE = 1002;
    public static final int REQUEST_CODE_PICK_FILE = 2001;
    public static final int REQUEST_CODE_PICK_IMAGE = 2002;
    
    // Intent Extras
    public static final String EXTRA_OPPORTUNITY_ID = "opportunity_id";
    public static final String EXTRA_APPLICATION_ID = "application_id";
    public static final String EXTRA_FRAGMENT_NAME = "fragment_name";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";
    
    // API
    public static final String BASE_URL = "https://api.opportunityhub.com/";
    public static final int API_TIMEOUT = 30; // seconds
    public static final int MAX_RETRY_ATTEMPTS = 3;
    
    // Pagination
    public static final int PAGE_SIZE = 20;
    public static final int INITIAL_LOAD_SIZE = 40;
    public static final int PREFETCH_DISTANCE = 10;
    
    // Cache
    public static final long CACHE_EXPIRY_TIME = 1000 * 60 * 60 * 24; // 24 hours
    public static final int MAX_CACHE_SIZE = 10 * 1024 * 1024; // 10 MB
    
    // UI
    public static final int SPLASH_DELAY = 2000; // ms
    public static final int DEBOUNCE_DELAY = 300; // ms for search
    public static final int ANIMATION_DURATION = 300; // ms
    
    // Opportunity Types
    public static final String TYPE_INTERNSHIP = "internship";
    public static final String TYPE_JOB = "job";
    public static final String TYPE_HACKATHON = "hackathon";
    
    // Application Status
    public static final String STATUS_SAVED = "saved";
    public static final String STATUS_APPLIED = "Applied";
    public static final String STATUS_INTERVIEW = "Interview";
    public static final String STATUS_REJECTED = "Rejected";
    public static final String STATUS_ACCEPTED = "Accepted";
    
    // Interview Domains
    public static final String DOMAIN_SDE = "SDE";
    public static final String DOMAIN_ML = "ML";
    public static final String DOMAIN_WEB = "Web";
    public static final String DOMAIN_ANDROID = "Android";
    public static final String DOMAIN_HR = "HR";
    
    // Interview Topics
    public static final String TOPIC_DSA = "DSA";
    public static final String TOPIC_OOPS = "OOPS";
    public static final String TOPIC_DBMS = "DBMS";
    public static final String TOPIC_SYSTEM_DESIGN = "System Design";
    public static final String TOPIC_BEHAVIORAL = "Behavioral";
    
    // Sort Options
    public static final String SORT_RECOMMENDATION = "recommendation";
    public static final String SORT_DEADLINE = "deadline";
    public static final String SORT_MATCH = "match";
    public static final String SORT_POPULARITY = "popularity";
    public static final String SORT_COMPANY = "company";
    
    // Notification Types
    public static final String NOTIFICATION_DEADLINE = "deadline";
    public static final String NOTIFICATION_INTERVIEW = "interview";
    public static final String NOTIFICATION_STATUS_UPDATE = "status_update";
    public static final String NOTIFICATION_NEW_OPPORTUNITY = "new_opportunity";
    
    // Activity Types
    public static final String ACTIVITY_VIEW = "view";
    public static final String ACTIVITY_SAVE = "save";
    public static final String ACTIVITY_APPLY = "apply";
    public static final String ACTIVITY_SEARCH = "search";
    public static final String ACTIVITY_FILTER = "filter";
    
    // Validation
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_NOTES_LENGTH = 500;
    public static final int MIN_SEARCH_LENGTH = 2;
    
    // Default Values
    public static final int DEFAULT_MATCH_PERCENTAGE = 70;
    public static final int DEADLINE_WARNING_DAYS = 7;
    public static final int INTERVIEW_REMINDER_HOURS = 24;
    
    // Error Messages
    public static final String ERROR_NETWORK = "Network connection failed. Please check your internet.";
    public static final String ERROR_UNKNOWN = "Something went wrong. Please try again.";
    public static final String ERROR_NO_DATA = "No data available.";
    public static final String ERROR_INVALID_INPUT = "Please enter valid information.";
    
    // Success Messages
    public static final String SUCCESS_SAVED = "Opportunity saved!";
    public static final String SUCCESS_APPLIED = "Application submitted!";
    public static final String SUCCESS_UPDATED = "Updated successfully!";
    public static final String SUCCESS_DELETED = "Deleted successfully!";
    
    // File Paths
    public static final String EXPORT_DIR = "OpportunityHub";
    public static final String RESUME_DIR = "Resumes";
    public static final String CACHE_DIR = "Cache";
    
    // Image
    public static final int MAX_IMAGE_SIZE = 2 * 1024 * 1024; // 2 MB
    public static final int IMAGE_QUALITY = 85;
    public static final int THUMBNAIL_SIZE = 200; // px
    
    // Analytics Events
    public static final String EVENT_OPPORTUNITY_VIEWED = "opportunity_viewed";
    public static final String EVENT_OPPORTUNITY_SAVED = "opportunity_saved";
    public static final String EVENT_APPLICATION_SUBMITTED = "application_submitted";
    public static final String EVENT_SEARCH_PERFORMED = "search_performed";
    public static final String EVENT_FILTER_APPLIED = "filter_applied";
    public static final String EVENT_INTERVIEW_PRACTICED = "interview_practiced";
    
    // Private constructor to prevent instantiation
    private Constants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}
