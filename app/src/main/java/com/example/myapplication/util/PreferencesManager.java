package com.example.myapplication.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    
    private static final String PREF_NAME = "OpportunityHubPrefs";
    private static final String KEY_FIRST_RUN = "first_run";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    
    private final SharedPreferences preferences;
    
    public PreferencesManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public boolean isFirstRun() {
        return preferences.getBoolean(KEY_FIRST_RUN, true);
    }
    
    public void setFirstRun(boolean firstRun) {
        preferences.edit().putBoolean(KEY_FIRST_RUN, firstRun).apply();
    }
    
    public int getUserId() {
        return preferences.getInt(KEY_USER_ID, -1);
    }
    
    public void setUserId(int userId) {
        preferences.edit().putInt(KEY_USER_ID, userId).apply();
    }
    
    public String getUserName() {
        return preferences.getString(KEY_USER_NAME, "");
    }
    
    public void setUserName(String name) {
        preferences.edit().putString(KEY_USER_NAME, name).apply();
    }
    
    public String getUserEmail() {
        return preferences.getString(KEY_USER_EMAIL, "");
    }
    
    public void setUserEmail(String email) {
        preferences.edit().putString(KEY_USER_EMAIL, email).apply();
    }
    
    // Generic methods for any key-value pairs
    
    public String getString(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }
    
    public void putString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }
    
    public void putBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }
    
    public int getInt(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }
    
    public void putInt(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }
    
    public long getLong(String key, long defaultValue) {
        return preferences.getLong(key, defaultValue);
    }
    
    public void putLong(String key, long value) {
        preferences.edit().putLong(key, value).apply();
    }
    
    public void clearAll() {
        preferences.edit().clear().apply();
    }
}
