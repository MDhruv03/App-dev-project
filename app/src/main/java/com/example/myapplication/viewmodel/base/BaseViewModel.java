package com.example.myapplication.viewmodel.base;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.util.Logger;

/**
 * Base ViewModel with common functionality
 */
public abstract class BaseViewModel extends ViewModel {
    
    private static final String TAG = "BaseViewModel";
    
    protected final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    protected final MutableLiveData<String> error = new MutableLiveData<>();
    protected final MutableLiveData<String> success = new MutableLiveData<>();
    
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
        Logger.e(TAG, "Error: " + message);
        error.postValue(message);
        setLoading(false);
    }
    
    protected void setSuccess(String message) {
        Logger.d(TAG, "Success: " + message);
        success.postValue(message);
        setLoading(false);
    }
    
    protected void handleError(Throwable throwable) {
        Logger.e(TAG, "Error occurred", throwable);
        setError(throwable.getMessage() != null ? throwable.getMessage() : "An error occurred");
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        Logger.d(TAG, getClass().getSimpleName() + " cleared");
    }
}
