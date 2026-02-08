package com.example.myapplication;

import android.app.Application;

import com.example.myapplication.database.AppDatabase;

public class OpportunityHubApplication extends Application {
    
    private static OpportunityHubApplication instance;
    private AppDatabase database;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // Initialize database
        database = AppDatabase.getInstance(this);
    }
    
    public static OpportunityHubApplication getInstance() {
        return instance;
    }
    
    public AppDatabase getDatabase() {
        return database;
    }
}
