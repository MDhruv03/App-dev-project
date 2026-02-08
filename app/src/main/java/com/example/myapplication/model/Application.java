package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.myapplication.database.Converters;

import java.util.Date;

@Entity(tableName = "applications")
@TypeConverters(Converters.class)
public class Application {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private int opportunityId;
    private int userId;
    
    // Application status
    private String status; // "saved", "applied", "interview", "rejected", "accepted"
    
    // Interview details
    private Date interviewDate;
    private String interviewNotes;
    private String interviewLocation;
    private boolean interviewReminderSet;
    
    // Timeline
    private Date savedAt;
    private Date appliedAt;
    private Date interviewScheduledAt;
    private Date statusUpdatedAt;
    
    // Notes
    private String notes;
    
    // Constructors
    public Application() {
        this.savedAt = new Date();
        this.statusUpdatedAt = new Date();
        this.status = "saved";
    }
    
    public Application(int opportunityId, int userId) {
        this();
        this.opportunityId = opportunityId;
        this.userId = userId;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getOpportunityId() { return opportunityId; }
    public void setOpportunityId(int opportunityId) { this.opportunityId = opportunityId; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { 
        this.status = status;
        this.statusUpdatedAt = new Date();
    }
    
    public Date getInterviewDate() { return interviewDate; }
    public void setInterviewDate(Date interviewDate) { this.interviewDate = interviewDate; }
    
    public String getInterviewNotes() { return interviewNotes; }
    public void setInterviewNotes(String interviewNotes) { 
        this.interviewNotes = interviewNotes; 
    }
    
    public String getInterviewLocation() { return interviewLocation; }
    public void setInterviewLocation(String interviewLocation) { 
        this.interviewLocation = interviewLocation; 
    }
    
    public boolean isInterviewReminderSet() { return interviewReminderSet; }
    public void setInterviewReminderSet(boolean interviewReminderSet) { 
        this.interviewReminderSet = interviewReminderSet; 
    }
    
    public Date getSavedAt() { return savedAt; }
    public void setSavedAt(Date savedAt) { this.savedAt = savedAt; }
    
    public Date getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Date appliedAt) { this.appliedAt = appliedAt; }
    
    public Date getInterviewScheduledAt() { return interviewScheduledAt; }
    public void setInterviewScheduledAt(Date interviewScheduledAt) { 
        this.interviewScheduledAt = interviewScheduledAt; 
    }
    
    public Date getStatusUpdatedAt() { return statusUpdatedAt; }
    public void setStatusUpdatedAt(Date statusUpdatedAt) { 
        this.statusUpdatedAt = statusUpdatedAt; 
    }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
