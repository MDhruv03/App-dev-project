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
import com.example.myapplication.viewmodel.AnalyticsViewModel;

public class AnalyticsFragment extends Fragment {
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setBackgroundColor(android.graphics.Color.WHITE);
        layout.setPadding(48, 48, 48, 48);
        
        android.widget.TextView tv = new android.widget.TextView(requireContext());
        tv.setText("📈 Analytics Dashboard\n\nComing Soon");
        tv.setTextSize(20);
        tv.setTextColor(android.graphics.Color.BLACK);
        tv.setGravity(android.view.Gravity.CENTER);
        layout.addView(tv);
        
        return layout;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupViewModel();
        
        viewModel.loadAnalytics();
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
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);
        
        viewModel.getTotalApplications().observe(getViewLifecycleOwner(), count -> {
            if (count != null && tvTotalApplications != null) {
                tvTotalApplications.setText(String.valueOf(count));
            }
        });
        
        viewModel.getInterviewsScheduled().observe(getViewLifecycleOwner(), count -> {
            if (count != null && tvInterviewsScheduled != null) {
                tvInterviewsScheduled.setText(String.valueOf(count));
            }
        });
        
        viewModel.getSuccessRate().observe(getViewLifecycleOwner(), rate -> {
            if (rate != null && tvSuccessRate != null) {
                tvSuccessRate.setText(String.format("%.1f%%", rate));
            }
        });
        
        viewModel.getOffersReceived().observe(getViewLifecycleOwner(), count -> {
            if (count != null && tvOffersReceived != null) {
                tvOffersReceived.setText(String.valueOf(count));
            }
        });
        
        viewModel.getInterviewReadiness().observe(getViewLifecycleOwner(), score -> {
            if (score != null && tvInterviewReadiness != null) {
                tvInterviewReadiness.setText(String.format("%.0f%%", score));
            }
        });
        
        viewModel.getPracticeAttempts().observe(getViewLifecycleOwner(), count -> {
            if (count != null && tvPracticeAttempts != null) {
                tvPracticeAttempts.setText(String.valueOf(count));
            }
        });
        
        viewModel.getAverageInterviewScore().observe(getViewLifecycleOwner(), score -> {
            if (score != null && tvAverageScore != null) {
                tvAverageScore.setText(String.format("%.1f/100", score));
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadAnalytics();
    }
}
