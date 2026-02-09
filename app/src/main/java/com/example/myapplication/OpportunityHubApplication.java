package com.example.myapplication;

import android.app.Application;
import android.util.Log;

import com.example.myapplication.database.AppDatabase;

public class OpportunityHubApplication extends Application {
    
    private static final String TAG = "OpportunityHubApp";
    private static OpportunityHubApplication instance;
    private AppDatabase database;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate started");
        instance = this;
        
        try {
            // Initialize database
            database = AppDatabase.getInstance(this);
            Log.d(TAG, "Database initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing database", e);
        }
        
        Log.d(TAG, "Application onCreate completed");
    }
    
    public static OpportunityHubApplication getInstance() {
        return instance;
    }
    
    public AppDatabase getDatabase() {
        return database;
    }
}
