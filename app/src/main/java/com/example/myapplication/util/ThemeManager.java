package com.example.myapplication.util;

import android.content.Context;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Theme manager for handling dark mode and UI themes
 */
public class ThemeManager {
    
    private static ThemeManager instance;
    private final SettingsManager settingsManager;
    
    private ThemeManager(Context context) {
        settingsManager = SettingsManager.getInstance(context);
    }
    
    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Apply theme based on settings
     */
    public void applyTheme() {
        boolean darkModeEnabled = settingsManager.getSettings().isDarkModeEnabled();
        
        if (darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    
    /**
     * Apply system theme
     */
    public void applySystemTheme() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
    
    /**
     * Enable dark mode
     */
    public void enableDarkMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        settingsManager.getSettings().setDarkModeEnabled(true);
        settingsManager.saveSettings(settingsManager.getSettings());
    }
    
    /**
     * Disable dark mode
     */
    public void disableDarkMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        settingsManager.getSettings().setDarkModeEnabled(false);
        settingsManager.saveSettings(settingsManager.getSettings());
    }
    
    /**
     * Toggle dark mode
     */
    public void toggleDarkMode() {
        if (isDarkModeEnabled()) {
            disableDarkMode();
        } else {
            enableDarkMode();
        }
    }
    
    /**
     * Check if dark mode is enabled
     */
    public boolean isDarkModeEnabled() {
        return settingsManager.getSettings().isDarkModeEnabled();
    }
    
    /**
     * Check if system is in dark mode
     */
    public boolean isSystemInDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode 
            & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}
