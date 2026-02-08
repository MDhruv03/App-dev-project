package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.myapplication.database.Converters;

import java.util.Date;
import java.util.List;

@Entity(tableName = "user_preferences")
@TypeConverters(Converters.class)
public class UserPreferences {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private int userId;
    
    // Preferred roles
    private List<String> preferredRoles;
    
    // Preferred locations
    private List<String> preferredLocations;
    
    // Job type preferences
    private boolean preferRemote;
    private boolean preferOnsite;
    private boolean preferHybrid;
    
    // Compensation preference
    private boolean preferPaid;
    
    // Opportunity types
    private boolean interestedInInternships;
    private boolean interestedInJobs;
    private boolean interestedInHackathons;
    
    // Notification preferences
    private boolean enableDailyRecommendations;
    private boolean enableDeadlineReminders;
    private boolean enableInterviewReminders;
    private boolean enableNewOpportunityAlerts;
    
    // Update timestamp
    private Date updatedAt;
    
    // Constructors
    public UserPreferences() {
        this.updatedAt = new Date();
        this.preferPaid = true;
        this.interestedInInternships = true;
        this.interestedInJobs = true;
        this.interestedInHackathons = true;
        this.enableDailyRecommendations = true;
        this.enableDeadlineReminders = true;
        this.enableInterviewReminders = true;
        this.enableNewOpportunityAlerts = true;
    }
    
    public UserPreferences(int userId) {
        this();
        this.userId = userId;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public List<String> getPreferredRoles() { return preferredRoles; }
    public void setPreferredRoles(List<String> preferredRoles) { 
        this.preferredRoles = preferredRoles; 
    }
    
    public List<String> getPreferredLocations() { return preferredLocations; }
    public void setPreferredLocations(List<String> preferredLocations) { 
        this.preferredLocations = preferredLocations; 
    }
    
    public boolean isPreferRemote() { return preferRemote; }
    public void setPreferRemote(boolean preferRemote) { this.preferRemote = preferRemote; }
    
    public boolean isPreferOnsite() { return preferOnsite; }
    public void setPreferOnsite(boolean preferOnsite) { this.preferOnsite = preferOnsite; }
    
    public boolean isPreferHybrid() { return preferHybrid; }
    public void setPreferHybrid(boolean preferHybrid) { this.preferHybrid = preferHybrid; }
    
    public boolean isPreferPaid() { return preferPaid; }
    public void setPreferPaid(boolean preferPaid) { this.preferPaid = preferPaid; }
    
    public boolean isInterestedInInternships() { return interestedInInternships; }
    public void setInterestedInInternships(boolean interestedInInternships) { 
        this.interestedInInternships = interestedInInternships; 
    }
    
    public boolean isInterestedInJobs() { return interestedInJobs; }
    public void setInterestedInJobs(boolean interestedInJobs) { 
        this.interestedInJobs = interestedInJobs; 
    }
    
    public boolean isInterestedInHackathons() { return interestedInHackathons; }
    public void setInterestedInHackathons(boolean interestedInHackathons) { 
        this.interestedInHackathons = interestedInHackathons; 
    }
    
    public boolean isEnableDailyRecommendations() { return enableDailyRecommendations; }
    public void setEnableDailyRecommendations(boolean enableDailyRecommendations) { 
        this.enableDailyRecommendations = enableDailyRecommendations; 
    }
    
    public boolean isEnableDeadlineReminders() { return enableDeadlineReminders; }
    public void setEnableDeadlineReminders(boolean enableDeadlineReminders) { 
        this.enableDeadlineReminders = enableDeadlineReminders; 
    }
    
    public boolean isEnableInterviewReminders() { return enableInterviewReminders; }
    public void setEnableInterviewReminders(boolean enableInterviewReminders) { 
        this.enableInterviewReminders = enableInterviewReminders; 
    }
    
    public boolean isEnableNewOpportunityAlerts() { return enableNewOpportunityAlerts; }
    public void setEnableNewOpportunityAlerts(boolean enableNewOpportunityAlerts) { 
        this.enableNewOpportunityAlerts = enableNewOpportunityAlerts; 
    }
    
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
