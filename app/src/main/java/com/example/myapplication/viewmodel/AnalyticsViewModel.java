package com.example.myapplication.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.repository.ApplicationRepository;
import com.example.myapplication.repository.InterviewRepository;

public class AnalyticsViewModel extends AndroidViewModel {
    
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    
    private final MutableLiveData<Integer> totalApplications = new MutableLiveData<>();
    private final MutableLiveData<Integer> interviewsScheduled = new MutableLiveData<>();
    private final MutableLiveData<Integer> offersReceived = new MutableLiveData<>();
    private final MutableLiveData<Double> successRate = new MutableLiveData<>();
    private final MutableLiveData<Double> interviewReadiness = new MutableLiveData<>();
    private final MutableLiveData<Integer> practiceAttempts = new MutableLiveData<>();
    private final MutableLiveData<Double> averageInterviewScore = new MutableLiveData<>();
    
    private static final int DEFAULT_USER_ID = 1; // Mock user ID
    
    public AnalyticsViewModel(@NonNull Application application) {
        super(application);
        applicationRepository = new ApplicationRepository(application.getApplicationContext());
        interviewRepository = new InterviewRepository(application.getApplicationContext());
    }
    
    // Load all analytics
    public void loadAnalytics() {
        loadApplicationStats();
        loadInterviewStats();
    }
    
    // Load application statistics
    private void loadApplicationStats() {
        applicationRepository.getTotalApplications(DEFAULT_USER_ID, count -> {
            totalApplications.postValue(count);
        });
        
        applicationRepository.getApplicationCountByStatus(DEFAULT_USER_ID, "interview", count -> {
            interviewsScheduled.postValue(count);
        });
        
        applicationRepository.getApplicationCountByStatus(DEFAULT_USER_ID, "accepted", count -> {
            offersReceived.postValue(count);
            calculateSuccessRate();
        });
    }
    
    // Load interview statistics
    private void loadInterviewStats() {
        interviewRepository.getAverageScore(DEFAULT_USER_ID, score -> {
            averageInterviewScore.postValue(score);
            // Calculate readiness (0-100)
            double readiness = Math.min(100, (score / 100.0) * 120);
            interviewReadiness.postValue(readiness);
        });
        
        interviewRepository.getTotalAttempts(DEFAULT_USER_ID, attempts -> {
            practiceAttempts.postValue(attempts);
        });
    }
    
    // Calculate success rate
    private void calculateSuccessRate() {
        Integer total = totalApplications.getValue();
        Integer offers = offersReceived.getValue();
        
        if (total != null && offers != null && total > 0) {
            double rate = (offers * 100.0) / total;
            successRate.postValue(rate);
        } else {
            successRate.postValue(0.0);
        }
    }
    
    // Getters for LiveData
    public LiveData<Integer> getTotalApplications() {
        return totalApplications;
    }
    
    public LiveData<Integer> getInterviewsScheduled() {
        return interviewsScheduled;
    }
    
    public LiveData<Integer> getOffersReceived() {
        return offersReceived;
    }
    
    public LiveData<Double> getSuccessRate() {
        return successRate;
    }
    
    public LiveData<Double> getInterviewReadiness() {
        return interviewReadiness;
    }
    
    public LiveData<Integer> getPracticeAttempts() {
        return practiceAttempts;
    }
    
    public LiveData<Double> getAverageInterviewScore() {
        return averageInterviewScore;
    }
}
