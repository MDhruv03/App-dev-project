package com.example.myapplication;

import android.app.Application;

public class OpportunityHubApplication extends Application {
    
    private static OpportunityHubApplication instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
    
    public static OpportunityHubApplication getInstance() {
        return instance;
    }
}
