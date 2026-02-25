package com.example.myapplication.util;

import android.content.Context;

import com.example.myapplication.database.ActivityLogDao;
import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.model.ActivityLog;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Analytics tracker for user behavior and feature usage
 */
public class AnalyticsTracker {
    
    private static final String TAG = "AnalyticsTracker";
    
    private static AnalyticsTracker instance;
    private final ActivityLogDao activityLogDao;
    private final Map<String, Long> eventCounts;
    
    private AnalyticsTracker(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        activityLogDao = database.activityLogDao();
        eventCounts = new HashMap<>();
    }
    
    public static synchronized AnalyticsTracker getInstance(Context context) {
        if (instance == null) {
            instance = new AnalyticsTracker(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Track event
     */
    public void trackEvent(String eventName) {
        trackEvent(eventName, null);
    }
    
    /**
     * Track event with metadata
     */
    public void trackEvent(String eventName, Map<String, String> metadata) {
        Logger.d(TAG, "Event: " + eventName);
        
        // Update local count
        long count = eventCounts.getOrDefault(eventName, 0L) + 1;
        eventCounts.put(eventName, count);
        
        // Log to database
        new Thread(() -> {
            ActivityLog log = new ActivityLog();
            log.setActivityType(eventName);
            log.setDescription(createDescription(eventName, metadata));
            log.setTimestamp(new Date());
            
            if (metadata != null && metadata.containsKey("opportunity_id")) {
                try {
                    long opportunityId = Long.parseLong(metadata.get("opportunity_id"));
                    log.setRelatedOpportunityId((int) opportunityId);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
            
            activityLogDao.insert(log);
        }).start();
    }
    
    /**
     * Track screen view
     */
    public void trackScreenView(String screenName) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("screen", screenName);
        trackEvent("screen_view", metadata);
    }
    
    /**
     * Track opportunity view
     */
    public void trackOpportunityView(long opportunityId, String opportunityTitle) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("opportunity_id", String.valueOf(opportunityId));
        metadata.put("title", opportunityTitle);
        trackEvent(Constants.EVENT_OPPORTUNITY_VIEWED, metadata);
    }
    
    /**
     * Track opportunity save
     */
    public void trackOpportunitySave(long opportunityId, String opportunityTitle) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("opportunity_id", String.valueOf(opportunityId));
        metadata.put("title", opportunityTitle);
        trackEvent(Constants.EVENT_OPPORTUNITY_SAVED, metadata);
    }
    
    /**
     * Track application submission
     */
    public void trackApplicationSubmission(long opportunityId, String company) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("opportunity_id", String.valueOf(opportunityId));
        metadata.put("company", company);
        trackEvent(Constants.EVENT_APPLICATION_SUBMITTED, metadata);
    }
    
    /**
     * Track search
     */
    public void trackSearch(String query, int resultsCount) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("query", query);
        metadata.put("results", String.valueOf(resultsCount));
        trackEvent(Constants.EVENT_SEARCH_PERFORMED, metadata);
    }
    
    /**
     * Track filter application
     */
    public void trackFilterApplication(String filterType, String filterValue) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("filter_type", filterType);
        metadata.put("filter_value", filterValue);
        trackEvent(Constants.EVENT_FILTER_APPLIED, metadata);
    }
    
    /**
     * Track interview practice
     */
    public void trackInterviewPractice(String domain, String topic, int score) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("domain", domain);
        metadata.put("topic", topic);
        metadata.put("score", String.valueOf(score));
        trackEvent(Constants.EVENT_INTERVIEW_PRACTICED, metadata);
    }
    
    /**
     * Track feature usage
     */
    public void trackFeatureUsage(String featureName) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("feature", featureName);
        trackEvent("feature_used", metadata);
    }
    
    /**
     * Track error
     */
    public void trackError(String errorType, String errorMessage) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("error_type", errorType);
        metadata.put("error_message", errorMessage);
        trackEvent("error_occurred", metadata);
    }
    
    /**
     * Get event count
     */
    public long getEventCount(String eventName) {
        return eventCounts.getOrDefault(eventName, 0L);
    }
    
    /**
     * Get all event counts
     */
    public Map<String, Long> getAllEventCounts() {
        return new HashMap<>(eventCounts);
    }
    
    /**
     * Clear event counts
     */
    public void clearEventCounts() {
        eventCounts.clear();
    }
    
    /**
     * Create description from event and metadata
     */
    private String createDescription(String eventName, Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return eventName;
        }
        
        StringBuilder sb = new StringBuilder(eventName);
        sb.append(" - ");
        
        boolean first = true;
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
            first = false;
        }
        
        return sb.toString();
    }
}
