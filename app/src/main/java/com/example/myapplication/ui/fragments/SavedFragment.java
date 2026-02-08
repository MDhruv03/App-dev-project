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

public class SavedFragment extends Fragment {
    
    private ChipGroup filterChipGroup;
    private RecyclerView recyclerViewSaved;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupRecyclerView();
    }
    
    private void initializeViews(View view) {
        filterChipGroup = view.findViewById(R.id.filter_chip_group);
        recyclerViewSaved = view.findViewById(R.id.recycler_saved);
    }
    
    private void setupRecyclerView() {
        recyclerViewSaved.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // TODO: Set adapter
    }
}
