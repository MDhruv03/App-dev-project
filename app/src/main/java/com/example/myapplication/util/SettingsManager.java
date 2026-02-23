package com.example.myapplication.util;

import android.content.Context;
import com.example.myapplication.model.AppSettings;
import com.google.gson.Gson;

/**
 * Settings manager for app-wide settings
 */
public class SettingsManager {
    
    private static final String SETTINGS_KEY = "app_settings";
    private static SettingsManager instance;
    
    private final PreferencesManager preferencesManager;
    private final Gson gson;
    private AppSettings settings;
    
    private SettingsManager(Context context) {
        this.preferencesManager = new PreferencesManager(context);
        this.gson = new Gson();
        loadSettings();
    }
    
    public static synchronized SettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Load settings from preferences
     */
    private void loadSettings() {
        String json = preferencesManager.getString(SETTINGS_KEY, null);
        if (json != null) {
            settings = gson.fromJson(json, AppSettings.class);
        } else {
            settings = new AppSettings();
        }
    }
    
    /**
     * Save settings to preferences
     */
    public void saveSettings() {
        String json = gson.toJson(settings);
        preferencesManager.putString(SETTINGS_KEY, json);
    }
    
    /**
     * Save specific settings object to preferences
     */
    public void saveSettings(AppSettings appSettings) {
        this.settings = appSettings;
        saveSettings();
    }
    
    /**
     * Get current settings
     */
    public AppSettings getSettings() {
        return settings;
    }
    
    /**
     * Update settings
     */
    public void updateSettings(AppSettings newSettings) {
        this.settings = newSettings;
        saveSettings();
    }
    
    /**
     * Reset to default settings
     */
    public void resetToDefaults() {
        this.settings = new AppSettings();
        saveSettings();
    }
    
    // Convenience methods for common settings
    
    public boolean areNotificationsEnabled() {
        return settings.isNotificationsEnabled();
    }
    
    public void setNotificationsEnabled(boolean enabled) {
        settings.setNotificationsEnabled(enabled);
        saveSettings();
    }
    
    public boolean isDarkModeEnabled() {
        return settings.isDarkModeEnabled();
    }
    
    public void setDarkModeEnabled(boolean enabled) {
        settings.setDarkModeEnabled(enabled);
        saveSettings();
    }
    
    public String getDefaultSortBy() {
        return settings.getDefaultSortBy();
    }
    
    public void setDefaultSortBy(String sortBy) {
        settings.setDefaultSortBy(sortBy);
        saveSettings();
    }
    
    public boolean isFirstLaunch() {
        return preferencesManager.getBoolean("first_launch", true);
    }
    
    public void setFirstLaunch(boolean isFirst) {
        preferencesManager.putBoolean("first_launch", isFirst);
    }
    
    public void clearSettings() {
        resetToDefaults();
    }
}
