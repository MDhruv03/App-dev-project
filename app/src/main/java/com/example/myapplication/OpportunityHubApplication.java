package com.example.myapplication;

import android.app.Application;
import android.app.Activity;
import android.os.Bundle;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.model.Opportunity;
import com.example.myapplication.network.MockApiService;
import com.example.myapplication.util.AnalyticsTracker;
import com.example.myapplication.util.CacheManager;
import com.example.myapplication.util.CrashHandler;
import com.example.myapplication.util.DiagnosticLogger;
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

        DiagnosticLogger.init(this);
        DiagnosticLogger.log("application_onCreate_start");
        
        // Initialize crash handler
        CrashHandler.install(this);
        Logger.d("App", "OpportunityHub Application Starting...");
        DiagnosticLogger.log("crash_handler_installed");
        
        // Initialize core components
        safeInit("logger", this::initializeLogger);
        safeInit("theme", this::initializeTheme);
        safeInit("cache", this::initializeCacheManager);
        safeInit("analytics", this::initializeAnalytics);
        safeInit("notifications", this::initializeNotifications);
        safeInit("settings", this::initializeSettings);
        
        // Initialize Mock API Service
        safeInit("mock-api", MockApiService::getInstance);
        
        // Initialize database with sample data
        safeInit("database", this::initializeDatabase);

        registerLifecycleDiagnostics();
        DiagnosticLogger.log("application_onCreate_complete");
    }

    private void safeInit(String name, Runnable initBlock) {
        try {
            DiagnosticLogger.log("init_start:" + name);
            initBlock.run();
            DiagnosticLogger.log("init_success:" + name);
        } catch (Exception e) {
            Logger.e("App", "Startup init failed: " + name, e);
            DiagnosticLogger.logError("init_failure:" + name, e);
        }
    }

    private void registerLifecycleDiagnostics() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                DiagnosticLogger.log("activity_created:" + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityStarted(Activity activity) {
                DiagnosticLogger.log("activity_started:" + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityResumed(Activity activity) {
                DiagnosticLogger.log("activity_resumed:" + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }
    
    /**
     * Initialize logger with debug level
     */
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
        NotificationHelper.createNotificationChannels(this);
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
                
                // Check if opportunity database is empty
                List<Opportunity> existing = db.opportunityDao().getAllOpportunities();
                
                if (existing == null || existing.isEmpty()) {
                    Logger.d("App", "Database empty, generating sample data...");
                    
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
                
                // Initialize interview questions if empty
                List<com.example.myapplication.model.InterviewQuestion> existingQuestions = 
                    db.interviewQuestionDao().getQuestionsByDomain("SDE");
                
                if (existingQuestions == null || existingQuestions.isEmpty()) {
                    Logger.d("App", "Populating interview questions...");
                    
                    // Generate questions for all domains
                    String[] domains = {"SDE", "ML", "Web", "Android", "HR"};
                    int questionCount = 0;
                    
                    for (String domain : domains) {
                        List<com.example.myapplication.model.InterviewQuestion> questions = 
                            com.example.myapplication.util.InterviewDataGenerator.generateQuestionsForDomain(domain);
                        for (com.example.myapplication.model.InterviewQuestion q : questions) {
                            db.interviewQuestionDao().insert(q);
                            questionCount++;
                        }
                    }
                    
                    Logger.d("App", "Inserted " + questionCount + " interview questions");
                } else {
                    Logger.d("App", "Interview questions already populated");
                }
            } catch (Exception e) {
                Logger.e("App", "Error initializing database", e);
            }
        });
    }
    
    public static OpportunityHubApplication getInstance() {
        return instance;
    }
}
