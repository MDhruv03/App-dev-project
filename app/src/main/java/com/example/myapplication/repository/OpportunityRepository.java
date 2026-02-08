package com.example.myapplication.repository;

import android.content.Context;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.database.OpportunityDao;
import com.example.myapplication.model.Opportunity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpportunityRepository {
    
    private final OpportunityDao opportunityDao;
    private final ExecutorService executorService;
    
    public OpportunityRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        opportunityDao = database.opportunityDao();
        executorService = Executors.newSingleThreadExecutor();
    }
    
    // Insert opportunity
    public void insert(Opportunity opportunity, OnOperationCompleteListener listener) {
        executorService.execute(() -> {
            long id = opportunityDao.insert(opportunity);
            if (listener != null) {
                listener.onComplete(id > 0);
            }
        });
    }
    
    // Get all opportunities
    public void getAllOpportunities(OnOpportunitiesLoadedListener listener) {
        executorService.execute(() -> {
            List<Opportunity> opportunities = opportunityDao.getAllOpportunities();
            if (listener != null) {
                listener.onLoaded(opportunities);
            }
        });
    }
    
    // Get recommended opportunities
    public void getRecommendedOpportunities(OnOpportunitiesLoadedListener listener) {
        executorService.execute(() -> {
            List<Opportunity> opportunities = opportunityDao.getRecommendedOpportunities();
            if (listener != null) {
                listener.onLoaded(opportunities);
            }
        });
    }
    
    // Get saved opportunities
    public void getSavedOpportunities(OnOpportunitiesLoadedListener listener) {
        executorService.execute(() -> {
            List<Opportunity> opportunities = opportunityDao.getSavedOpportunities();
            if (listener != null) {
                listener.onLoaded(opportunities);
            }
        });
    }
    
    // Update saved status
    public void updateSavedStatus(int id, boolean saved, OnOperationCompleteListener listener) {
        executorService.execute(() -> {
            opportunityDao.updateSavedStatus(id, saved);
            if (listener != null) {
                listener.onComplete(true);
            }
        });
    }
    
    // Update applied status
    public void updateAppliedStatus(int id, boolean applied, OnOperationCompleteListener listener) {
        executorService.execute(() -> {
            opportunityDao.updateAppliedStatus(id, applied);
            if (listener != null) {
                listener.onComplete(true);
            }
        });
    }
    
    // Delete opportunity
    public void delete(Opportunity opportunity, OnOperationCompleteListener listener) {
        executorService.execute(() -> {
            opportunityDao.delete(opportunity);
            if (listener != null) {
                listener.onComplete(true);
            }
        });
    }
    
    // Interfaces for callbacks
    public interface OnOpportunitiesLoadedListener {
        void onLoaded(List<Opportunity> opportunities);
    }
    
    public interface OnOperationCompleteListener {
        void onComplete(boolean success);
    }
}
