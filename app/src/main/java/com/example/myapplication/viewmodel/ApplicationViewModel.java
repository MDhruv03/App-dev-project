package com.example.myapplication.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.repository.ApplicationRepository;

import java.util.List;
import java.util.stream.Collectors;

public class ApplicationViewModel extends AndroidViewModel {
    
    private final ApplicationRepository repository;
    private final MutableLiveData<List<com.example.myapplication.model.Application>> allApplications = new MutableLiveData<>();
    private final MutableLiveData<List<com.example.myapplication.model.Application>> filteredApplications = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> appliedCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> interviewCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> acceptedCount = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    
    private static final int DEFAULT_USER_ID = 1; // Mock user ID
    
    public ApplicationViewModel(@NonNull Application application) {
        super(application);
        repository = new ApplicationRepository(application.getApplicationContext());
    }
    
    // Load all applications for current user
    public void loadAllApplications() {
        isLoading.setValue(true);
        repository.getAllApplications(DEFAULT_USER_ID, applications -> {
            isLoading.postValue(false);
            allApplications.postValue(applications);
            filteredApplications.postValue(applications);
            updateCounts(applications);
        });
    }
    
    // Filter by status
    public void filterByStatus(String status) {
        if (status == null || status.equals("all")) {
            filteredApplications.setValue(allApplications.getValue());
        } else {
            repository.getApplicationsByStatus(DEFAULT_USER_ID, status, applications -> {
                filteredApplications.postValue(applications);
            });
        }
    }
    
    // Add new application
    public void addApplication(com.example.myapplication.model.Application application) {
        application.setUserId(DEFAULT_USER_ID);
        repository.insert(application, success -> {
            if (success) {
                loadAllApplications();
            }
        });
    }
    
    // Update application
    public void updateApplication(com.example.myapplication.model.Application application) {
        repository.update(application, success -> {
            if (success) {
                loadAllApplications();
            }
        });
    }
    
    // Delete application
    public void deleteApplication(com.example.myapplication.model.Application application) {
        repository.delete(application, success -> {
            if (success) {
                loadAllApplications();
            }
        });
    }
    
    // Update counts
    private void updateCounts(List<com.example.myapplication.model.Application> applications) {
        if (applications == null) return;
        
        totalCount.postValue(applications.size());
        appliedCount.postValue((int) applications.stream()
                .filter(a -> a.getStatus().equals("applied")).count());
        interviewCount.postValue((int) applications.stream()
                .filter(a -> a.getStatus().equals("interview")).count());
        acceptedCount.postValue((int) applications.stream()
                .filter(a -> a.getStatus().equals("accepted")).count());
    }
    
    // Getters for LiveData
    public LiveData<List<com.example.myapplication.model.Application>> getAllApplications() {
        return allApplications;
    }
    
    public LiveData<List<com.example.myapplication.model.Application>> getFilteredApplications() {
        return filteredApplications;
    }
    
    public LiveData<Integer> getTotalCount() {
        return totalCount;
    }
    
    public LiveData<Integer> getAppliedCount() {
        return appliedCount;
    }
    
    public LiveData<Integer> getInterviewCount() {
        return interviewCount;
    }
    
    public LiveData<Integer> getAcceptedCount() {
        return acceptedCount;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
}
