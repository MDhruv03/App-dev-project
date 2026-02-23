package com.example.myapplication.model;

import java.util.List;

/**
 * Filter model for advanced opportunity filtering
 */
public class OpportunityFilter {
    private String searchQuery;
    private List<String> selectedTypes; // internship, job, hackathon
    private List<String> selectedLocations;
    private boolean remoteOnly;
    private boolean paidOnly;
    private List<String> requiredSkills;
    private String experienceLevel;
    private String sortBy; // recommendation, deadline, match, company
    private int minMatchPercentage;
    private Integer minSalary;
    private Integer maxSalary;
    
    public OpportunityFilter() {
        this.remoteOnly = false;
        this.paidOnly = false;
        this.sortBy = "recommendation";
        this.minMatchPercentage = 0;
    }
    
    // Getters and Setters
    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
    
    public List<String> getSelectedTypes() { return selectedTypes; }
    public void setSelectedTypes(List<String> selectedTypes) { 
        this.selectedTypes = selectedTypes; 
    }
    
    public List<String> getSelectedLocations() { return selectedLocations; }
    public void setSelectedLocations(List<String> selectedLocations) { 
        this.selectedLocations = selectedLocations; 
    }
    
    public boolean isRemoteOnly() { return remoteOnly; }
    public void setRemoteOnly(boolean remoteOnly) { this.remoteOnly = remoteOnly; }
    
    public boolean isPaidOnly() { return paidOnly; }
    public void setPaidOnly(boolean paidOnly) { this.paidOnly = paidOnly; }
    
    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { 
        this.requiredSkills = requiredSkills; 
    }
    
    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { 
        this.experienceLevel = experienceLevel; 
    }
    
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    
    public int getMinMatchPercentage() { return minMatchPercentage; }
    public void setMinMatchPercentage(int minMatchPercentage) { 
        this.minMatchPercentage = minMatchPercentage; 
    }
    
    public Integer getMinSalary() { return minSalary; }
    public void setMinSalary(Integer minSalary) { this.minSalary = minSalary; }
    
    public Integer getMaxSalary() { return maxSalary; }
    public void setMaxSalary(Integer maxSalary) { this.maxSalary = maxSalary; }
    
    /**
     * Check if any filters are active
     */
    public boolean hasActiveFilters() {
        return (searchQuery != null && !searchQuery.isEmpty()) ||
               (selectedTypes != null && !selectedTypes.isEmpty()) ||
               (selectedLocations != null && !selectedLocations.isEmpty()) ||
               remoteOnly || paidOnly ||
               (requiredSkills != null && !requiredSkills.isEmpty()) ||
               (experienceLevel != null && !experienceLevel.isEmpty()) ||
               minMatchPercentage > 0 ||
               minSalary != null || maxSalary != null;
    }
    
    /**
     * Clear all filters
     */
    public void clearAll() {
        searchQuery = null;
        selectedTypes = null;
        selectedLocations = null;
        remoteOnly = false;
        paidOnly = false;
        requiredSkills = null;
        experienceLevel = null;
        sortBy = "recommendation";
        minMatchPercentage = 0;
        minSalary = null;
        maxSalary = null;
    }
}
