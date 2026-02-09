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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.myapplication.R;
import com.example.myapplication.adapter.OpportunityAdapter;
import com.example.myapplication.adapter.OpportunityHorizontalAdapter;
import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.model.Opportunity;
import com.example.myapplication.util.SampleDataGenerator;
import com.example.myapplication.viewmodel.OpportunityViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.search.SearchBar;
import java.util.List;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {
    
    private SearchBar searchBar;
    private ChipGroup filterChipGroup;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerViewRecommended;
    private RecyclerView recyclerViewAll;
    
    private OpportunityHorizontalAdapter recommendedAdapter;
    private OpportunityAdapter allOpportunitiesAdapter;
    private OpportunityViewModel viewModel;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupViewModel();
        setupRecyclerViews();
        setupSwipeRefresh();
        setupFilters();
        initializeDatabaseIfNeeded();
        
        viewModel.loadAllOpportunities();
        viewModel.loadRecommendedOpportunities();
    }
    
    private void initializeViews(View view) {
        searchBar = view.findViewById(R.id.search_bar);
        filterChipGroup = view.findViewById(R.id.filter_chip_group);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        recyclerViewRecommended = view.findViewById(R.id.recycler_recommended);
        recyclerViewAll = view.findViewById(R.id.recycler_all);
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(OpportunityViewModel.class);
        
        viewModel.getRecommendedOpportunities().observe(getViewLifecycleOwner(), opportunities -> {
            if (opportunities != null && !opportunities.isEmpty()) {
                recommendedAdapter.setOpportunities(opportunities);
            }
        });
        
        viewModel.getFilteredOpportunities().observe(getViewLifecycleOwner(), opportunities -> {
            if (opportunities != null) {
                allOpportunitiesAdapter.setOpportunities(opportunities);
            }
        });
        
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                swipeRefreshLayout.setRefreshing(isLoading);
            }
        });
    }
    
    private void setupRecyclerViews() {
        recyclerViewRecommended.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recommendedAdapter = new OpportunityHorizontalAdapter();
        recommendedAdapter.setOnOpportunityClickListener(new OpportunityHorizontalAdapter.OnOpportunityClickListener() {
            @Override
            public void onOpportunityClick(Opportunity opportunity) {
                showOpportunityDetails(opportunity);
            }
            
            @Override
            public void onSaveClick(Opportunity opportunity) {
                saveOpportunity(opportunity);
            }
        });
        recyclerViewRecommended.setAdapter(recommendedAdapter);
        
        recyclerViewAll.setLayoutManager(new LinearLayoutManager(requireContext()));
        allOpportunitiesAdapter = new OpportunityAdapter();
        allOpportunitiesAdapter.setOnOpportunityClickListener(new OpportunityAdapter.OnOpportunityClickListener() {
            @Override
            public void onOpportunityClick(Opportunity opportunity) {
                showOpportunityDetails(opportunity);
            }
            
            @Override
            public void onApplyClick(Opportunity opportunity) {
                applyToOpportunity(opportunity);
            }
            
            @Override
            public void onSaveClick(Opportunity opportunity) {
                saveOpportunity(opportunity);
            }
        });
        recyclerViewAll.setAdapter(allOpportunitiesAdapter);
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.loadAllOpportunities();
            viewModel.loadRecommendedOpportunities();
        });
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
            viewModel.filterByType(type);
        });
    }
    
    private void initializeDatabaseIfNeeded() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<Opportunity> existing = db.opportunityDao().getAllOpportunitiesSync();
            
            if (existing == null || existing.isEmpty()) {
                List<Opportunity> sampleData = SampleDataGenerator.generateOpportunities();
                db.opportunityDao().insertAll(sampleData);
            }
        });
    }
    
    private void showOpportunityDetails(Opportunity opportunity) {
        Toast.makeText(requireContext(), "Opportunity: " + opportunity.getTitle(), Toast.LENGTH_SHORT).show();
    }
    
    private void applyToOpportunity(Opportunity opportunity) {
        Toast.makeText(requireContext(), "Applied to " + opportunity.getTitle(), Toast.LENGTH_SHORT).show();
    }
    
    private void saveOpportunity(Opportunity opportunity) {
        opportunity.setSaved(!opportunity.isSaved());
        viewModel.updateOpportunity(opportunity);
        Toast.makeText(requireContext(), opportunity.isSaved() ? "Saved!" : "Unsaved", Toast.LENGTH_SHORT).show();
    }
}
