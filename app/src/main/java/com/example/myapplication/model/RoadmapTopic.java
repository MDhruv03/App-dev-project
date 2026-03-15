package com.example.myapplication.model;

public class RoadmapTopic {

    private int id;
    private String title;
    private String description;
    private String skill;
    private int priority; // 1=high, 2=medium, 3=low
    private int estimatedDays;
    private String resources; // comma-separated links
    private boolean isCompleted = false;
    private String category; // Weak Area, Skill Gap, Career Goal

    public RoadmapTopic() {
    }

    public RoadmapTopic(
        int id,
        String title,
        String description,
        String skill,
        int priority,
        int estimatedDays,
        String resources,
        boolean isCompleted,
        String category
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.skill = skill;
        this.priority = priority;
        this.estimatedDays = estimatedDays;
        this.resources = resources;
        this.isCompleted = isCompleted;
        this.category = category;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getEstimatedDays() {
        return estimatedDays;
    }

    public void setEstimatedDays(int estimatedDays) {
        this.estimatedDays = estimatedDays;
    }

    public String getResources() {
        return resources;
    }

    public void setResources(String resources) {
        this.resources = resources;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
