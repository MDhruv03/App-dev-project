package com.example.myapplication.model;

/**
 * Onboarding step model
 */
public class OnboardingStep {
    private String title;
    private String description;
    private int imageResource;
    private String buttonText;
    
    public OnboardingStep(String title, String description, int imageResource, String buttonText) {
        this.title = title;
        this.description = description;
        this.imageResource = imageResource;
        this.buttonText = buttonText;
    }
    
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getImageResource() { return imageResource; }
    public String getButtonText() { return buttonText; }
}
