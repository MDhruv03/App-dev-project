package com.example.myapplication.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.adapter.OpportunityAdapter;
import com.example.myapplication.model.Opportunity;
import com.example.myapplication.viewmodel.OpportunityViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SavedFragment extends Fragment {
    
    private ChipGroup filterChipGroup;
    private RecyclerView recyclerViewSaved;
    private OpportunityViewModel viewModel;
    private OpportunityAdapter adapter;
    private List<Opportunity> savedSource = new ArrayList<>();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupViewModel();
        setupRecyclerView();
        setupFilters();
        
        viewModel.loadSavedOpportunities();
    }
    
    private void initializeViews(View view) {
        filterChipGroup = view.findViewById(R.id.filter_chip_group);
        recyclerViewSaved = view.findViewById(R.id.recycler_saved);
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(OpportunityViewModel.class);
        
        viewModel.getSavedOpportunities().observe(getViewLifecycleOwner(), opportunities -> {
            if (opportunities != null) {
                savedSource = opportunities;
                applySavedFilters();
            }
        });
    }
    
    private void setupRecyclerView() {
        recyclerViewSaved.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OpportunityAdapter();
        adapter.setOnOpportunityClickListener(new OpportunityAdapter.OnOpportunityClickListener() {
            @Override
            public void onOpportunityClick(Opportunity opportunity) {
                Toast.makeText(requireContext(), opportunity.getTitle(), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onApplyClick(Opportunity opportunity) {
                viewModel.markAsApplied(opportunity);
                Toast.makeText(requireContext(), "Applied to " + opportunity.getTitle(), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onSaveClick(Opportunity opportunity) {
                viewModel.toggleSaveStatus(opportunity);
                Toast.makeText(requireContext(), "Removed from saved", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerViewSaved.setAdapter(adapter);
    }
    
    private void setupFilters() {
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            applySavedFilters();
        });
    }

    private void applySavedFilters() {
        List<Integer> checkedIds = filterChipGroup.getCheckedChipIds();
        if (checkedIds == null || checkedIds.isEmpty()) {
            adapter.setOpportunities(savedSource);
            return;
        }

        List<String> typeFilters = new ArrayList<>();
        for (Integer checkedId : checkedIds) {
            Chip chip = filterChipGroup.findViewById(checkedId);
            if (chip != null) {
                String label = chip.getText().toString().toLowerCase(Locale.ROOT);
                if (!"all".equals(label)) {
                    typeFilters.add(label);
                }
            }
        }

        if (typeFilters.isEmpty()) {
            adapter.setOpportunities(savedSource);
            return;
        }

        List<Opportunity> filtered = savedSource.stream()
                .filter(opp -> typeFilters.contains(opp.getType().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        adapter.setOpportunities(filtered);
    }
}
