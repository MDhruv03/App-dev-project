package com.example.myapplication.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.model.InterviewProgress;
import com.example.myapplication.model.InterviewQuestion;
import com.example.myapplication.repository.InterviewRepository;

import java.util.List;

public class InterviewViewModel extends AndroidViewModel {
    
    private final InterviewRepository repository;
    private final MutableLiveData<List<InterviewQuestion>> questions = new MutableLiveData<>();
    private final MutableLiveData<InterviewQuestion> currentQuestion = new MutableLiveData<>();
    private final MutableLiveData<List<InterviewProgress>> userProgress = new MutableLiveData<>();
    private final MutableLiveData<Double> averageScore = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalAttempts = new MutableLiveData<>();
    private final MutableLiveData<Double> readinessScore = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    
    private static final int DEFAULT_USER_ID = 1; // Mock user ID
    private int currentQuestionIndex = 0;
    
    public InterviewViewModel(@NonNull Application application) {
        super(application);
        repository = new InterviewRepository(application.getApplicationContext());
    }
    
    // Load questions by domain
    public void loadQuestionsByDomain(String domain) {
        isLoading.setValue(true);
        repository.getQuestionsByDomain(domain, questionList -> {
            isLoading.postValue(false);
            questions.postValue(questionList);
            if (questionList != null && !questionList.isEmpty()) {
                currentQuestionIndex = 0;
                currentQuestion.postValue(questionList.get(0));
            }
        });
    }
    
    // Load user progress
    public void loadUserProgress() {
        repository.getUserProgress(DEFAULT_USER_ID, progress -> {
            userProgress.postValue(progress);
        });
    }
    
    // Load user statistics
    public void loadUserStatistics() {
        repository.getAverageScore(DEFAULT_USER_ID, score -> {
            averageScore.postValue(score);
            // Calculate readiness score (0-100)
            double readiness = Math.min(100, (score / 100.0) * 120); // Slight boost to encourage
            readinessScore.postValue(readiness);
        });
        
        repository.getTotalAttempts(DEFAULT_USER_ID, attempts -> {
            totalAttempts.postValue(attempts);
        });
    }
    
    // Move to next question
    public boolean nextQuestion() {
        List<InterviewQuestion> questionList = questions.getValue();
        if (questionList == null || questionList.isEmpty()) return false;
        
        currentQuestionIndex++;
        if (currentQuestionIndex < questionList.size()) {
            currentQuestion.setValue(questionList.get(currentQuestionIndex));
            return true;
        }
        return false;
    }
    
    // Submit answer
    public void submitAnswer(String answer, OnAnswerEvaluatedListener listener) {
        InterviewQuestion question = currentQuestion.getValue();
        if (question == null) return;
        
        // Evaluate answer
        InterviewProgress progress = evaluateAnswer(question, answer);
        progress.setUserId(DEFAULT_USER_ID);
        progress.setQuestionId(question.getId());
        
        // Save to database
        repository.insertProgress(progress, success -> {
            if (success) {
                loadUserProgress();
                loadUserStatistics();
                if (listener != null) {
                    listener.onEvaluated(progress);
                }
            }
        });
    }
    
    // Evaluate answer (simple keyword matching)
    private InterviewProgress evaluateAnswer(InterviewQuestion question, String answer) {
        InterviewProgress progress = new InterviewProgress();
        progress.setUserAnswer(answer);
        
        List<String> keywords = question.getExpectedKeywords();
        if (keywords == null || keywords.isEmpty()) {
            progress.setScore(50.0); // Default score if no keywords
            progress.setFeedback("Your answer has been recorded. Keep practicing!");
            progress.setMatchedKeywords(0);
            progress.setTotalKeywords(0);
            return progress;
        }
        
        String lowerAnswer = answer.toLowerCase();
        int matchedCount = 0;
        
        for (String keyword : keywords) {
            if (lowerAnswer.contains(keyword.toLowerCase())) {
                matchedCount++;
            }
        }
        
        double score = (matchedCount * 100.0) / keywords.size();
        progress.setScore(score);
        progress.setMatchedKeywords(matchedCount);
        progress.setTotalKeywords(keywords.size());
        
        // Generate feedback
        String feedback;
        if (score >= 80) {
            feedback = "Excellent answer! You covered most key concepts. ";
        } else if (score >= 60) {
            feedback = "Good answer! You could improve by mentioning: ";
        } else if (score >= 40) {
            feedback = "Fair answer. Consider including these important points: ";
        } else {
            feedback = "You might want to review this topic. Key concepts to cover: ";
        }
        
        // Add missing keywords
        if (score < 100) {
            StringBuilder missing = new StringBuilder();
            for (String keyword : keywords) {
                if (!lowerAnswer.contains(keyword.toLowerCase())) {
                    if (missing.length() > 0) missing.append(", ");
                    missing.append(keyword);
                }
            }
            feedback += missing.toString();
        }
        
        progress.setFeedback(feedback);
        progress.setComplete(true);
        
        return progress;
    }
    
    // Reset interview
    public void resetInterview() {
        currentQuestionIndex = 0;
        List<InterviewQuestion> questionList = questions.getValue();
        if (questionList != null && !questionList.isEmpty()) {
            currentQuestion.setValue(questionList.get(0));
        }
    }
    
    // Getters for LiveData
    public LiveData<List<InterviewQuestion>> getQuestions() {
        return questions;
    }
    
    public LiveData<InterviewQuestion> getCurrentQuestion() {
        return currentQuestion;
    }
    
    public LiveData<List<InterviewProgress>> getUserProgress() {
        return userProgress;
    }
    
    public LiveData<Double> getAverageScore() {
        return averageScore;
    }
    
    public LiveData<Integer> getTotalAttempts() {
        return totalAttempts;
    }
    
    public LiveData<Double> getReadinessScore() {
        return readinessScore;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public int getCurrentQuestionNumber() {
        return currentQuestionIndex + 1;
    }
    
    public int getTotalQuestions() {
        List<InterviewQuestion> questionList = questions.getValue();
        return questionList == null ? 0 : questionList.size();
    }
    
    // Listener interface
    public interface OnAnswerEvaluatedListener {
        void onEvaluated(InterviewProgress progress);
    }
}
