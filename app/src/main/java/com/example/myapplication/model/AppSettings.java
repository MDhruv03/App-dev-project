package com.example.myapplication.model;

/**
 * App settings model
 */
public class AppSettings {
    private boolean notificationsEnabled;
    private boolean deadlineNotifications;
    private boolean interviewNotifications;
    private boolean newOpportunityNotifications;
    private int notificationAdvanceHours; // Hours before deadline to notify
    
    private boolean darkModeEnabled;
    private boolean autoSync;
    private int syncFrequencyHours;
    
    private String defaultSortBy;
    private boolean showRemoteOnly;
    private boolean showPaidOnly;
    
    private boolean analyticsEnabled;
    private boolean crashReportingEnabled;
    
    public AppSettings() {
        // Default values
        this.notificationsEnabled = true;
        this.deadlineNotifications = true;
        this.interviewNotifications = true;
        this.newOpportunityNotifications = true;
        this.notificationAdvanceHours = 24;
        
        this.darkModeEnabled = false;
        this.autoSync = true;
        this.syncFrequencyHours = 6;
        
        this.defaultSortBy = "recommendation";
        this.showRemoteOnly = false;
        this.showPaidOnly = false;
        
        this.analyticsEnabled = true;
        this.crashReportingEnabled = true;
    }
    
    // Getters and Setters
    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { 
        this.notificationsEnabled = notificationsEnabled; 
    }
    
    public boolean isDeadlineNotifications() { return deadlineNotifications; }
    public void setDeadlineNotifications(boolean deadlineNotifications) { 
        this.deadlineNotifications = deadlineNotifications; 
    }
    
    public boolean isInterviewNotifications() { return interviewNotifications; }
    public void setInterviewNotifications(boolean interviewNotifications) { 
        this.interviewNotifications = interviewNotifications; 
    }
    
    public boolean isNewOpportunityNotifications() { return newOpportunityNotifications; }
    public void setNewOpportunityNotifications(boolean newOpportunityNotifications) { 
        this.newOpportunityNotifications = newOpportunityNotifications; 
    }
    
    public int getNotificationAdvanceHours() { return notificationAdvanceHours; }
    public void setNotificationAdvanceHours(int notificationAdvanceHours) { 
        this.notificationAdvanceHours = notificationAdvanceHours; 
    }
    
    public boolean isDarkModeEnabled() { return darkModeEnabled; }
    public void setDarkModeEnabled(boolean darkModeEnabled) { 
        this.darkModeEnabled = darkModeEnabled; 
    }
    
    public boolean isAutoSync() { return autoSync; }
    public void setAutoSync(boolean autoSync) { this.autoSync = autoSync; }
    
    public int getSyncFrequencyHours() { return syncFrequencyHours; }
    public void setSyncFrequencyHours(int syncFrequencyHours) { 
        this.syncFrequencyHours = syncFrequencyHours; 
    }
    
    public String getDefaultSortBy() { return defaultSortBy; }
    public void setDefaultSortBy(String defaultSortBy) { this.defaultSortBy = defaultSortBy; }
    
    public boolean isShowRemoteOnly() { return showRemoteOnly; }
    public void setShowRemoteOnly(boolean showRemoteOnly) { 
        this.showRemoteOnly = showRemoteOnly; 
    }
    
    public boolean isShowPaidOnly() { return showPaidOnly; }
    public void setShowPaidOnly(boolean showPaidOnly) { this.showPaidOnly = showPaidOnly; }
    
    public boolean isAnalyticsEnabled() { return analyticsEnabled; }
    public void setAnalyticsEnabled(boolean analyticsEnabled) { 
        this.analyticsEnabled = analyticsEnabled; 
    }
    
    public boolean isCrashReportingEnabled() { return crashReportingEnabled; }
    public void setCrashReportingEnabled(boolean crashReportingEnabled) { 
        this.crashReportingEnabled = crashReportingEnabled; 
    }
    
    // Alias methods for backward compatibility
    public void setDeadlineReminders(boolean enabled) {
        setDeadlineNotifications(enabled);
    }
    
    public void setInterviewReminders(boolean enabled) {
        setInterviewNotifications(enabled);
    }
}
