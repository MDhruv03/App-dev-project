package com.example.myapplication.repository;

import android.content.Context;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.database.ApplicationDao;
import com.example.myapplication.model.Application;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApplicationRepository {
    
    private final ApplicationDao applicationDao;
    private final ExecutorService executorService;
    
    public ApplicationRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        applicationDao = database.applicationDao();
        executorService = Executors.newSingleThreadExecutor();
    }
    
    // Insert application
    public void insert(Application application, OnOperationCompleteListener listener) {
        executorService.execute(() -> {
            long id = applicationDao.insert(application);
            if (listener != null) {
                listener.onComplete(id > 0);
            }
        });
    }
    
    // Update application
    public void update(Application application, OnOperationCompleteListener listener) {
        executorService.execute(() -> {
            applicationDao.update(application);
            if (listener != null) {
                listener.onComplete(true);
            }
        });
    }
    
    // Delete application
    public void delete(Application application, OnOperationCompleteListener listener) {
        executorService.execute(() -> {
            applicationDao.delete(application);
            if (listener != null) {
                listener.onComplete(true);
            }
        });
    }
    
    // Get all applications
    public void getAllApplications(int userId, OnApplicationsLoadedListener listener) {
        executorService.execute(() -> {
            List<Application> applications = applicationDao.getAllApplications(userId);
            if (listener != null) {
                listener.onLoaded(applications);
            }
        });
    }
    
    // Get applications by status
    public void getApplicationsByStatus(int userId, String status, OnApplicationsLoadedListener listener) {
        executorService.execute(() -> {
            List<Application> applications = applicationDao.getApplicationsByStatus(userId, status);
            if (listener != null) {
                listener.onLoaded(applications);
            }
        });
    }
    
    // Get total applications count
    public void getTotalApplications(int userId, OnCountLoadedListener listener) {
        executorService.execute(() -> {
            int count = applicationDao.getTotalApplications(userId);
            if (listener != null) {
                listener.onLoaded(count);
            }
        });
    }
    
    // Get count by status
    public void getApplicationCountByStatus(int userId, String status, OnCountLoadedListener listener) {
        executorService.execute(() -> {
            int count = applicationDao.getApplicationCountByStatus(userId, status);
            if (listener != null) {
                listener.onLoaded(count);
            }
        });
    }
    
    // Interfaces for callbacks
    public interface OnApplicationsLoadedListener {
        void onLoaded(List<Application> applications);
    }
    
    public interface OnOperationCompleteListener {
        void onComplete(boolean success);
    }
    
    public interface OnCountLoadedListener {
        void onLoaded(int count);
    }
}
