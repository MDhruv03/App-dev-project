package com.example.myapplication.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.model.Opportunity;
import com.example.myapplication.repository.OpportunityRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OpportunityViewModel extends AndroidViewModel {
    
    private final OpportunityRepository repository;
    private final MutableLiveData<List<Opportunity>> allOpportunities = new MutableLiveData<>();
    private final MutableLiveData<List<Opportunity>> recommendedOpportunities = new MutableLiveData<>();
    private final MutableLiveData<List<Opportunity>> savedOpportunities = new MutableLiveData<>();
    private final MutableLiveData<List<Opportunity>> filteredOpportunities = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    public OpportunityViewModel(@NonNull Application application) {
        super(application);
        repository = new OpportunityRepository(application.getApplicationContext());
    }
    
    // Load all opportunities
    public void loadAllOpportunities() {
        isLoading.setValue(true);
        repository.getAllOpportunities(opportunities -> {
            isLoading.postValue(false);
            allOpportunities.postValue(opportunities);
            filteredOpportunities.postValue(opportunities);
        });
    }
    
    // Load recommended opportunities
    public void loadRecommendedOpportunities() {
        repository.getRecommendedOpportunities(opportunities -> {
            recommendedOpportunities.postValue(opportunities);
        });
    }
    
    // Load saved opportunities
    public void loadSavedOpportunities() {
        repository.getSavedOpportunities(opportunities -> {
            savedOpportunities.postValue(opportunities);
        });
    }
    
    // Toggle save status
    public void toggleSaveStatus(Opportunity opportunity) {
        boolean newStatus = !opportunity.isSaved();
        opportunity.setSaved(newStatus);
        repository.updateSavedStatus(opportunity.getId(), newStatus, success -> {
            if (success) {
                loadSavedOpportunities();
                loadAllOpportunities();
            }
        });
    }
    
    // Mark as applied
    public void markAsApplied(Opportunity opportunity) {
        opportunity.setApplied(true);
        repository.updateAppliedStatus(opportunity.getId(), true, success -> {
            if (success) {
                loadAllOpportunities();
            }
        });
    }
    
    // Filter by type
    public void filterByType(String type) {
        List<Opportunity> all = allOpportunities.getValue();
        if (all == null) return;
        
        if (type == null || type.equals("all")) {
            filteredOpportunities.setValue(all);
        } else {
            List<Opportunity> filtered = all.stream()
                    .filter(o -> o.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
            filteredOpportunities.setValue(filtered);
        }
    }
    
    // Search opportunities
    public void searchOpportunities(String query) {
        List<Opportunity> all = allOpportunities.getValue();
        if (all == null) return;
        
        if (query == null || query.trim().isEmpty()) {
            filteredOpportunities.setValue(all);
        } else {
            String lowerQuery = query.toLowerCase();
            List<Opportunity> filtered = all.stream()
                    .filter(o -> o.getTitle().toLowerCase().contains(lowerQuery) ||
                               o.getCompany().toLowerCase().contains(lowerQuery) ||
                               o.getRole().toLowerCase().contains(lowerQuery))
                    .collect(Collectors.toList());
            filteredOpportunities.setValue(filtered);
        }
    }
    
    // Insert opportunity
    public void insertOpportunity(Opportunity opportunity) {
        repository.insert(opportunity, success -> {
            if (success) {
                loadAllOpportunities();
            }
        });
    }
    
    // Getters for LiveData
    public LiveData<List<Opportunity>> getAllOpportunities() {
        return allOpportunities;
    }
    
    public LiveData<List<Opportunity>> getRecommendedOpportunities() {
        return recommendedOpportunities;
    }
    
    public LiveData<List<Opportunity>> getSavedOpportunities() {
        return savedOpportunities;
    }
    
    public LiveData<List<Opportunity>> getFilteredOpportunities() {
        return filteredOpportunities;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}
