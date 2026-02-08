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
    
    public void clearAll() {
        preferences.edit().clear().apply();
    }
}
