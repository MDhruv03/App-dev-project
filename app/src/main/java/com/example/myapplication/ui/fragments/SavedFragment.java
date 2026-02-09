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

public class SavedFragment extends Fragment {
    
    private ChipGroup filterChipGroup;
    private RecyclerView recyclerViewSaved;
    private OpportunityViewModel viewModel;
    private OpportunityAdapter adapter;
    
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
                adapter.setOpportunities(opportunities);
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
                Toast.makeText(requireContext(), "Applied to " + opportunity.getTitle(), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onSaveClick(Opportunity opportunity) {
                opportunity.setSaved(false);
                viewModel.updateOpportunity(opportunity);
                Toast.makeText(requireContext(), "Removed from saved", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerViewSaved.setAdapter(adapter);
    }
    
    private void setupFilters() {
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            String type = "all";
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                Chip chip = group.findViewById(checkedId);
                if (chip != null) {
                    type = chip.getText().toString().toLowerCase();
                }
            }
            viewModel.filterSavedByType(type);
        });
    }
}
