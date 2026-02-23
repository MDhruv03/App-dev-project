package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Notification model for deadline and interview reminders
 */
@Entity(tableName = "notifications")
public class Notification {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String type; // "deadline", "interview", "status_update", "new_opportunity"
    private String title;
    private String message;
    private long timestamp;
    private boolean isRead;
    private int relatedId; // Opportunity or Application ID
    private String actionUrl;
    
    public Notification() {
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }
    
    public Notification(String type, String title, String message) {
        this();
        this.type = type;
        this.title = title;
        this.message = message;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    
    public int getRelatedId() { return relatedId; }
    public void setRelatedId(int relatedId) { this.relatedId = relatedId; }
    
    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
}
