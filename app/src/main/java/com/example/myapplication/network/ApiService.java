package com.example.myapplication.network;

import com.example.myapplication.model.Opportunity;

import java.util.List;

/**
 * API service interface for opportunity data
 */
public interface ApiService {
    
    /**
     * Fetch all opportunities
     */
    void fetchOpportunities(ApiCallback<List<Opportunity>> callback);
    
    /**
     * Fetch recommended opportunities
     */
    void fetchRecommendedOpportunities(ApiCallback<List<Opportunity>> callback);
    
    /**
     * Search opportunities
     */
    void searchOpportunities(String query, ApiCallback<List<Opportunity>> callback);
    
    /**
     * Filter opportunities
     */
    void filterOpportunities(FilterCriteria criteria, ApiCallback<List<Opportunity>> callback);
    
    /**
     * Fetch opportunity details
     */
    void fetchOpportunityDetails(int opportunityId, ApiCallback<Opportunity> callback);
    
    /**
     * Submit application
     */
    void submitApplication(int opportunityId, ApplicationData data, ApiCallback<Boolean> callback);
    
    /**
     * Sync saved opportunities
     */
    void syncSavedOpportunities(List<Integer> savedIds, ApiCallback<List<Opportunity>> callback);
    
    /**
     * Filter criteria class
     */
    class FilterCriteria {
        public String type;
        public String location;
        public Boolean remote;
        public Boolean paid;
        public String experienceLevel;
        public List<String> requiredSkills;
        public String sortBy;
        
        public FilterCriteria() {}
    }
    
    /**
     * Application data class
     */
    class ApplicationData {
        public String fullName;
        public String email;
        public String phone;
        public String resumePath;
        public String coverLetter;
        public List<String> skills;
        
        public ApplicationData() {}
    }
}
