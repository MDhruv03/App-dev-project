package com.example.myapplication.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class TrackerFragment extends Fragment {
    
    private ChipGroup statusChipGroup;
    private RecyclerView recyclerViewApplications;
    private ExtendedFloatingActionButton fabAddApplication;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tracker, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupRecyclerView();
        setupFab();
    }
    
    private void initializeViews(View view) {
        statusChipGroup = view.findViewById(R.id.status_chip_group);
        recyclerViewApplications = view.findViewById(R.id.recycler_applications);
        fabAddApplication = view.findViewById(R.id.fab_add_application);
    }
    
    private void setupRecyclerView() {
        recyclerViewApplications.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // TODO: Set adapter
    }
    
    private void setupFab() {
        fabAddApplication.setOnClickListener(v -> {
            // TODO: Add new application
        });
    }
}
