package com.example.myapplication.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SavedFragment extends Fragment {
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.WHITE);
        layout.setPadding(48, 48, 48, 48);
        
        TextView tv = new TextView(requireContext());
        tv.setText("💾 Saved Opportunities\n\nComing Soon");
        tv.setTextSize(20);
        tv.setTextColor(Color.BLACK);
        tv.setGravity(Gravity.CENTER);
        layout.addView(tv);
        
        return layout;
    }
}
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
            public void onApplyClick(Opportunity opportunity) {
                Toast.makeText(requireContext(),
                    "Opening application for " + opportunity.getCompany(),
                    Toast.LENGTH_SHORT).show();
                viewModel.markAsApplied(opportunity);
            }
            
            @Override
            public void onSaveClick(Opportunity opportunity) {
                viewModel.toggleSaveStatus(opportunity);
                Toast.makeText(requireContext(), "Removed from saved", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onOpportunityClick(Opportunity opportunity) {
                Toast.makeText(requireContext(),
                    opportunity.getTitle() + " at " + opportunity.getCompany(),
                    Toast.LENGTH_LONG).show();
            }
        });
        recyclerViewSaved.setAdapter(adapter);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadSavedOpportunities();
    }
}
