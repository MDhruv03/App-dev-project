package com.example.myapplication.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

public class AIInterviewFragment extends Fragment {
    
    private ChipGroup domainChipGroup;
    private RecyclerView recyclerViewQuestions;
    private MaterialButton btnStartInterview;
    private TextView tvReadinessScore;
    
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
        setupDomainSelection();
        setupRecyclerView();
    }
    
    private void initializeViews(View view) {
        domainChipGroup = view.findViewById(R.id.domain_chip_group);
        recyclerViewQuestions = view.findViewById(R.id.recycler_questions);
        btnStartInterview = view.findViewById(R.id.btn_start_interview);
        tvReadinessScore = view.findViewById(R.id.tv_readiness_score);
    }
    
    private void setupDomainSelection() {
        btnStartInterview.setOnClickListener(v -> {
            // TODO: Start mock interview
        });
    }
    
    private void setupRecyclerView() {
        recyclerViewQuestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // TODO: Set adapter
    }
}
