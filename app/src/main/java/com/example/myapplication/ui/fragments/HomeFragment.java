package com.example.myapplication.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.R;
import com.example.myapplication.adapter.OpportunityAdapter;
import com.example.myapplication.adapter.OpportunityHorizontalAdapter;
import com.example.myapplication.model.Opportunity;
import com.example.myapplication.util.SampleDataGenerator;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.search.SearchBar;

import java.util.List;

public class HomeFragment extends Fragment {
    
    private SearchBar searchBar;
    private ChipGroup filterChipGroup;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerViewRecommended;
    private RecyclerView recyclerViewAll;
    
    private OpportunityHorizontalAdapter recommendedAdapter;
    private OpportunityAdapter allOpportunitiesAdapter;
    
    private List<Opportunity> allOpportunities;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupRecyclerViews();
        setupSwipeRefresh();
        setupFilters();
        loadOpportunities();
    }
    
    private void initializeViews(View view) {
        searchBar = view.findViewById(R.id.search_bar);
        filterChipGroup = view.findViewById(R.id.filter_chip_group);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        recyclerViewRecommended = view.findViewById(R.id.recycler_recommended);
        recyclerViewAll = view.findViewById(R.id.recycler_all);
    }
    
    private void setupRecyclerViews() {
        // Recommended opportunities (horizontal)
        recyclerViewRecommended.setLayoutManager(
            new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        recommendedAdapter = new OpportunityHorizontalAdapter();
        recommendedAdapter.setOnOpportunityClickListener(opportunity -> {
            Toast.makeText(requireContext(), 
                "Viewing details for " + opportunity.getTitle(), 
                Toast.LENGTH_SHORT).show();
        });
        recyclerViewRecommended.setAdapter(recommendedAdapter);
        
        // All opportunities (vertical)
        recyclerViewAll.setLayoutManager(new LinearLayoutManager(requireContext()));
        allOpportunitiesAdapter = new OpportunityAdapter();
        allOpportunitiesAdapter.setOnOpportunityClickListener(createOpportunityListener());
        recyclerViewAll.setAdapter(allOpportunitiesAdapter);
    }
    
    private OpportunityAdapter.OnOpportunityClickListener createOpportunityListener() {
        return new OpportunityAdapter.OnOpportunityClickListener() {
            @Override
            public void onApplyClick(Opportunity opportunity) {
                Toast.makeText(requireContext(), 
                    "Opening application for " + opportunity.getCompany(), 
                    Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onSaveClick(Opportunity opportunity) {
                opportunity.setSaved(!opportunity.isSaved());
                Toast.makeText(requireContext(), 
                    opportunity.isSaved() ? "Saved!" : "Removed from saved", 
                    Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onOpportunityClick(Opportunity opportunity) {
                Toast.makeText(requireContext(), 
                    "Viewing details for " + opportunity.getTitle(), 
                    Toast.LENGTH_SHORT).show();
            }
        };
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadOpportunities();
            swipeRefreshLayout.setRefreshing(false);
        });
    }
    
    private void setupFilters() {
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            // Filter logic will be implemented when connected to ViewModel
        });
    }
    
    private void loadOpportunities() {
        // Load sample data
        List<Opportunity> recommended = SampleDataGenerator.getRecommendedOpportunities();
        allOpportunities = SampleDataGenerator.getAllOpportunities();
        
        recommendedAdapter.setOpportunities(recommended);
        allOpportunitiesAdapter.setOpportunities(allOpportunities);
    }
}
