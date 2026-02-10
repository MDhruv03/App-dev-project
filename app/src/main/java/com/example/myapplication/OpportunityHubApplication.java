package com.example.myapplication;

import android.app.Application;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.model.Opportunity;
import com.example.myapplication.network.MockApiService;
import com.example.myapplication.util.SampleDataGenerator;

import java.util.List;
import java.util.concurrent.Executors;

public class OpportunityHubApplication extends Application {
    
    private static OpportunityHubApplication instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // Initialize Mock API Service
        MockApiService.getInstance();
        
        // Initialize database with sample data
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                
                // Check if database is empty
                List<Opportunity> existing = db.opportunityDao().getAllOpportunities();
                
                if (existing == null || existing.isEmpty()) {
                    // Populate with sample data
                    List<Opportunity> opportunities = SampleDataGenerator.generateOpportunities(50);
                    for (Opportunity opp : opportunities) {
                        db.opportunityDao().insert(opp);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    public static OpportunityHubApplication getInstance() {
        return instance;
    }
}
