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
import java.util.Locale;
import java.util.stream.Collectors;

public class OpportunityViewModel extends AndroidViewModel {
    
    private final OpportunityRepository repository;
    private final MutableLiveData<List<Opportunity>> allOpportunities = new MutableLiveData<>();
    private final MutableLiveData<List<Opportunity>> recommendedOpportunities = new MutableLiveData<>();
    private final MutableLiveData<List<Opportunity>> savedOpportunities = new MutableLiveData<>();
    private final MutableLiveData<List<Opportunity>> filteredOpportunities = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final List<String> activeFilters = new ArrayList<>();
    private String activeSearchQuery = "";
    
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
            applyFilters();
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
        activeFilters.clear();
        if (type != null && !type.equalsIgnoreCase("all")) {
            activeFilters.add(type.toLowerCase(Locale.ROOT));
        }
        applyFilters();
    }
    
    // Search opportunities
    public void searchOpportunities(String query) {
        activeSearchQuery = query == null ? "" : query.trim();
        applyFilters();
    }

    public void updateFilters(List<String> filters) {
        activeFilters.clear();
        if (filters != null) {
            for (String filter : filters) {
                if (filter != null && !filter.trim().isEmpty()) {
                    activeFilters.add(filter.toLowerCase(Locale.ROOT));
                }
            }
        }
        applyFilters();
    }

    private void applyFilters() {
        List<Opportunity> all = allOpportunities.getValue();
        if (all == null) {
            filteredOpportunities.postValue(new ArrayList<>());
            return;
        }

        String query = activeSearchQuery.toLowerCase(Locale.ROOT);
        boolean requireRemote = activeFilters.contains("remote");
        boolean requirePaid = activeFilters.contains("paid");

        List<String> typeFilters = activeFilters.stream()
                .filter(filter -> filter.equals("internship") || filter.equals("job") || filter.equals("hackathon"))
                .collect(Collectors.toList());

        List<Opportunity> filtered = all.stream()
                .filter(o -> typeFilters.isEmpty() || typeFilters.contains(o.getType().toLowerCase(Locale.ROOT)))
                .filter(o -> !requireRemote || o.isRemote())
                .filter(o -> !requirePaid || o.isPaid())
                .filter(o -> query.isEmpty() ||
                        safe(o.getTitle()).contains(query) ||
                        safe(o.getCompany()).contains(query) ||
                        safe(o.getRole()).contains(query) ||
                        safe(o.getLocation()).contains(query) ||
                        safe(o.getDescription()).contains(query))
                .collect(Collectors.toList());

        filteredOpportunities.postValue(filtered);
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
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
