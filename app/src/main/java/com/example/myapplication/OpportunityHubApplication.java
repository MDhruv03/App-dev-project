package com.example.myapplication;

import android.app.Application;
import android.app.Activity;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.network.MockApiService;
import com.example.myapplication.worker.DeadlineReminderWorker;
import com.example.myapplication.worker.NotificationWorker;
import com.example.myapplication.worker.SyncWorker;
import com.example.myapplication.util.AnalyticsTracker;
import com.example.myapplication.util.CacheManager;
import com.example.myapplication.util.CrashHandler;
import com.example.myapplication.util.DiagnosticLogger;
import com.example.myapplication.util.Logger;
import com.example.myapplication.util.NotificationHelper;
import com.example.myapplication.util.PreferencesManager;
import com.example.myapplication.util.SettingsManager;
import com.example.myapplication.util.TaskManager;
import com.example.myapplication.util.ThemeManager;
import com.example.myapplication.repository.ApplicationRepository;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

public class OpportunityHubApplication extends Application {
    
    private static OpportunityHubApplication instance;
    private static final String KEY_DARK_MODE = "dark_mode";
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        DiagnosticLogger.init(this);
        DiagnosticLogger.log("application_onCreate_start");

        PreferencesManager preferencesManager = new PreferencesManager(this);
        boolean darkModeEnabled = preferencesManager.getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
            darkModeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        
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
        safeInit("workmanager-sync", this::schedulePeriodicSyncWork);
        safeInit("workmanager-daily-recommendations", this::scheduleDailyRecommendationsWork);
        safeInit("workmanager-deadline-reminders", this::scheduleDeadlineRemindersWork);
        safeInit("sync-scheduler", this::initializeSyncScheduler);
        
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

    private void initializeSyncScheduler() {
        ApplicationRepository applicationRepository = new ApplicationRepository(this);
        TaskManager.getInstance().scheduleWithFixedDelay(
            applicationRepository::syncPendingOperations,
            20,
            60,
            TimeUnit.SECONDS
        );
        Logger.d("App", "Background sync scheduler initialized");
    }

        private void schedulePeriodicSyncWork() {
        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
            SyncWorker.class,
            15,
            TimeUnit.MINUTES
        )
            .addTag("background_sync")
            .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "background_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        );
        Logger.d("App", "WorkManager background sync scheduled");
        }

    private void scheduleDailyRecommendationsWork() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
            NotificationWorker.class,
            24,
            TimeUnit.HOURS
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_recommendations",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        );
        Logger.d("App", "Daily recommendations work scheduled");
    }

    private void scheduleDeadlineRemindersWork() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
            DeadlineReminderWorker.class,
            24,
            TimeUnit.HOURS
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "deadline_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        );
        Logger.d("App", "Deadline reminders work scheduled");
    }
    
    private void initializeDatabase() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);

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
