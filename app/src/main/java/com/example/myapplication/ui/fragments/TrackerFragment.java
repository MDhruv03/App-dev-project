package com.example.myapplication.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.text.DateFormat;
import java.util.Date;

public class TrackerFragment extends Fragment {
    
    private ChipGroup statusChipGroup;
    private TextView tvPendingCount;
    private TextView tvPendingBreakdown;
    private TextView tvLastSynced;
    private MaterialButton btnSyncNow;
    private RecyclerView recyclerViewApplications;
    private View layoutEmptyState;
    private ExtendedFloatingActionButton fabAddApplication;
    private ApplicationViewModel viewModel;
    private ApplicationAdapter adapter;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tracker, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupRecyclerView();
        setupViewModel();
        setupFab();
        setupFilters();
        
        viewModel.loadAllApplications();
    }
    
    private void initializeViews(View view) {
        statusChipGroup = view.findViewById(R.id.status_chip_group);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvPendingBreakdown = view.findViewById(R.id.tvPendingBreakdown);
        tvLastSynced = view.findViewById(R.id.tvLastSynced);
        btnSyncNow = view.findViewById(R.id.btnSyncNow);
        recyclerViewApplications = view.findViewById(R.id.recycler_applications);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        fabAddApplication = view.findViewById(R.id.fab_add_application);
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ApplicationViewModel.class);
        
        viewModel.getFilteredApplications().observe(getViewLifecycleOwner(), applications -> {
            boolean isEmpty = applications == null || applications.isEmpty();
            layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recyclerViewApplications.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            if (applications != null) {
                adapter.setApplications(applications);
            }
        });

        viewModel.getPendingSyncCount().observe(getViewLifecycleOwner(), count -> {
            if (count == null || count <= 0) {
                tvPendingCount.setVisibility(View.GONE);
                tvPendingBreakdown.setVisibility(View.GONE);
            } else {
                // pendingOps list size is represented by the repository-provided pending count
                tvPendingCount.setVisibility(View.VISIBLE);
                tvPendingCount.setText(getString(R.string.pending_sync_count, count));
            }
        });

        viewModel.getPendingSyncBreakdown().observe(getViewLifecycleOwner(), breakdown -> {
            if (breakdown == null || breakdown.trim().isEmpty()) {
                tvPendingBreakdown.setVisibility(View.GONE);
            } else {
                tvPendingBreakdown.setVisibility(View.VISIBLE);
                tvPendingBreakdown.setText(breakdown);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), syncing -> {
            boolean isSyncing = syncing != null && syncing;
            btnSyncNow.setEnabled(!isSyncing);
            btnSyncNow.setText(isSyncing ? getString(R.string.syncing) : getString(R.string.sync_now));
        });

        viewModel.getLastSyncedAt().observe(getViewLifecycleOwner(), lastSynced -> {
            if (lastSynced == null || lastSynced <= 0) {
                tvLastSynced.setText(getString(R.string.never_synced));
                return;
            }

            String formatted = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(new Date(lastSynced));
            tvLastSynced.setText(getString(R.string.last_synced_format, formatted));
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
        btnSyncNow.setOnClickListener(v -> viewModel.syncNow());
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
                application.setOpportunityId(1);
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
        ApplicationDetailBottomSheet bottomSheet = ApplicationDetailBottomSheet.newInstance(application);
        bottomSheet.show(getChildFragmentManager(), "application_detail_bottom_sheet");
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
