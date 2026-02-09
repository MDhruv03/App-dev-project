package com.example.myapplication.ui.fragments;

import android.os.Bundle;
import android.util.Log;
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
import com.example.myapplication.util.InterviewDataGenerator;
import com.example.myapplication.util.SampleDataGenerator;
import com.example.myapplication.viewmodel.OpportunityViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.search.SearchBar;

import java.util.List;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {
    
    private static final String TAG = "HomeFragment";
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        try {
            View view = inflater.inflate(R.layout.fragment_home, container, false);
            Log.d(TAG, "Layout inflated successfully");
            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error inflating layout", e);
            Toast.makeText(requireContext(), "Error loading home: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");
        
        try {
            initializeViews(view);
            setupViewModel();
            setupRecyclerViews();
            setupSwipeRefresh();
            setupFilters();
            
            // Initialize database with sample data on first run
            initializeDatabaseIfNeeded();
            
            viewModel.loadAllOpportunities();
            viewModel.loadRecommendedOpportunities();
            Log.d(TAG, "View setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onViewCreated", e);
            Toast.makeText(requireContext(), "Error setting up home: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
            if (opportunities != null) {
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
        // Recommended opportunities (horizontal)
        recyclerViewRecommended.setLayoutManager(
            new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        recommendedAdapter = new OpportunityHorizontalAdapter();
        recommendedAdapter.setOnOpportunityClickListener(opportunity -> {
            Toast.makeText(requireContext(), 
                "Viewing " + opportunity.getTitle() + " at " + opportunity.getCompany(), 
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
                viewModel.markAsApplied(opportunity);
            }
            
            @Override
            public void onSaveClick(Opportunity opportunity) {
                viewModel.toggleSaveStatus(opportunity);
                String message = opportunity.isSaved() ? "Saved!" : "Removed from saved";
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onOpportunityClick(Opportunity opportunity) {
                Toast.makeText(requireContext(), 
                    opportunity.getTitle() + " at " + opportunity.getCompany(),
                    Toast.LENGTH_LONG).show();
            }
        };
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.loadAllOpportunities();
            viewModel.loadRecommendedOpportunities();
        });
    }
    
    private void setupFilters() {
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                viewModel.filterByType("all");
                return;
            }
            
            int checkedId = checkedIds.get(0);
            Chip chip = group.findViewById(checkedId);
            if (chip != null) {
                String filter = chip.getText().toString().toLowerCase();
                viewModel.filterByType(filter);
            }
        });
    }
    
    private void initializeDatabaseIfNeeded() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            
            // Check if database has data
            List<Opportunity> existingOpportunities = db.opportunityDao().getAllOpportunities();
            
            if (existingOpportunities.isEmpty()) {
                // Add sample opportunities
                List<Opportunity> sampleOpportunities = SampleDataGenerator.generateOpportunities(30);
                for (Opportunity opportunity : sampleOpportunities) {
                    db.opportunityDao().insert(opportunity);
                }
                
                // Add sample interview questions
                String[] domains = {"SDE", "ML", "WEB", "ANDROID", "HR"};
                for (String domain : domains) {
                    List<com.example.myapplication.model.InterviewQuestion> questions = 
                        InterviewDataGenerator.generateQuestionsForDomain(domain);
                    for (com.example.myapplication.model.InterviewQuestion question : questions) {
                        db.interviewQuestionDao().insert(question);
                    }
                }
                
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Database initialized with sample data!", 
                        Toast.LENGTH_SHORT).show();
                    viewModel.loadAllOpportunities();
                    viewModel.loadRecommendedOpportunities();
                });
            }
        });
    }
}
