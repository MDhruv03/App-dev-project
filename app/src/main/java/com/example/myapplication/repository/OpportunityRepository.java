package com.example.myapplication.repository;

import android.content.Context;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.database.OpportunityDao;
import com.example.myapplication.model.Opportunity;
import com.example.myapplication.network.MockApiService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpportunityRepository {
    
    private final OpportunityDao opportunityDao;
    private final MockApiService apiService;
    private final ExecutorService executorService;
    
    public OpportunityRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        opportunityDao = database.opportunityDao();
        apiService = MockApiService.getInstance();
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
    
    // Get all opportunities (fetch from API and cache locally)
    public void getAllOpportunities(OnOpportunitiesLoadedListener listener) {
        apiService.fetchOpportunities(opportunities -> {
            executorService.execute(() -> {
                // Clear and refresh local cache
                opportunityDao.deleteAll();
                for (Opportunity opp : opportunities) {
                    opportunityDao.insert(opp);
                }
                if (listener != null) {
                    listener.onLoaded(opportunities);
                }
            });
        });
    }
    
    // Get recommended opportunities
    public void getRecommendedOpportunities(OnOpportunitiesLoadedListener listener) {
        apiService.fetchRecommendedOpportunities(opportunities -> {
            if (listener != null) {
                listener.onLoaded(opportunities);
            }
        });
    }
    
    // Get saved opportunities
    public void getSavedOpportunities(OnOpportunitiesLoadedListener listener) {
        apiService.fetchSavedOpportunities(opportunities -> {
            if (listener != null) {
                listener.onLoaded(opportunities);
            }
        });
    }
    
    // Update saved status
    public void updateSavedStatus(int id, boolean saved, OnOperationCompleteListener listener) {
        apiService.updateOpportunitySaveStatus(id, saved, success -> {
            executorService.execute(() -> {
                opportunityDao.updateSavedStatus(id, saved);
                if (listener != null) {
                    listener.onComplete(success);
                }
            });
        });
    }
    
    // Update applied status
    public void updateAppliedStatus(int id, boolean applied, OnOperationCompleteListener listener) {
        apiService.updateOpportunityApplyStatus(id, applied, success -> {
            executorService.execute(() -> {
                opportunityDao.updateAppliedStatus(id, applied);
                if (listener != null) {
                    listener.onComplete(success);
                }
            });
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
