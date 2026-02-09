package com.example.myapplication.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.adapter.InterviewQuestionAdapter;
import com.example.myapplication.model.InterviewQuestion;
import com.example.myapplication.util.InterviewDataGenerator;
import com.example.myapplication.viewmodel.InterviewViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.List;

public class AIInterviewFragment extends Fragment {
    
    private TextView tvReadinessScore;
    private TextView tvTotalAttempts;
    private TextView tvAverageScore;
    private ChipGroup domainChipGroup;
    private MaterialButton btnStartInterview;
    private RecyclerView recyclerQuestions;
    private InterviewViewModel viewModel;
    private InterviewQuestionAdapter adapter;
    private String selectedDomain = "SDE";
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_interview, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupViewModel();
        setupRecyclerView();
        setupDomainSelection();
        setupStartButton();
        
        loadInitialData();
    }
    
    private void initializeViews(View view) {
        tvReadinessScore = view.findViewById(R.id.tv_readiness_score);
        tvTotalAttempts = view.findViewById(R.id.tv_total_attempts);
        tvAverageScore = view.findViewById(R.id.tv_average_score);
        domainChipGroup = view.findViewById(R.id.domain_chip_group);
        btnStartInterview = view.findViewById(R.id.btn_start_interview);
        recyclerQuestions = view.findViewById(R.id.recycler_questions);
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(InterviewViewModel.class);
        
        viewModel.getUserProgress().observe(getViewLifecycleOwner(), progress -> {
            tvTotalAttempts.setText(String.valueOf(progress != null ? progress.size() : 0));
            
            if (progress != null && !progress.isEmpty()) {
                double avgScore = progress.stream()
                    .mapToDouble(p -> p.getScore())
                    .average()
                    .orElse(0.0);
                tvAverageScore.setText(String.format("%.1f", avgScore));
                
                int readiness = calculateReadinessScore(progress.size(), avgScore);
                tvReadinessScore.setText(readiness + "%");
            } else {
                tvAverageScore.setText("0.0");
                tvReadinessScore.setText("0%");
            }
        });
        
        viewModel.getQuestions().observe(getViewLifecycleOwner(), questions -> {
            if (questions != null) {
                adapter.setQuestions(questions);
            }
        });
    }
    
    private void setupRecyclerView() {
        recyclerQuestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new InterviewQuestionAdapter();
        adapter.setOnQuestionClickListener(question -> {
            Toast.makeText(requireContext(), 
                "Practice: " + question.getQuestion(), 
                Toast.LENGTH_SHORT).show();
        });
        recyclerQuestions.setAdapter(adapter);
    }
    
    private void setupDomainSelection() {
        domainChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            int checkedId = checkedIds.get(0);
            Chip chip = group.findViewById(checkedId);
            if (chip != null) {
                selectedDomain = chip.getText().toString();
                loadQuestionsForDomain(selectedDomain);
            }
        });
        
        // Set default selection
        domainChipGroup.check(R.id.chip_sde);
    }
    
    private void setupStartButton() {
        btnStartInterview.setOnClickListener(v -> {
            Toast.makeText(requireContext(), 
                "Starting " + selectedDomain + " interview...", 
                Toast.LENGTH_LONG).show();
            // TODO: Launch interview activity
        });
    }
    
    private void loadInitialData() {
        viewModel.loadUserProgress();
        viewModel.loadUserStatistics();
        loadQuestionsForDomain(selectedDomain);
    }
    
    private void loadQuestionsForDomain(String domain) {
        viewModel.loadQuestionsByDomain(domain);
    }
    
    private int calculateReadinessScore(int attempts, double avgScore) {
        // Readiness = (attempts * 10) + (avgScore / 2), max 100
        int score = (int) ((Math.min(attempts, 5) * 10) + (avgScore / 2));
        return Math.min(score, 100);
    }
}
