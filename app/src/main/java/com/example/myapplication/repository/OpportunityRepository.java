package com.example.myapplication.repository;

import android.content.Context;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.database.OpportunityDao;
import com.example.myapplication.model.Opportunity;
import com.example.myapplication.network.ApiCallback;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.NetworkUtils;
import com.example.myapplication.network.RetrofitApiService;
import com.example.myapplication.util.SampleDataGenerator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpportunityRepository {
    
    private final Context appContext;
    private final OpportunityDao opportunityDao;
    private final ApiService apiService;
    private final ExecutorService executorService;
    
    public OpportunityRepository(Context context) {
        appContext = context.getApplicationContext();
        AppDatabase database = AppDatabase.getInstance(context);
        opportunityDao = database.opportunityDao();
        apiService = RetrofitApiService.getInstance();
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
    
    // Network-first sync with local fallback
    public void getAllOpportunities(OnOpportunitiesLoadedListener listener) {
        if (!NetworkUtils.isNetworkAvailable(appContext)) {
            loadAllFromLocal(listener, "No internet connection. Showing cached opportunities.");
            return;
        }

        apiService.fetchOpportunities(new ApiCallback<List<Opportunity>>() {
            @Override
            public void onSuccess(List<Opportunity> opportunities) {
                executorService.execute(() -> {
                    mergeAndPersist(opportunities);
                    List<Opportunity> local = opportunityDao.getAllOpportunities();
                    if (listener != null) {
                        listener.onLoaded(local);
                    }
                });
            }

            @Override
            public void onError(String error) {
                loadAllFromLocal(listener, "Failed to sync latest opportunities. Showing cached data.");
            }
        });
    }
    
    // Get recommended opportunities
    public void getRecommendedOpportunities(OnOpportunitiesLoadedListener listener) {
        getAllOpportunities(opportunities -> {
            executorService.execute(() -> {
                List<Opportunity> recommended = opportunityDao.getRecommendedOpportunities();
                if (listener != null) {
                    listener.onLoaded(recommended);
                }
            });
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

        default void onError(String message) {
        }
    }
    
    public interface OnOperationCompleteListener {
        void onComplete(boolean success);
    }

    private void loadAllFromLocal(OnOpportunitiesLoadedListener listener, String fallbackMessage) {
        executorService.execute(() -> {
            try {
                List<Opportunity> opportunities = opportunityDao.getAllOpportunities();
                if (opportunities == null || opportunities.isEmpty()) {
                    List<Opportunity> seeded = SampleDataGenerator.generateOpportunities(60);
                    opportunityDao.insertAll(seeded);
                    opportunities = opportunityDao.getAllOpportunities();
                }
                if (listener != null) {
                    if (fallbackMessage != null && !fallbackMessage.trim().isEmpty()) {
                        listener.onError(fallbackMessage);
                    }
                    listener.onLoaded(opportunities);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError("Unable to load opportunities from local cache.");
                    listener.onLoaded(java.util.Collections.emptyList());
                }
            }
        });
    }

    private void mergeAndPersist(List<Opportunity> remoteItems) {
        if (remoteItems == null || remoteItems.isEmpty()) {
            return;
        }

        List<Opportunity> existing = opportunityDao.getAllOpportunitiesSync();
        OpportunitySyncHelper.mergeLocalStateIntoRemote(remoteItems, existing);

        opportunityDao.deleteAll();
        opportunityDao.insertAll(remoteItems);
    }
}
