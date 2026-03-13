package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.myapplication.database.Converters;

import java.util.Date;

/**
 * Activity log model for tracking user actions
 */
@Entity(tableName = "activity_logs")
@TypeConverters(Converters.class)
public class ActivityLog {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String activityType; // "view", "save", "apply", "search", "filter"
    private String description;
    private Date timestamp;
    private int relatedOpportunityId;
    private String metadata; // JSON string for additional data
    
    public ActivityLog() {
        this.timestamp = new Date();
    }
    
    @Ignore
    public ActivityLog(String activityType, String description) {
        this();
        this.activityType = activityType;
        this.description = description;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    
    public int getRelatedOpportunityId() { return relatedOpportunityId; }
    public void setRelatedOpportunityId(int relatedOpportunityId) { 
        this.relatedOpportunityId = relatedOpportunityId; 
    }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
