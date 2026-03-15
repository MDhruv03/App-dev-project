package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {

    @PrimaryKey
    private int id = 1;

    private String name;
    private String email;
    private String skills;
    private String preferredRoles;
    private String preferredLocation;
    private String jobTypePreference;
    private boolean paidPreference;

    public UserProfile() {
        this.id = 1;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getPreferredRoles() {
        return preferredRoles;
    }

    public void setPreferredRoles(String preferredRoles) {
        this.preferredRoles = preferredRoles;
    }

    public String getPreferredLocation() {
        return preferredLocation;
    }

    public void setPreferredLocation(String preferredLocation) {
        this.preferredLocation = preferredLocation;
    }

    public String getJobTypePreference() {
        return jobTypePreference;
    }

    public void setJobTypePreference(String jobTypePreference) {
        this.jobTypePreference = jobTypePreference;
    }

    public boolean isPaidPreference() {
        return paidPreference;
    }

    public void setPaidPreference(boolean paidPreference) {
        this.paidPreference = paidPreference;
    }
}
