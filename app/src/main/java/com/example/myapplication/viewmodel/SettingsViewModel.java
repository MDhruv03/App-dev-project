package com.example.myapplication.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.model.AppSettings;
import com.example.myapplication.util.SettingsManager;

/**
 * ViewModel for app settings
 */
public class SettingsViewModel extends AndroidViewModel {
    
    private final SettingsManager settingsManager;
    private final MutableLiveData<AppSettings> settings = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> success = new MutableLiveData<>();
    
    public SettingsViewModel(@NonNull Application application) {
        super(application);
        this.settingsManager = SettingsManager.getInstance(application);
        loadSettings();
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getError() {
        return error;
    }
    
    public LiveData<String> getSuccess() {
        return success;
    }
    
    protected void setLoading(boolean loading) {
        isLoading.postValue(loading);
    }
    
    protected void setError(String message) {
        error.postValue(message);
        setLoading(false);
    }
    
    protected void setSuccess(String message) {
        success.postValue(message);
        setLoading(false);
    }
    
    public LiveData<AppSettings> getSettings() {
        return settings;
    }
    
    private void loadSettings() {
        AppSettings appSettings = settingsManager.getSettings();
        settings.postValue(appSettings);
    }
    
    public void updateNotificationsEnabled(boolean enabled) {
        AppSettings appSettings = settings.getValue();
        if (appSettings != null) {
            appSettings.setNotificationsEnabled(enabled);
            settingsManager.saveSettings(appSettings);
            settings.postValue(appSettings);
        }
    }
    
    public void updateDeadlineReminders(boolean enabled) {
        AppSettings appSettings = settings.getValue();
        if (appSettings != null) {
            appSettings.setDeadlineReminders(enabled);
            settingsManager.saveSettings(appSettings);
            settings.postValue(appSettings);
        }
    }
    
    public void updateInterviewReminders(boolean enabled) {
        AppSettings appSettings = settings.getValue();
        if (appSettings != null) {
            appSettings.setInterviewReminders(enabled);
            settingsManager.saveSettings(appSettings);
            settings.postValue(appSettings);
        }
    }
    
    public void updateDarkMode(boolean enabled) {
        AppSettings appSettings = settings.getValue();
        if (appSettings != null) {
            appSettings.setDarkModeEnabled(enabled);
            settingsManager.saveSettings(appSettings);
            settings.postValue(appSettings);
            setSuccess("Theme updated");
        }
    }
    
    public void updateAutoSync(boolean enabled) {
        AppSettings appSettings = settings.getValue();
        if (appSettings != null) {
            appSettings.setAutoSync(enabled);
            settingsManager.saveSettings(appSettings);
            settings.postValue(appSettings);
        }
    }
    
    public void resetSettings() {
        settingsManager.clearSettings();
        loadSettings();
        setSuccess("Settings reset to defaults");
    }
}
