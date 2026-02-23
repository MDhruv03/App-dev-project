package com.example.myapplication.util;

import android.content.Context;
import android.net.Uri;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.model.Application;
import com.example.myapplication.model.Opportunity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Backup and restore manager for app data
 */
public class BackupRestoreManager {
    
    private static final String TAG = "BackupRestoreManager";
    private static final String BACKUP_FILE_PREFIX = "opportunityhub_backup_";
    private static final String BACKUP_FILE_EXTENSION = ".json";
    
    private final Context context;
    private final AppDatabase database;
    private final Gson gson;
    private final ExecutorService executorService;
    
    public interface BackupListener {
        void onBackupSuccess(String filePath);
        void onBackupError(String error);
    }
    
    public interface RestoreListener {
        void onRestoreSuccess(int opportunitiesRestored, int applicationsRestored);
        void onRestoreError(String error);
    }
    
    private static class BackupData {
        List<Opportunity> opportunities;
        List<Application> applications;
        long backupTimestamp;
        String appVersion;
        
        BackupData(List<Opportunity> opportunities, List<Application> applications) {
            this.opportunities = opportunities;
            this.applications = applications;
            this.backupTimestamp = System.currentTimeMillis();
            this.appVersion = Constants.APP_VERSION;
        }
    }
    
    public BackupRestoreManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .setPrettyPrinting()
                .create();
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Create backup of all data
     */
    public void createBackup(BackupListener listener) {
        executorService.execute(() -> {
            try {
                // Fetch all data
                List<Opportunity> opportunities = database.opportunityDao().getAllOpportunitiesSync();
                List<Application> applications = database.applicationDao().getAllApplicationsSync();
                
                // Create backup data object
                BackupData backupData = new BackupData(opportunities, applications);
                
                // Convert to JSON
                String json = gson.toJson(backupData);
                
                // Create backup file
                String fileName = generateBackupFileName();
                File backupFile = new File(context.getExternalFilesDir(Constants.EXPORT_DIR), fileName);
                
                // Write to file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(backupFile))) {
                    writer.write(json);
                }
                
                Logger.d(TAG, "Backup created: " + backupFile.getAbsolutePath());
                
                if (listener != null) {
                    listener.onBackupSuccess(backupFile.getAbsolutePath());
                }
                
            } catch (Exception e) {
                Logger.e(TAG, "Backup failed", e);
                if (listener != null) {
                    listener.onBackupError(e.getMessage());
                }
            }
        });
    }
    
    /**
     * Create backup to specific URI (for SAF)
     */
    public void createBackupToUri(Uri uri, BackupListener listener) {
        executorService.execute(() -> {
            try {
                // Fetch all data
                List<Opportunity> opportunities = database.opportunityDao().getAllOpportunitiesSync();
                List<Application> applications = database.applicationDao().getAllApplicationsSync();
                
                // Create backup data object
                BackupData backupData = new BackupData(opportunities, applications);
                
                // Convert to JSON
                String json = gson.toJson(backupData);
                
                // Write to URI
                try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                    writer.write(json);
                }
                
                Logger.d(TAG, "Backup created to URI: " + uri.toString());
                
                if (listener != null) {
                    listener.onBackupSuccess(uri.toString());
                }
                
            } catch (Exception e) {
                Logger.e(TAG, "Backup to URI failed", e);
                if (listener != null) {
                    listener.onBackupError(e.getMessage());
                }
            }
        });
    }
    
    /**
     * Restore from backup file
     */
    public void restoreFromBackup(File backupFile, RestoreListener listener) {
        executorService.execute(() -> {
            try {
                // Read JSON from file
                StringBuilder jsonBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(backupFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonBuilder.append(line);
                    }
                }
                
                // Parse JSON
                BackupData backupData = gson.fromJson(jsonBuilder.toString(), BackupData.class);
                
                if (backupData == null) {
                    throw new IOException("Invalid backup file");
                }
                
                // Insert opportunities
                if (backupData.opportunities != null) {
                    for (Opportunity opportunity : backupData.opportunities) {
                        database.opportunityDao().insert(opportunity);
                    }
                }
                
                // Insert applications
                if (backupData.applications != null) {
                    for (Application application : backupData.applications) {
                        database.applicationDao().insert(application);
                    }
                }
                
                Logger.d(TAG, "Restore completed from: " + backupFile.getAbsolutePath());
                
                if (listener != null) {
                    listener.onRestoreSuccess(
                        backupData.opportunities != null ? backupData.opportunities.size() : 0,
                        backupData.applications != null ? backupData.applications.size() : 0
                    );
                }
                
            } catch (Exception e) {
                Logger.e(TAG, "Restore failed", e);
                if (listener != null) {
                    listener.onRestoreError(e.getMessage());
                }
            }
        });
    }
    
    /**
     * Restore from backup URI
     */
    public void restoreFromUri(Uri uri, RestoreListener listener) {
        executorService.execute(() -> {
            try {
                // Read JSON from URI
                StringBuilder jsonBuilder = new StringBuilder();
                try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonBuilder.append(line);
                    }
                }
                
                // Parse JSON
                BackupData backupData = gson.fromJson(jsonBuilder.toString(), BackupData.class);
                
                if (backupData == null) {
                    throw new IOException("Invalid backup file");
                }
                
                // Insert opportunities
                if (backupData.opportunities != null) {
                    for (Opportunity opportunity : backupData.opportunities) {
                        database.opportunityDao().insert(opportunity);
                    }
                }
                
                // Insert applications
                if (backupData.applications != null) {
                    for (Application application : backupData.applications) {
                        database.applicationDao().insert(application);
                    }
                }
                
                Logger.d(TAG, "Restore completed from URI: " + uri.toString());
                
                if (listener != null) {
                    listener.onRestoreSuccess(
                        backupData.opportunities != null ? backupData.opportunities.size() : 0,
                        backupData.applications != null ? backupData.applications.size() : 0
                    );
                }
                
            } catch (Exception e) {
                Logger.e(TAG, "Restore from URI failed", e);
                if (listener != null) {
                    listener.onRestoreError(e.getMessage());
                }
            }
        });
    }
    
    /**
     * Generate backup file name with timestamp
     */
    private String generateBackupFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String timestamp = sdf.format(new Date());
        return BACKUP_FILE_PREFIX + timestamp + BACKUP_FILE_EXTENSION;
    }
    
    /**
     * Get list of available backups
     */
    public File[] getAvailableBackups() {
        File backupDir = new File(context.getExternalFilesDir(Constants.EXPORT_DIR), "");
        if (!backupDir.exists()) {
            return new File[0];
        }
        
        File[] files = backupDir.listFiles((dir, name) -> 
            name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(BACKUP_FILE_EXTENSION)
        );
        
        return files != null ? files : new File[0];
    }
    
    /**
     * Delete backup file
     */
    public boolean deleteBackup(File backupFile) {
        return backupFile.delete();
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}
