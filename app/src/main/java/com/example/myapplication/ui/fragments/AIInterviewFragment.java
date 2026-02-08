package com.example.myapplication.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.InterviewQuestionAdapter;
import com.example.myapplication.ui.activities.InterviewActivity;
import com.example.myapplication.viewmodel.InterviewViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class AIInterviewFragment extends Fragment {
    
    private ChipGroup domainChipGroup;
    private RecyclerView recyclerViewQuestions;
    private MaterialButton btnStartInterview;
    private TextView tvReadinessScore;
    private TextView tvTotalAttempts;
    private TextView tvAverageScore;
    
    private InterviewViewModel viewModel;
    private InterviewQuestionAdapter adapter;
    private String selectedDomain = "SDE";
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_interview, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupViewModel();
        setupDomainSelection();
        setupRecyclerView();
        
        viewModel.loadQuestionsByDomain(selectedDomain);
        viewModel.loadUserStatistics();
    }
    
    private void initializeViews(View view) {
        domainChipGroup = view.findViewById(R.id.domain_chip_group);
        recyclerViewQuestions = view.findViewById(R.id.recycler_questions);
        btnStartInterview = view.findViewById(R.id.btn_start_interview);
        tvReadinessScore = view.findViewById(R.id.tv_readiness_score);
        tvTotalAttempts = view.findViewById(R.id.tv_total_attempts);
        tvAverageScore = view.findViewById(R.id.tv_average_score);
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(InterviewViewModel.class);
        
        viewModel.getQuestions().observe(getViewLifecycleOwner(), questions -> {
            if (questions != null) {
                adapter.setQuestions(questions);
            }
        });
        
        viewModel.getReadinessScore().observe(getViewLifecycleOwner(), score -> {
            if (score != null && tvReadinessScore != null) {
                tvReadinessScore.setText(String.format("%.0f%%", score));
            }
        });
        
        viewModel.getTotalAttempts().observe(getViewLifecycleOwner(), attempts -> {
            if (attempts != null && tvTotalAttempts != null) {
                tvTotalAttempts.setText(String.valueOf(attempts));
            }
        });
        
        viewModel.getAverageScore().observe(getViewLifecycleOwner(), score -> {
            if (score != null && tvAverageScore != null) {
                tvAverageScore.setText(String.format("%.1f", score));
            }
        });
    }
    
    private void setupDomainSelection() {
        domainChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            int checkedId = checkedIds.get(0);
            Chip chip = group.findViewById(checkedId);
            if (chip != null) {
                selectedDomain = chip.getText().toString().toUpperCase();
                viewModel.loadQuestionsByDomain(selectedDomain);
            }
        });
        
        btnStartInterview.setOnClickListener(v -> startInterview());
    }
    
    private void setupRecyclerView() {
        recyclerViewQuestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new InterviewQuestionAdapter();
        adapter.setOnQuestionClickListener(question -> {
            // Could show question details or hints
        });
        recyclerViewQuestions.setAdapter(adapter);
    }
    
    private void startInterview() {
        Intent intent = new Intent(requireContext(), InterviewActivity.class);
        intent.putExtra("DOMAIN", selectedDomain);
        startActivity(intent);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh statistics when returning from interview
        viewModel.loadUserStatistics();
    }
}
