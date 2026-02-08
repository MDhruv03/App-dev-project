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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.search.SearchBar;

public class HomeFragment extends Fragment {
    
    private SearchBar searchBar;
    private ChipGroup filterChipGroup;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerViewRecommended;
    private RecyclerView recyclerViewAll;
    
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
    }
    
    private void initializeViews(View view) {
        searchBar = view.findViewById(R.id.search_bar);
        filterChipGroup = view.findViewById(R.id.filter_chip_group);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        recyclerViewRecommended = view.findViewById(R.id.recycler_recommended);
        recyclerViewAll = view.findViewById(R.id.recycler_all);
    }
    
    private void setupRecyclerViews() {
        recyclerViewRecommended.setLayoutManager(
            new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        
        recyclerViewAll.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // TODO: Set adapters
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // TODO: Refresh opportunities
            swipeRefreshLayout.setRefreshing(false);
        });
    }
    
    private void setupFilters() {
        // Filter chips will be populated dynamically
    }
}
