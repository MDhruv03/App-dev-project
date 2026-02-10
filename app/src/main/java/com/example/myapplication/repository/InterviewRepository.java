package com.example.myapplication.repository;

import android.content.Context;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.database.InterviewQuestionDao;
import com.example.myapplication.model.InterviewProgress;
import com.example.myapplication.model.InterviewQuestion;
import com.example.myapplication.network.MockApiService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InterviewRepository {
    
    private final InterviewQuestionDao interviewDao;
    private final MockApiService apiService;
    private final ExecutorService executorService;
    
    public InterviewRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        interviewDao = database.interviewQuestionDao();
        apiService = MockApiService.getInstance();
        executorService = Executors.newSingleThreadExecutor();
    }
    
    // Insert question
    public void insertQuestion(InterviewQuestion question, OnOperationCompleteListener listener) {
        executorService.execute(() -> {
            long id = interviewDao.insert(question);
            if (listener != null) {
                listener.onComplete(id > 0);
            }
        });
    }
    
    // Get questions by domain (fetch from API, cache locally)
    public void getQuestionsByDomain(String domain, OnQuestionsLoadedListener listener) {
        apiService.fetchQuestionsByDomain(domain, questions -> {
            executorService.execute(() -> {
                // Cache questions locally
                for (InterviewQuestion q : questions) {
                    interviewDao.insert(q);
                }
                if (listener != null) {
                    listener.onLoaded(questions);
                }
            });
        });
    }
    
    // Get questions by topic
    public void getQuestionsByTopic(String domain, String topic, OnQuestionsLoadedListener listener) {
        executorService.execute(() -> {
            List<InterviewQuestion> questions = interviewDao.getQuestionsByTopic(domain, topic);
            if (listener != null) {
                listener.onLoaded(questions);
            }
        });
    }
    
    // Insert progress
    public void insertProgress(InterviewProgress progress, OnOperationCompleteListener listener) {
        executorService.execute(() -> {
            long id = interviewDao.insertProgress(progress);
            if (listener != null) {
                listener.onComplete(id > 0);
            }
        });
    }
    
    // Get user progress
    public void getUserProgress(int userId, OnProgressLoadedListener listener) {
        executorService.execute(() -> {
            List<InterviewProgress> progress = interviewDao.getUserProgress(userId);
            if (listener != null) {
                listener.onLoaded(progress);
            }
        });
    }
    
    // Get average score
    public void getAverageScore(int userId, OnScoreLoadedListener listener) {
        executorService.execute(() -> {
            double score = interviewDao.getAverageScore(userId);
            if (listener != null) {
                listener.onLoaded(score);
            }
        });
    }
    
    // Get total attempts
    public void getTotalAttempts(int userId, OnCountLoadedListener listener) {
        executorService.execute(() -> {
            int count = interviewDao.getTotalAttempts(userId);
            if (listener != null) {
                listener.onLoaded(count);
            }
        });
    }
    
    // Interfaces for callbacks
    public interface OnQuestionsLoadedListener {
        void onLoaded(List<InterviewQuestion> questions);
    }
    
    public interface OnProgressLoadedListener {
        void onLoaded(List<InterviewProgress> progress);
    }
    
    public interface OnOperationCompleteListener {
        void onComplete(boolean success);
    }
    
    public interface OnScoreLoadedListener {
        void onLoaded(double score);
    }
    
    public interface OnCountLoadedListener {
        void onLoaded(int count);
    }
}
