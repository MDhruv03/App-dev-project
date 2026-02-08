package com.example.myapplication.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.myapplication.model.InterviewProgress;
import com.example.myapplication.model.InterviewQuestion;

import java.util.List;

@Dao
public interface InterviewQuestionDao {
    
    @Insert
    long insert(InterviewQuestion question);
    
    @Query("SELECT * FROM interview_questions WHERE domain = :domain")
    List<InterviewQuestion> getQuestionsByDomain(String domain);
    
    @Query("SELECT * FROM interview_questions WHERE domain = :domain AND topic = :topic")
    List<InterviewQuestion> getQuestionsByTopic(String domain, String topic);
    
    @Insert
    long insertProgress(InterviewProgress progress);
    
    @Query("SELECT * FROM interview_progress WHERE userId = :userId ORDER BY attemptedAt DESC")
    List<InterviewProgress> getUserProgress(int userId);
    
    @Query("SELECT AVG(score) FROM interview_progress WHERE userId = :userId")
    double getAverageScore(int userId);
    
    @Query("SELECT COUNT(*) FROM interview_progress WHERE userId = :userId")
    int getTotalAttempts(int userId);
}
