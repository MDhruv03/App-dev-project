package com.example.myapplication.ui.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.ApplicationAdapter;
import com.example.myapplication.model.Application;
import com.example.myapplication.viewmodel.ApplicationViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TrackerFragment extends Fragment {
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setBackgroundColor(android.graphics.Color.WHITE);
        layout.setPadding(48, 48, 48, 48);
        
        android.widget.TextView tv = new android.widget.TextView(requireContext());
        tv.setText("📊 Application Tracker\n\nComing Soon");
        tv.setTextSize(20);
        tv.setTextColor(android.graphics.Color.BLACK);
        tv.setGravity(android.view.Gravity.CENTER);
        layout.addView(tv);
        
        return layout;
    }
    
    /* Simplified version - full implementation coming soon */
    /*@Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupViewModel();
        setupRecyclerView();
        setupFab();
        setupFilters();
        
        viewModel.loadAllApplications();
    }
    
    private void initializeViews(View view) {
        statusChipGroup = view.findViewById(R.id.status_chip_group);
        recyclerViewApplications = view.findViewById(R.id.recycler_applications);
        fabAddApplication = view.findViewById(R.id.fab_add_application);
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ApplicationViewModel.class);
        
        viewModel.getFilteredApplications().observe(getViewLifecycleOwner(), applications -> {
            if (applications != null) {
                adapter.setApplications(applications);
            }
        });
    }
    
    private void setupRecyclerView() {
        recyclerViewApplications.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ApplicationAdapter();
        adapter.setOnApplicationClickListener(new ApplicationAdapter.OnApplicationClickListener() {
            @Override
            public void onClick(Application application) {
                showApplicationDialog(application);
            }
            
            @Override
            public void onLongClick(Application application) {
                showDeleteConfirmation(application);
            }
        });
        recyclerViewApplications.setAdapter(adapter);
    }
    
    private void setupFab() {
        fabAddApplication.setOnClickListener(v -> showAddApplicationDialog());
    }
    
    private void setupFilters() {
        statusChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                viewModel.filterByStatus("all");
                return;
            }
            
            int checkedId = checkedIds.get(0);
            Chip chip = group.findViewById(checkedId);
            if (chip != null) {
                String status = chip.getText().toString().toLowerCase();
                viewModel.filterByStatus(status);
            }
        });
    }
    
    private void showAddApplicationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_application, null);
        
        EditText etNotes = dialogView.findViewById(R.id.et_notes);
        
        builder.setView(dialogView)
            .setTitle("Add Application")
            .setPositiveButton("Add", (dialog, which) -> {
                Application application = new Application();
                application.setOpportunityId(1); // Mock
                application.setStatus("saved");
                application.setNotes(etNotes.getText().toString());
                application.setSavedAt(new Date());
                
                viewModel.addApplication(application);
                Toast.makeText(requireContext(), "Application added!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showApplicationDialog(Application application) {
        String[] statuses = {"saved", "applied", "interview", "rejected", "accepted"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Update Status")
            .setItems(statuses, (dialog, which) -> {
                application.setStatus(statuses[which]);
                if (statuses[which].equals("applied") && application.getAppliedAt() == null) {
                    application.setAppliedAt(new Date());
                }
                viewModel.updateApplication(application);
                Toast.makeText(requireContext(), "Status updated!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showDeleteConfirmation(Application application) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete Application?")
            .setMessage("This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                viewModel.deleteApplication(application);
                Toast.makeText(requireContext(), "Application deleted", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
