package com.example.myapplication.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.ai.RecommendationEngine;
import com.example.myapplication.model.Opportunity;
import com.example.myapplication.model.UserProfile;
import com.example.myapplication.repository.ApplicationRepository;
import com.example.myapplication.repository.OpportunityRepository;
import com.example.myapplication.repository.UserProfileRepository;

import java.util.Arrays;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class OpportunityViewModel extends AndroidViewModel {
    
    private final OpportunityRepository repository;
    private final ApplicationRepository applicationRepository;
    private final UserProfileRepository userProfileRepository;
    private final MutableLiveData<List<Opportunity>> allOpportunities = new MutableLiveData<>();
    private final MutableLiveData<List<Opportunity>> recommendedOpportunities = new MutableLiveData<>();
    private final MutableLiveData<List<Opportunity>> savedOpportunities = new MutableLiveData<>();
    private final MutableLiveData<List<Opportunity>> filteredOpportunities = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final List<String> activeFilters = new ArrayList<>();
    private List<Opportunity> currentOpportunities = new ArrayList<>();
    private String activeSearchQuery = "";
    
    public OpportunityViewModel(@NonNull Application application) {
        super(application);
        repository = new OpportunityRepository(application.getApplicationContext());
        applicationRepository = new ApplicationRepository(application.getApplicationContext());
        userProfileRepository = new UserProfileRepository(application.getApplicationContext());
    }
    
    private void injectMatchPercentages(List<Opportunity> opportunities) {
        if (opportunities == null || opportunities.isEmpty()) return;
        UserProfile profile = userProfileRepository.getProfileSync();
        if (profile != null) {
            for (Opportunity opp : opportunities) {
                opp.setMatchPercentage(RecommendationEngine.getMatchPercentage(opp, profile));
            }
        }
    }

    // Load all opportunities
    public void loadAllOpportunities() {
        isLoading.postValue(true);
        repository.getAllOpportunities(new OpportunityRepository.OnOpportunitiesLoadedListener() {
            @Override
            public void onLoaded(List<Opportunity> opportunities) {
                isLoading.postValue(false);
                injectMatchPercentages(opportunities);
                currentOpportunities = opportunities;
                allOpportunities.postValue(opportunities);
                applyFilters();
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }
    
    // Load recommended opportunities
    public void loadRecommendedOpportunities() {
        repository.getAllOpportunities(new OpportunityRepository.OnOpportunitiesLoadedListener() {
            @Override
            public void onLoaded(List<Opportunity> opportunities) {
                UserProfile profile = userProfileRepository.getProfileSync();
                injectMatchPercentages(opportunities);
                List<Opportunity> recommended = RecommendationEngine.getRecommended(opportunities, profile);
                recommendedOpportunities.postValue(recommended);
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue(message);
            }
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
        if (opportunity == null) {
            return;
        }

        if (opportunity.isApplied()) {
            return;
        }

        opportunity.setApplied(true);
        repository.updateAppliedStatus(opportunity.getId(), true, success -> {
            if (success) {
                com.example.myapplication.model.Application app = new com.example.myapplication.model.Application();
                app.setUserId(1);
                app.setOpportunityId(opportunity.getId());
                app.setCompany(opportunity.getCompany());
                app.setPosition(opportunity.getTitle());
                app.setStatus("applied");
                app.setAppliedAt(new Date());
                app.setAppliedDate(new Date());
                app.setSavedAt(new Date());
                applicationRepository.insert(app, insertSuccess -> {
                });
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

    public void refresh() {
        loadAllOpportunities();
        loadRecommendedOpportunities();
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
        List<Opportunity> all = currentOpportunities;
        if (all == null || all.isEmpty()) {
            filteredOpportunities.postValue(new ArrayList<>());
            return;
        }

        String query = activeSearchQuery.toLowerCase(Locale.ROOT);
        List<String> queryTokens = expandQueryTokens(query);
        boolean requireRemote = activeFilters.contains("remote");
        boolean requirePaid = activeFilters.contains("paid");
        boolean requireRemoteYes = activeFilters.contains("remote_yes");
        boolean requireRemoteNo = activeFilters.contains("remote_no");
        boolean requirePaidYes = activeFilters.contains("paid_yes");
        boolean requirePaidNo = activeFilters.contains("paid_no");

        boolean remotePositive = requireRemote || requireRemoteYes;
        boolean remoteNegative = requireRemoteNo;
        boolean paidPositive = requirePaid || requirePaidYes;
        boolean paidNegative = requirePaidNo;

        List<String> typeFilters = activeFilters.stream()
                .filter(filter -> filter.equals("internship") || filter.equals("job") || filter.equals("hackathon"))
                .collect(Collectors.toList());

        List<Opportunity> filtered = all.stream()
                .filter(o -> typeFilters.isEmpty() || (o.getType() != null && typeFilters.contains(o.getType().toLowerCase(Locale.ROOT))))
            .filter(o -> !remotePositive || o.isRemote())
            .filter(o -> !remoteNegative || !o.isRemote())
            .filter(o -> !paidPositive || o.isPaid())
            .filter(o -> !paidNegative || !o.isPaid())
                .filter(o -> matchesQuery(o, query, queryTokens))
                .collect(Collectors.toList());

        filteredOpportunities.postValue(filtered);
    }

    private boolean matchesQuery(Opportunity opportunity, String query, List<String> queryTokens) {
        if (query == null || query.isEmpty()) {
            return true;
        }

        List<String> searchable = new ArrayList<>();
        searchable.add(safe(opportunity.getTitle()));
        searchable.add(safe(opportunity.getCompany()));
        searchable.add(safe(opportunity.getRole()));
        searchable.add(safe(opportunity.getLocation()));
        searchable.add(safe(opportunity.getDescription()));
        searchable.add(safe(opportunity.getType()));
        searchable.add(opportunity.isRemote() ? "remote" : "onsite");

        if (opportunity.getRequiredSkills() != null) {
            for (String skill : opportunity.getRequiredSkills()) {
                searchable.add(safe(skill));
            }
        }

        String bag = String.join(" ", searchable);
        for (String token : queryTokens) {
            if (bag.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private List<String> expandQueryTokens(String query) {
        List<String> tokens = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            return tokens;
        }

        tokens.add(query);
        if (query.equals("sde") || query.equals("software")) {
            tokens.addAll(Arrays.asList("software engineer", "software development", "developer", "backend", "frontend"));
        }
        if (query.equals("ml") || query.equals("ai")) {
            tokens.addAll(Arrays.asList("machine learning", "data science", "artificial intelligence"));
        }
        if (query.equals("android")) {
            tokens.addAll(Arrays.asList("kotlin", "java", "mobile"));
        }
        if (query.equals("web")) {
            tokens.addAll(Arrays.asList("frontend", "react", "javascript", "full stack"));
        }
        return tokens;
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
