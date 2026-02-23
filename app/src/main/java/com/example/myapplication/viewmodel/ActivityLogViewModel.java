package com.example.myapplication.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.database.ActivityLogDao;
import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.model.ActivityLog;
import com.example.myapplication.util.Logger;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for activity tracking
 */
public class ActivityLogViewModel extends AndroidViewModel {
    
    private static final String TAG = "ActivityLogViewModel";
    
    private final ActivityLogDao activityLogDao;
    private final ExecutorService executorService;
    private final LiveData<List<ActivityLog>> allLogs;
    
    public ActivityLogViewModel(@NonNull Application application) {
        super(application);
        
        AppDatabase database = AppDatabase.getInstance(application);
        activityLogDao = database.activityLogDao();
        executorService = Executors.newSingleThreadExecutor();
        
        allLogs = activityLogDao.getAllLogs();
    }
    
    public LiveData<List<ActivityLog>> getAllLogs() {
        return allLogs;
    }
    
    public LiveData<List<ActivityLog>> getLogsByType(String activityType) {
        return activityLogDao.getLogsByType(activityType);
    }
    
    public LiveData<List<ActivityLog>> getRecentLogs(int limit) {
        return activityLogDao.getRecentLogs(limit);
    }
    
    public void logActivity(String activityType, String description, Long relatedOpportunityId) {
        executorService.execute(() -> {
            try {
                ActivityLog log = new ActivityLog();
                log.setActivityType(activityType);
                log.setDescription(description);
                log.setTimestamp(new Date());
                log.setRelatedOpportunityId(relatedOpportunityId);
                
                activityLogDao.insert(log);
                Logger.d(TAG, "Activity logged: " + activityType);
            } catch (Exception e) {
                Logger.e(TAG, "Error logging activity", e);
            }
        });
    }
    
    public void deleteOldLogs(Date beforeDate) {
        executorService.execute(() -> {
            try {
                activityLogDao.deleteOldLogs(beforeDate);
                Logger.d(TAG, "Old logs deleted");
            } catch (Exception e) {
                Logger.e(TAG, "Error deleting old logs", e);
            }
        });
    }
    
    public void clearAllLogs() {
        executorService.execute(() -> {
            try {
                activityLogDao.deleteAll();
                Logger.d(TAG, "All logs cleared");
            } catch (Exception e) {
                Logger.e(TAG, "Error clearing logs", e);
            }
        });
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
