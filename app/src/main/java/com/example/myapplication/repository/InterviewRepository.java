package com.example.myapplication.repository;

import android.content.Context;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.database.InterviewQuestionDao;
import com.example.myapplication.model.InterviewProgress;
import com.example.myapplication.model.InterviewQuestion;
import com.example.myapplication.network.MockApiService;
import com.example.myapplication.network.NetworkUtils;
import com.example.myapplication.util.InterviewDataGenerator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InterviewRepository {
    
    private final Context appContext;
    private final InterviewQuestionDao interviewDao;
    private final MockApiService apiService;
    private final ExecutorService executorService;
    
    public InterviewRepository(Context context) {
        appContext = context.getApplicationContext();
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
    
    // Network-first with local fallback
    public void getQuestionsByDomain(String domain, OnQuestionsLoadedListener listener) {
        if (!NetworkUtils.isNetworkAvailable(appContext)) {
            loadQuestionsFromLocal(domain, listener, "No internet connection. Showing cached interview questions.");
            return;
        }

        apiService.fetchQuestionsByDomain(domain, new com.example.myapplication.network.ApiCallback<List<InterviewQuestion>>() {
            @Override
            public void onSuccess(List<InterviewQuestion> remoteQuestions) {
                executorService.execute(() -> {
                    if (remoteQuestions == null || remoteQuestions.isEmpty()) {
                        loadQuestionsFromLocal(domain, listener, null);
                        return;
                    }

                    List<InterviewQuestion> localQuestions = interviewDao.getQuestionsByDomain(domain);
                    InterviewSyncHelper.mergeLocalQuestionStateIntoRemote(remoteQuestions, localQuestions);
                    interviewDao.deleteQuestionsByDomain(domain);
                    interviewDao.insertAll(remoteQuestions);
                    List<InterviewQuestion> questions = interviewDao.getQuestionsByDomain(domain);
                    if (listener != null) {
                        listener.onLoaded(questions);
                    }
                });
            }

            @Override
            public void onError(String error) {
                loadQuestionsFromLocal(domain, listener, "Failed to sync interview questions. Showing cached data.");
            }
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

        default void onError(String message) {
        }
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

    private void loadQuestionsFromLocal(String domain, OnQuestionsLoadedListener listener, String fallbackMessage) {
        executorService.execute(() -> {
            List<InterviewQuestion> questions = interviewDao.getQuestionsByDomain(domain);
            if (questions == null || questions.isEmpty()) {
                List<InterviewQuestion> seeded = InterviewDataGenerator.generateQuestionsForDomain(domain);
                if (seeded != null && !seeded.isEmpty()) {
                    interviewDao.insertAll(seeded);
                    questions = interviewDao.getQuestionsByDomain(domain);
                }
            }

            if (listener != null) {
                if (fallbackMessage != null && !fallbackMessage.trim().isEmpty()) {
                    listener.onError(fallbackMessage);
                }
                listener.onLoaded(questions);
            }
        });
    }
}
