package com.example.myapplication.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Task manager for background operations
 */
public class TaskManager {
    
    private static TaskManager instance;
    
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Handler mainHandler;
    
    private TaskManager() {
        executorService = Executors.newFixedThreadPool(4);
        scheduledExecutorService = Executors.newScheduledThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized TaskManager getInstance() {
        if (instance == null) {
            instance = new TaskManager();
        }
        return instance;
    }
    
    /**
     * Execute task on background thread
     */
    public void executeOnBackground(Runnable task) {
        executorService.execute(task);
    }
    
    /**
     * Execute task on main thread
     */
    public void executeOnMain(Runnable task) {
        mainHandler.post(task);
    }
    
    /**
     * Execute task on main thread with delay
     */
    public void executeOnMainDelayed(Runnable task, long delayMillis) {
        mainHandler.postDelayed(task, delayMillis);
    }
    
    /**
     * Execute task with result callback
     */
    public <T> void executeWithCallback(
            BackgroundTask<T> backgroundTask,
            Callback<T> callback) {
        
        executorService.execute(() -> {
            try {
                T result = backgroundTask.execute();
                
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
            }
        });
    }
    
    /**
     * Schedule task with fixed delay
     */
    public void scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        scheduledExecutorService.scheduleWithFixedDelay(task, initialDelay, delay, unit);
    }
    
    /**
     * Schedule task at fixed rate
     */
    public void scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        scheduledExecutorService.scheduleAtFixedRate(task, initialDelay, period, unit);
    }
    
    /**
     * Schedule one-time task
     */
    public void schedule(Runnable task, long delay, TimeUnit unit) {
        scheduledExecutorService.schedule(task, delay, unit);
    }
    
    /**
     * Shutdown task manager
     */
    public void shutdown() {
        executorService.shutdown();
        scheduledExecutorService.shutdown();
    }
    
    /**
     * Shutdown immediately
     */
    public void shutdownNow() {
        executorService.shutdownNow();
        scheduledExecutorService.shutdownNow();
    }
    
    /**
     * Background task interface
     */
    public interface BackgroundTask<T> {
        T execute() throws Exception;
    }
    
    /**
     * Callback interface
     */
    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
}
