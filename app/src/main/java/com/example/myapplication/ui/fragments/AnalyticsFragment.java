package com.example.myapplication.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.R;
import com.example.myapplication.model.Application;
import com.example.myapplication.viewmodel.AnalyticsViewModel;
import com.example.myapplication.viewmodel.ApplicationViewModel;
import com.example.myapplication.viewmodel.InterviewViewModel;
import java.util.List;

public class AnalyticsFragment extends Fragment {
    
    private TextView tvTotalApplications;
    private TextView tvInterviewsScheduled;
    private TextView tvSuccessRate;
    private TextView tvOffersReceived;
    private TextView tvInterviewReadiness;
    private TextView tvPracticeAttempts;
    private TextView tvAverageScore;
    
    private ApplicationViewModel applicationViewModel;
    private InterviewViewModel interviewViewModel;
    private AnalyticsViewModel analyticsViewModel;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupViewModels();
        loadAnalyticsData();
    }
    
    private void initializeViews(View view) {
        tvTotalApplications = view.findViewById(R.id.tv_total_applications);
        tvInterviewsScheduled = view.findViewById(R.id.tv_interviews_scheduled);
        tvSuccessRate = view.findViewById(R.id.tv_success_rate);
        tvOffersReceived = view.findViewById(R.id.tv_offers_received);
        tvInterviewReadiness = view.findViewById(R.id.tv_interview_readiness);
        tvPracticeAttempts = view.findViewById(R.id.tv_practice_attempts);
        tvAverageScore = view.findViewById(R.id.tv_average_score);
    }
    
    private void setupViewModels() {
        applicationViewModel = new ViewModelProvider(this).get(ApplicationViewModel.class);
        interviewViewModel = new ViewModelProvider(this).get(InterviewViewModel.class);
        analyticsViewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);
        
        // Observe application data
        applicationViewModel.getAllApplications().observe(getViewLifecycleOwner(), applications -> {
            if (applications != null) {
                updateApplicationStats(applications);
            }
        });
        
        // Observe interview data
        interviewViewModel.getUserProgress().observe(getViewLifecycleOwner(), progress -> {
            if (progress != null) {
                updateInterviewStats(progress);
            }
        });
        
        // Observe analytics data
        analyticsViewModel.getSuccessRate().observe(getViewLifecycleOwner(), rate -> {
            if (rate != null) {
                tvSuccessRate.setText(String.format("%.0f%%", rate));
            }
        });
    }
    
    private void loadAnalyticsData() {
        applicationViewModel.loadAllApplications();
        interviewViewModel.loadUserProgress();
        interviewViewModel.loadUserStatistics();
        analyticsViewModel.loadAnalytics();
    }
    
    private void updateApplicationStats(List<Application> applications) {
        int total = applications.size();
        tvTotalApplications.setText(String.valueOf(total));
        
        long interviewCount = applications.stream()
            .filter(app -> "interview".equals(app.getStatus()))
            .count();
        tvInterviewsScheduled.setText(String.valueOf(interviewCount));
        
        long offerCount = applications.stream()
            .filter(app -> "accepted".equals(app.getStatus()))
            .count();
        tvOffersReceived.setText(String.valueOf(offerCount));
        
        // Calculate success rate
        if (total > 0) {
            double successRate = (offerCount * 100.0) / total;
            tvSuccessRate.setText(String.format("%.0f%%", successRate));
        } else {
            tvSuccessRate.setText("0%");
        }
    }
    
    private void updateInterviewStats(List<?> attempts) {
        int attemptCount = attempts.size();
        tvPracticeAttempts.setText(String.valueOf(attemptCount));
        
        if (attemptCount > 0) {
            // Calculate average score and readiness
            double avgScore = 75.0; // Placeholder - would calculate from actual attempts
            int readiness = Math.min((attemptCount * 10) + (int)(avgScore / 2), 100);
            
            tvInterviewReadiness.setText(readiness + "%");
            tvAverageScore.setText(String.format("%.1f/100", avgScore));
        } else {
            tvInterviewReadiness.setText("0%");
            tvAverageScore.setText("0.0/100");
        }
    }
}
