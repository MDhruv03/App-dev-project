package com.example.myapplication.network;

import com.example.myapplication.model.Application;
import com.example.myapplication.model.InterviewQuestion;
import com.example.myapplication.model.Opportunity;
import com.example.myapplication.util.InterviewDataGenerator;
import com.example.myapplication.util.SampleDataGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MockApiService {
    
    private static MockApiService instance;
    private List<Opportunity> cachedOpportunities;
    private List<Application> cachedApplications;
    private List<InterviewQuestion> cachedQuestions;
    
    private MockApiService() {
        initializeCache();
    }
    
    public static synchronized MockApiService getInstance() {
        if (instance == null) {
            instance = new MockApiService();
        }
        return instance;
    }
    
    private void initializeCache() {
        cachedOpportunities = SampleDataGenerator.generateOpportunities(50);
        cachedApplications = new ArrayList<>();
        cachedQuestions = new ArrayList<>();
        
        // Initialize questions for all domains
        cachedQuestions.addAll(InterviewDataGenerator.generateQuestionsForDomain("SDE"));
        cachedQuestions.addAll(InterviewDataGenerator.generateQuestionsForDomain("ML"));
        cachedQuestions.addAll(InterviewDataGenerator.generateQuestionsForDomain("Web"));
        cachedQuestions.addAll(InterviewDataGenerator.generateQuestionsForDomain("Android"));
        cachedQuestions.addAll(InterviewDataGenerator.generateQuestionsForDomain("HR"));
    }
    
    // Opportunities API
    public void fetchOpportunities(ApiCallback<List<Opportunity>> callback) {
        simulateNetworkDelay(() -> callback.onSuccess(new ArrayList<>(cachedOpportunities)));
    }
    
    public void fetchRecommendedOpportunities(ApiCallback<List<Opportunity>> callback) {
        simulateNetworkDelay(() -> {
            List<Opportunity> recommended = cachedOpportunities.stream()
                .filter(o -> o.getRecommendationScore() >= 70)
                .limit(10)
                .collect(Collectors.toList());
            callback.onSuccess(recommended);
        });
    }
    
    public void fetchSavedOpportunities(ApiCallback<List<Opportunity>> callback) {
        simulateNetworkDelay(() -> {
            List<Opportunity> saved = cachedOpportunities.stream()
                .filter(Opportunity::isSaved)
                .collect(Collectors.toList());
            callback.onSuccess(saved);
        });
    }
    
    public void updateOpportunitySaveStatus(int id, boolean saved, ApiCallback<Boolean> callback) {
        simulateNetworkDelay(() -> {
            cachedOpportunities.stream()
                .filter(o -> o.getId() == id)
                .findFirst()
                .ifPresent(o -> o.setSaved(saved));
            callback.onSuccess(true);
        });
    }
    
    public void updateOpportunityApplyStatus(int id, boolean applied, ApiCallback<Boolean> callback) {
        simulateNetworkDelay(() -> {
            cachedOpportunities.stream()
                .filter(o -> o.getId() == id)
                .findFirst()
                .ifPresent(o -> o.setApplied(applied));
            callback.onSuccess(true);
        });
    }
    
    // Applications API
    public void fetchApplications(int userId, ApiCallback<List<Application>> callback) {
        simulateNetworkDelay(() -> callback.onSuccess(new ArrayList<>(cachedApplications)));
    }
    
    public void addApplication(Application application, ApiCallback<Application> callback) {
        simulateNetworkDelay(() -> {
            application.setId(cachedApplications.size() + 1);
            cachedApplications.add(application);
            callback.onSuccess(application);
        });
    }
    
    public void updateApplication(Application application, ApiCallback<Boolean> callback) {
        simulateNetworkDelay(() -> {
            for (int i = 0; i < cachedApplications.size(); i++) {
                if (cachedApplications.get(i).getId() == application.getId()) {
                    cachedApplications.set(i, application);
                    callback.onSuccess(true);
                    return;
                }
            }
            callback.onSuccess(false);
        });
    }
    
    public void deleteApplication(int id, ApiCallback<Boolean> callback) {
        simulateNetworkDelay(() -> {
            boolean removed = cachedApplications.removeIf(app -> app.getId() == id);
            callback.onSuccess(removed);
        });
    }
    
    // Interview Questions API
    public void fetchQuestionsByDomain(String domain, ApiCallback<List<InterviewQuestion>> callback) {
        simulateNetworkDelay(() -> {
            List<InterviewQuestion> filtered = cachedQuestions.stream()
                .filter(q -> q.getDomain().equalsIgnoreCase(domain))
                .collect(Collectors.toList());
            callback.onSuccess(filtered);
        });
    }
    
    public void fetchAllQuestions(ApiCallback<List<InterviewQuestion>> callback) {
        simulateNetworkDelay(() -> callback.onSuccess(new ArrayList<>(cachedQuestions)));
    }
    
    // Analytics API
    public void fetchUserStats(int userId, ApiCallback<UserStats> callback) {
        simulateNetworkDelay(() -> {
            UserStats stats = new UserStats();
            stats.totalApplications = cachedApplications.size();
            stats.interviewsScheduled = (int) cachedApplications.stream()
                .filter(a -> "interview".equals(a.getStatus()))
                .count();
            stats.offersReceived = (int) cachedApplications.stream()
                .filter(a -> "accepted".equals(a.getStatus()))
                .count();
            stats.successRate = stats.totalApplications > 0 
                ? (stats.offersReceived * 100.0) / stats.totalApplications 
                : 0.0;
            callback.onSuccess(stats);
        });
    }
    
    // Utility methods
    private void simulateNetworkDelay(Runnable task) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Thread.sleep(300 + (long)(Math.random() * 500)); // 300-800ms delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            task.run();
        });
    }
    
    // Callback interface
    public interface ApiCallback<T> {
        void onSuccess(T result);
        default void onError(String error) {
            // Default error handling
        }
    }
    
    // Stats model
    public static class UserStats {
        public int totalApplications;
        public int interviewsScheduled;
        public int offersReceived;
        public double successRate;
    }
}
