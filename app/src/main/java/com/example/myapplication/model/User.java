package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.myapplication.database.Converters;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Entity(tableName = "users")
@TypeConverters(Converters.class)
public class User {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String name;
    private String email;
    private String phone;
    private String profileImageUrl;
    
    // Skills with proficiency level
    private Map<String, Integer> skills; // Skill name -> Proficiency (1-5)
    
    // Resume
    private String resumePath;
    private List<String> extractedSkills;
    
    // Created/Updated timestamps
    private Date createdAt;
    private Date updatedAt;
    
    // Constructors
    public User() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
    
    public User(String name, String email) {
        this();
        this.name = name;
        this.email = email;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { 
        this.profileImageUrl = profileImageUrl; 
    }
    
    public Map<String, Integer> getSkills() { return skills; }
    public void setSkills(Map<String, Integer> skills) { this.skills = skills; }
    
    public String getResumePath() { return resumePath; }
    public void setResumePath(String resumePath) { this.resumePath = resumePath; }
    
    public List<String> getExtractedSkills() { return extractedSkills; }
    public void setExtractedSkills(List<String> extractedSkills) { 
        this.extractedSkills = extractedSkills; 
    }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
