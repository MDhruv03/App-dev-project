package com.example.myapplication.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.myapplication.model.Opportunity;
import com.example.myapplication.viewmodel.OpportunityViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {
    
    private TextInputEditText searchInput;
    private ChipGroup filterChipGroup;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerViewRecommended;
    private RecyclerView recyclerViewAll;
    private View layoutEmptyState;
    
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
        setupRecyclerViews();
        setupViewModel();
        setupSwipeRefresh();
        setupFilters();
        setupSearch();
        
        viewModel.loadAllOpportunities();
        viewModel.loadRecommendedOpportunities();
    }
    
    private void initializeViews(View view) {
        searchInput = view.findViewById(R.id.searchView);
        filterChipGroup = view.findViewById(R.id.filter_chip_group);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        recyclerViewRecommended = view.findViewById(R.id.rvRecommended);
        recyclerViewAll = view.findViewById(R.id.rvOpportunities);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(OpportunityViewModel.class);

        viewModel.getAllOpportunities().observe(getViewLifecycleOwner(), opportunities -> {
            if (opportunities == null) {
                recommendedAdapter.setOpportunities(new ArrayList<>());
                return;
            }

            List<Opportunity> recommended = opportunities.stream()
                    .filter(opportunity -> !opportunity.isSaved())
                    .sorted(Comparator.comparingDouble(Opportunity::getRecommendationScore).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
            recommendedAdapter.setOpportunities(recommended);
        });
        
        viewModel.getFilteredOpportunities().observe(getViewLifecycleOwner(), opportunities -> {
            boolean isEmpty = opportunities == null || opportunities.isEmpty();
            layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recyclerViewAll.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            if (opportunities != null) {
                allOpportunitiesAdapter.setOpportunities(opportunities);
            }
        });
        
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                swipeRefreshLayout.setRefreshing(isLoading);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
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

            @Override
            public void onShareClick(Opportunity opportunity) {
                shareOpportunity(opportunity);
            }
        });
        recyclerViewAll.setAdapter(allOpportunitiesAdapter);
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.refresh();
        });
    }
    
    private void setupFilters() {
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            List<String> activeFilters = new ArrayList<>();
            for (Integer checkedId : checkedIds) {
                Chip chip = group.findViewById(checkedId);
                if (chip != null) {
                    String token;
                    if (checkedId == R.id.chip_internship) {
                        token = "internship";
                    } else if (checkedId == R.id.chip_job) {
                        token = "job";
                    } else if (checkedId == R.id.chip_hackathon) {
                        token = "hackathon";
                    } else if (checkedId == R.id.chip_remote_yes) {
                        token = "remote_yes";
                    } else if (checkedId == R.id.chip_remote_no) {
                        token = "remote_no";
                    } else if (checkedId == R.id.chip_paid_yes) {
                        token = "paid_yes";
                    } else if (checkedId == R.id.chip_paid_no) {
                        token = "paid_no";
                    } else {
                        token = chip.getText().toString().toLowerCase(Locale.ROOT);
                    }
                    activeFilters.add(token);
                }
            }
            viewModel.updateFilters(activeFilters);
        });
    }
    
    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.searchOpportunities(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void showOpportunityDetails(Opportunity opportunity) {
        Toast.makeText(requireContext(), opportunity.getTitle() + "\n" + opportunity.getCompany(), Toast.LENGTH_SHORT).show();
    }
    
    private void applyToOpportunity(Opportunity opportunity) {
        viewModel.markAsApplied(opportunity);
        Toast.makeText(requireContext(), "Applied to " + opportunity.getTitle(), Toast.LENGTH_SHORT).show();
    }
    
    private void saveOpportunity(Opportunity opportunity) {
        viewModel.toggleSaveStatus(opportunity);
        Toast.makeText(requireContext(), opportunity.isSaved() ? "Saved!" : "Unsaved", Toast.LENGTH_SHORT).show();
    }

    private void shareOpportunity(Opportunity opportunity) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        String shareText = opportunity.getTitle() + " at " + opportunity.getCompany()
                + "\n\nApply here: " + opportunity.getApplyLink()
                + "\n\nShared via OpportunityHub";
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(intent, "Share via"));
    }
}
