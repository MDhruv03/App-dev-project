package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.myapplication.database.Converters;

import java.util.Date;
import java.util.List;

@Entity(tableName = "opportunities")
@TypeConverters(Converters.class)
public class Opportunity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String title;
    private String company;
    private String type; // "internship", "job", "hackathon"
    private String role;
    private String location;
    private boolean isRemote;
    private boolean isPaid;
    private String description;
    private List<String> requiredSkills;
    private Date deadline;
    private String applyLink;
    private String imageUrl;
    
    // Recommendation score
    private double recommendationScore;
    private int matchPercentage;
    
    // Metadata
    private boolean isSaved;
    private boolean isApplied;
    private Date createdAt;
    private Date updatedAt;
    
    // Trending/Popularity score (mock)
    private int popularityScore;
    
    // Constructors
    public Opportunity() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.isSaved = false;
        this.isApplied = false;
    }
    
    public Opportunity(String title, String company, String type, String role, 
                      String location, boolean isRemote, boolean isPaid) {
        this();
        this.title = title;
        this.company = company;
        this.type = type;
        this.role = role;
        this.location = location;
        this.isRemote = isRemote;
        this.isPaid = isPaid;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public boolean isRemote() { return isRemote; }
    public void setRemote(boolean remote) { isRemote = remote; }
    
    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { isPaid = paid; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { 
        this.requiredSkills = requiredSkills; 
    }
    
    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }
    
    public String getApplyLink() { return applyLink; }
    public void setApplyLink(String applyLink) { this.applyLink = applyLink; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public double getRecommendationScore() { return recommendationScore; }
    public void setRecommendationScore(double recommendationScore) { 
        this.recommendationScore = recommendationScore; 
    }
    
    public int getMatchPercentage() { return matchPercentage; }
    public void setMatchPercentage(int matchPercentage) { 
        this.matchPercentage = matchPercentage; 
    }
    
    public boolean isSaved() { return isSaved; }
    public void setSaved(boolean saved) { isSaved = saved; }
    
    public boolean isApplied() { return isApplied; }
    public void setApplied(boolean applied) { isApplied = applied; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    
    public int getPopularityScore() { return popularityScore; }
    public void setPopularityScore(int popularityScore) { 
        this.popularityScore = popularityScore; 
    }
}
