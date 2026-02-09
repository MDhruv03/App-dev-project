package com.example.myapplication.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    
    private static final String TAG = "HomeFragment";
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        
        try {
            // Create simple layout programmatically instead of XML for now
            LinearLayout layout = new LinearLayout(requireContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setBackgroundColor(Color.parseColor("#F9FAFB"));
            layout.setPadding(48, 48, 48, 48);
            layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ));
            
            TextView title = new TextView(requireContext());
            title.setText("OpportunityHub");
            title.setTextSize(28);
            title.setTextColor(Color.parseColor("#111827"));
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, 32);
            layout.addView(title);
            
            TextView message = new TextView(requireContext());
            message.setText("Home Fragment Loaded Successfully!\n\nThe app is now working.\nLayouts will be enhanced next.");
            message.setTextSize(16);
            message.setTextColor(Color.parseColor("#374151"));
            message.setGravity(Gravity.CENTER);
            message.setPadding(0, 0, 0, 24);
            layout.addView(message);
            
            ProgressBar progressBar = new ProgressBar(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
            params.gravity = Gravity.CENTER_HORIZONTAL;
            progressBar.setLayoutParams(params);
            layout.addView(progressBar);
            
            Log.d(TAG, "Simple home fragment view created");
            return layout;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating view", e);
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
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
