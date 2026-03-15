package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.Ignore;
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

    // Runtime-only fields for richer feedback UI
    @Ignore
    private String strengths;

    @Ignore
    private String improvements;

    @Ignore
    private String verdict;

    @Ignore
    private String evaluationSource;

    @Ignore
    private boolean aiEvaluated;
    
    // Timestamps
    private Date attemptedAt;
    
    // Constructors
    public InterviewProgress() {
        this.attemptedAt = new Date();
        this.isComplete = false;
    }
    
    @Ignore
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

    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }

    public String getImprovements() { return improvements; }
    public void setImprovements(String improvements) { this.improvements = improvements; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public String getEvaluationSource() { return evaluationSource; }
    public void setEvaluationSource(String evaluationSource) { this.evaluationSource = evaluationSource; }

    public boolean isAiEvaluated() { return aiEvaluated; }
    public void setAiEvaluated(boolean aiEvaluated) { this.aiEvaluated = aiEvaluated; }
    
    public Date getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(Date attemptedAt) { this.attemptedAt = attemptedAt; }
}
