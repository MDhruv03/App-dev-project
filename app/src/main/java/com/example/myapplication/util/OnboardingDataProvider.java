package com.example.myapplication.util;

import com.example.myapplication.model.OnboardingStep;
import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Onboarding data provider
 */
public class OnboardingDataProvider {
    
    /**
     * Get onboarding steps
     */
    public static List<OnboardingStep> getOnboardingSteps() {
        List<OnboardingStep> steps = new ArrayList<>();
        
        steps.add(new OnboardingStep(
            "Discover Opportunities",
            "Browse thousands of internships, jobs, and hackathons from top companies worldwide. Find your perfect match with AI-powered recommendations.",
            R.drawable.ic_launcher_foreground,
            "Next"
        ));
        
        steps.add(new OnboardingStep(
            "Track Applications",
            "Keep track of all your applications in one place. Get reminders for deadlines and interviews so you never miss an opportunity.",
            R.drawable.ic_launcher_foreground,
            "Next"
        ));
        
        steps.add(new OnboardingStep(
            "AI Interview Prep",
            "Practice with AI-powered mock interviews. Get instant feedback and improve your interview skills across various domains.",
            R.drawable.ic_launcher_foreground,
            "Next"
        ));
        
        steps.add(new OnboardingStep(
            "Get Started!",
            "Ready to accelerate your career? Let's set up your profile and find opportunities tailored just for you!",
            R.drawable.ic_launcher_foreground,
            "Get Started"
        ));
        
        return steps;
    }
    
    /**
     * Check if user has completed onboarding
     */
    public static boolean hasCompletedOnboarding(PreferencesManager prefsManager) {
        return prefsManager.getBoolean("onboarding_completed", false);
    }
    
    /**
     * Mark onboarding as completed
     */
    public static void markOnboardingCompleted(PreferencesManager prefsManager) {
        prefsManager.putBoolean("onboarding_completed", true);
    }
}
