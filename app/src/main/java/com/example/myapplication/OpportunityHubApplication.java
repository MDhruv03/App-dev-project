package com.example.myapplication;

import android.app.Application;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.model.Opportunity;
import com.example.myapplication.network.MockApiService;
import com.example.myapplication.util.AnalyticsTracker;
import com.example.myapplication.util.CacheManager;
import com.example.myapplication.util.CrashHandler;
import com.example.myapplication.util.Logger;
import com.example.myapplication.util.NotificationHelper;
import com.example.myapplication.util.SampleDataGenerator;
import com.example.myapplication.util.SettingsManager;
import com.example.myapplication.util.ThemeManager;

import java.util.List;
import java.util.concurrent.Executors;

public class OpportunityHubApplication extends Application {
    
    private static OpportunityHubApplication instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // Initialize crash handler
        CrashHandler.install(this);
        Logger.d("App", "OpportunityHub Application Starting...");
        
        // Initialize core components
        initializeLogger();
        initializeTheme();
        initializeCacheManager();
        initializeAnalytics();
        initializeNotifications();
        initializeSettings();
        
        // Initialize Mock API Service
        MockApiService.getInstance();
        
        // Initialize database with sample data
        initializeDaLogger.d("App", "Database empty, generating sample data...");
                    
                    // Populate with comprehensive sample data (120+ opportunities)
                    List<Opportunity> opportunities = SampleDataGenerator.generateOpportunities(120);
                    
                    int count = 0;
                    for (Opportunity opp : opportunities) {
                        db.opportunityDao().insert(opp);
                        count++;
                    }
                    
                    Logger.d("App", "Inserted " + count + " opportunities into database");
                } else {
                    Logger.d("App", "Database already populated with " + existing.size() + " opportunities");
                }
            } catch (Exception e) {
                Logger.e("App", "Error initializing database", e
    private void initializeLogger() {
        Logger.setLogLevel(Logger.LogLevel.DEBUG);
        Logger.d("App", "Logger initialized");
    }
    
    /**
     * Initialize theme manager
     */
    private void initializeTheme() {
        ThemeManager themeManager = ThemeManager.getInstance(this);
        themeManager.applyTheme();
        Logger.d("App", "Theme initialized");
    }
    
    /**
     * Initialize cache manager
     */
    private void initializeCacheManager() {
        CacheManager.getInstance();
        Logger.d("App", "Cache manager initialized");
    }
    
    /**
     * Initialize analytics tracker
     */
    private void initializeAnalytics() {
        AnalyticsTracker.getInstance(this);
        Logger.d("App", "Analytics tracker initialized");
    }
    
    /**
     * Initialize notification channels
     */
    private void initializeNotifications() {
        NotificationHelper notificationHelper = new NotificationHelper(this);
        notificationHelper.createNotificationChannels();
        Logger.d("App", "Notification channels created");
    }
    
    /**
     * Initialize settings
     */
    private void initializeSettings() {
        SettingsManager settingsManager = SettingsManager.getInstance(this);
        
        // Check if first launch
        if (settingsManager.isFirstLaunch()) {
            Logger.d("App", "First launch detected");
            settingsManager.setFirstLaunch(false);
        }
        
        Logger.d("App", "Settings initialized");
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
