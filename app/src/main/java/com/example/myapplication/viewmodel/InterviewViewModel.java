package com.example.myapplication.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.model.InterviewProgress;
import com.example.myapplication.model.InterviewQuestion;
import com.example.myapplication.network.GroqApiClient;
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
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    private static final int DEFAULT_USER_ID = 1; // Mock user ID
    private int currentQuestionIndex = 0;
    
    public InterviewViewModel(@NonNull Application application) {
        super(application);
        repository = new InterviewRepository(application.getApplicationContext());
    }
    
    // Load questions by domain
    public void loadQuestionsByDomain(String domain) {
        isLoading.setValue(true);
        repository.getQuestionsByDomain(domain, new InterviewRepository.OnQuestionsLoadedListener() {
            @Override
            public void onLoaded(List<InterviewQuestion> questionList) {
                isLoading.postValue(false);
                questions.postValue(questionList);
                if (questionList != null && !questionList.isEmpty()) {
                    currentQuestionIndex = 0;
                    currentQuestion.postValue(questionList.get(0));
                }
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
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

        InterviewProgress localProgress = evaluateAnswerLocally(question, answer);
        localProgress.setUserId(DEFAULT_USER_ID);
        localProgress.setQuestionId(question.getId());

        GroqApiClient.evaluateAnswer(
            question.getQuestion(),
            answer,
            question.getTopic(),
            question.getDifficulty(),
            new GroqApiClient.GroqCallback() {
                @Override
                public void onSuccess(double score, String feedback, String strengths, String improvements, String verdict) {
                    localProgress.setScore(score);
                    localProgress.setFeedback(feedback);
                    localProgress.setStrengths(strengths);
                    localProgress.setImprovements(improvements);
                    localProgress.setVerdict(resolveVerdict(verdict, score));
                    localProgress.setAiEvaluated(true);
                    localProgress.setEvaluationSource("AI Evaluated ✨");
                    localProgress.setComplete(true);

                    saveProgress(localProgress, listener);
                }

                @Override
                public void onError(String message) {
                    localProgress.setAiEvaluated(false);
                    localProgress.setEvaluationSource("Local Evaluation");
                    saveProgress(localProgress, listener);
                }
            }
        );
    }

    // Evaluate answer (simple keyword matching fallback)
    private InterviewProgress evaluateAnswerLocally(InterviewQuestion question, String answer) {
        InterviewProgress progress = new InterviewProgress();
        progress.setUserAnswer(answer);
        
        List<String> keywords = question.getExpectedKeywords();
        if (keywords == null || keywords.isEmpty()) {
            progress.setScore(50.0); // Default score if no keywords
            progress.setFeedback("Your answer has been recorded. Keep practicing!");
            progress.setMatchedKeywords(0);
            progress.setTotalKeywords(0);
            progress.setStrengths("You attempted the question and shared a structured response.");
            progress.setImprovements("Add concrete technical details and examples for stronger impact.");
            progress.setVerdict(resolveVerdict(null, progress.getScore()));
            progress.setAiEvaluated(false);
            progress.setEvaluationSource("Local Evaluation");
            progress.setComplete(true);
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
        progress.setStrengths(buildStrengths(answer, matchedCount, keywords.size()));
        progress.setImprovements(buildImprovements(answer, keywords));
        progress.setVerdict(resolveVerdict(null, score));
        progress.setAiEvaluated(false);
        progress.setEvaluationSource("Local Evaluation");
        progress.setComplete(true);
        
        return progress;
    }

    private void saveProgress(InterviewProgress progress, OnAnswerEvaluatedListener listener) {
        repository.insertProgress(progress, success -> {
            if (success) {
                loadUserProgress();
                loadUserStatistics();
            }
            if (listener != null) {
                listener.onEvaluated(progress);
            }
        });
    }

    private String resolveVerdict(String verdict, double score) {
        if (verdict != null) {
            String trimmed = verdict.trim();
            if (
                trimmed.equalsIgnoreCase("Excellent")
                    || trimmed.equalsIgnoreCase("Good")
                    || trimmed.equalsIgnoreCase("Fair")
                    || trimmed.equalsIgnoreCase("Poor")
            ) {
                return capitalize(trimmed);
            }
        }

        if (score >= 85) return "Excellent";
        if (score >= 65) return "Good";
        if (score >= 45) return "Fair";
        return "Poor";
    }

    private String buildStrengths(String answer, int matchedCount, int totalKeywords) {
        int answerLength = answer == null ? 0 : answer.trim().length();
        if (matchedCount >= Math.max(1, (int) (0.7 * totalKeywords))) {
            return "Strong coverage of key concepts and clear topic understanding.";
        }
        if (answerLength > 120) {
            return "Good effort with a detailed attempt and relevant technical context.";
        }
        return "You attempted the core idea and provided a concise response.";
    }

    private String buildImprovements(String answer, List<String> keywords) {
        String lowerAnswer = answer == null ? "" : answer.toLowerCase();
        StringBuilder missing = new StringBuilder();
        for (String keyword : keywords) {
            if (!lowerAnswer.contains(keyword.toLowerCase())) {
                if (missing.length() > 0) {
                    missing.append(", ");
                }
                missing.append(keyword);
            }
        }

        if (missing.length() == 0) {
            return "Add one practical example and discuss trade-offs to make the answer interview-ready.";
        }
        return "Include these points for a stronger answer: " + missing;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
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

    public LiveData<String> getErrorMessage() {
        return errorMessage;
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
