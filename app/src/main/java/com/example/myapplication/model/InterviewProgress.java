package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.myapplication.database.Converters;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Entity(tableName = "interview_progress")
@TypeConverters(Converters.class)
public class InterviewProgress {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private int userId;
    private int questionId;
    private String userAnswer;
    private double score; // 0.0 to 100.0
    private String feedback;
    
    // Performance metrics
    private int matchedKeywords;
    private int totalKeywords;
    private boolean isComplete;
    
    // Weak areas identified
    private List<String> weakTopics;
    
    // Timestamps
    private Date attemptedAt;
    
    // Constructors
    public InterviewProgress() {
        this.attemptedAt = new Date();
        this.isComplete = false;
    }
    
    public InterviewProgress(int userId, int questionId) {
        this();
        this.userId = userId;
        this.questionId = questionId;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }
    
    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
    
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    
    public int getMatchedKeywords() { return matchedKeywords; }
    public void setMatchedKeywords(int matchedKeywords) { 
        this.matchedKeywords = matchedKeywords; 
    }
    
    public int getTotalKeywords() { return totalKeywords; }
    public void setTotalKeywords(int totalKeywords) { this.totalKeywords = totalKeywords; }
    
    public boolean isComplete() { return isComplete; }
    public void setComplete(boolean complete) { isComplete = complete; }
    
    public List<String> getWeakTopics() { return weakTopics; }
    public void setWeakTopics(List<String> weakTopics) { this.weakTopics = weakTopics; }
    
    public Date getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(Date attemptedAt) { this.attemptedAt = attemptedAt; }
}
