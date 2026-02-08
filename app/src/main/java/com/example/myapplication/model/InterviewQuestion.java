package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.myapplication.database.Converters;

import java.util.List;

@Entity(tableName = "interview_questions")
@TypeConverters(Converters.class)
public class InterviewQuestion {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String domain; // "SDE", "ML", "Web", "Android", "HR"
    private String topic; // "DSA", "OOPS", "DBMS", "System Design", "Behavioral"
    private String question;
    private String difficulty; // "Easy", "Medium", "Hard"
    
    // Expected answer components (keywords to check)
    private List<String> expectedKeywords;
    private String sampleAnswer;
    private List<String> hints;
    
    // Metadata
    private int timesAsked;
    private double averageScore;
    
    // Constructors
    public InterviewQuestion() {
        this.timesAsked = 0;
        this.averageScore = 0.0;
    }
    
    public InterviewQuestion(String domain, String topic, String question, String difficulty) {
        this();
        this.domain = domain;
        this.topic = topic;
        this.question = question;
        this.difficulty = difficulty;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    
    public List<String> getExpectedKeywords() { return expectedKeywords; }
    public void setExpectedKeywords(List<String> expectedKeywords) { 
        this.expectedKeywords = expectedKeywords; 
    }
    
    public String getSampleAnswer() { return sampleAnswer; }
    public void setSampleAnswer(String sampleAnswer) { this.sampleAnswer = sampleAnswer; }
    
    public List<String> getHints() { return hints; }
    public void setHints(List<String> hints) { this.hints = hints; }
    
    public int getTimesAsked() { return timesAsked; }
    public void setTimesAsked(int timesAsked) { this.timesAsked = timesAsked; }
    
    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double averageScore) { this.averageScore = averageScore; }
}
