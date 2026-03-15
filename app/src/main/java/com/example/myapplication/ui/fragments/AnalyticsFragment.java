package com.example.myapplication.ui.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.content.ContextCompat;
import com.example.myapplication.R;
import com.example.myapplication.model.Application;
import com.example.myapplication.model.InterviewProgress;
import com.example.myapplication.viewmodel.AnalyticsViewModel;
import com.example.myapplication.viewmodel.InterviewViewModel;
import com.google.android.material.button.MaterialButton;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AnalyticsFragment extends Fragment {
    
    private TextView tvTotalApplications;
    private TextView tvInterviewCount;
    private TextView tvSuccessRate;
    private TextView tvOffersCount;
    private TextView tvReadinessScore;
    private TextView tvPracticeAttempts;
    private TextView tvAvgScore;
    private MaterialButton btnExportCsv;
    private PieChart statusChart;
    private List<Application> cachedApplications = new ArrayList<>();

    private InterviewViewModel interviewViewModel;
    private AnalyticsViewModel analyticsViewModel;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupViewModels();
        loadAnalyticsData();
    }
    
    private void initializeViews(View view) {
        tvTotalApplications = view.findViewById(R.id.tvTotalApplications);
        tvInterviewCount = view.findViewById(R.id.tvInterviewCount);
        tvSuccessRate = view.findViewById(R.id.tvSuccessRate);
        tvOffersCount = view.findViewById(R.id.tvOffersCount);
        tvReadinessScore = view.findViewById(R.id.tvReadinessScore);
        tvPracticeAttempts = view.findViewById(R.id.tvPracticeAttempts);
        tvAvgScore = view.findViewById(R.id.tvAvgScore);
        btnExportCsv = view.findViewById(R.id.btnExportCsv);
        statusChart = view.findViewById(R.id.pieChartStatus);

        btnExportCsv.setOnClickListener(v -> exportApplicationsToCsv());
    }
    
    private void setupViewModels() {
        interviewViewModel = new ViewModelProvider(this).get(InterviewViewModel.class);
        analyticsViewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);

        // Observe analytics application list for chart/status distribution.
        analyticsViewModel.getApplicationList().observe(getViewLifecycleOwner(), applications -> {
            if (applications != null) {
                cachedApplications = applications;
                updateApplicationStats(applications);
            }
        });

        analyticsViewModel.getTotalApplications().observe(getViewLifecycleOwner(), total -> {
            tvTotalApplications.setText(String.valueOf(total != null ? total : 0));
        });

        analyticsViewModel.getInterviewsScheduled().observe(getViewLifecycleOwner(), interviews -> {
            tvInterviewCount.setText(String.valueOf(interviews != null ? interviews : 0));
        });

        analyticsViewModel.getOffersReceived().observe(getViewLifecycleOwner(), offers -> {
            tvOffersCount.setText(String.valueOf(offers != null ? offers : 0));
        });

        analyticsViewModel.getInterviewReadiness().observe(getViewLifecycleOwner(), readiness -> {
            double value = readiness != null ? readiness : 0.0;
            tvReadinessScore.setText(String.format("%.0f%%", value));
        });

        analyticsViewModel.getPracticeAttempts().observe(getViewLifecycleOwner(), attempts -> {
            tvPracticeAttempts.setText(String.valueOf(attempts != null ? attempts : 0));
        });

        analyticsViewModel.getAverageInterviewScore().observe(getViewLifecycleOwner(), avg -> {
            double value = avg != null ? avg : 0.0;
            tvAvgScore.setText(String.format("%.1f/100", value));
        });
        
        // Observe interview data
        interviewViewModel.getUserProgress().observe(getViewLifecycleOwner(), progress -> {
            if (progress != null) {
                updateInterviewStats(progress);
            }
        });
        
        // Observe analytics data
        analyticsViewModel.getSuccessRate().observe(getViewLifecycleOwner(), rate -> {
            if (rate != null) {
                tvSuccessRate.setText(String.format("%.0f%%", rate));
            }
        });
    }
    
    private void loadAnalyticsData() {
        interviewViewModel.loadUserProgress();
        interviewViewModel.loadUserStatistics();
        analyticsViewModel.loadAnalytics();
    }
    
    private void updateApplicationStats(List<Application> applications) {
        int total = applications.size();
        tvTotalApplications.setText(String.valueOf(total));
        
        long interviewCount = applications.stream()
            .filter(app -> "interview".equals(app.getStatus()))
            .count();
        tvInterviewCount.setText(String.valueOf(interviewCount));
        
        long offerCount = applications.stream()
            .filter(app -> "accepted".equals(app.getStatus()))
            .count();
        tvOffersCount.setText(String.valueOf(offerCount));

        updateStatusChart(applications);
        
        // Calculate success rate
        if (total > 0) {
            double successRate = (offerCount * 100.0) / total;
            tvSuccessRate.setText(String.format("%.0f%%", successRate));
        } else {
            tvSuccessRate.setText("0%");
        }
    }
    
    private void updateInterviewStats(List<InterviewProgress> attempts) {
        int attemptCount = attempts.size();
        tvPracticeAttempts.setText(String.valueOf(attemptCount));
        
        if (attemptCount > 0) {
            // Calculate average score and readiness
            double totalScore = 0.0;
            for (InterviewProgress attempt : attempts) {
                totalScore += attempt.getScore();
            }
            double avgScore = totalScore / attemptCount;
            int readiness = Math.min((attemptCount * 10) + (int) (avgScore / 2), 100);
            
            tvReadinessScore.setText(readiness + "%");
            tvAvgScore.setText(String.format("%.1f/100", avgScore));
        } else {
            tvReadinessScore.setText("0%");
            tvAvgScore.setText("0.0/100");
        }
    }

    private void updateStatusChart(List<Application> applications) {
        int appliedCount = 0;
        int interviewCount = 0;
        int rejectedCount = 0;
        int acceptedCount = 0;

        for (Application app : applications) {
            String status = app.getStatus();
            if ("applied".equals(status)) {
                appliedCount++;
            } else if ("interview".equals(status)) {
                interviewCount++;
            } else if ("rejected".equals(status)) {
                rejectedCount++;
            } else if ("accepted".equals(status)) {
                acceptedCount++;
            }
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        if (appliedCount > 0) {
            entries.add(new PieEntry(appliedCount, "Applied"));
            colors.add(ContextCompat.getColor(requireContext(), R.color.chip_applied));
        }
        if (interviewCount > 0) {
            entries.add(new PieEntry(interviewCount, "Interview"));
            colors.add(ContextCompat.getColor(requireContext(), R.color.chip_interview));
        }
        if (rejectedCount > 0) {
            entries.add(new PieEntry(rejectedCount, "Rejected"));
            colors.add(ContextCompat.getColor(requireContext(), R.color.chip_rejected));
        }
        if (acceptedCount > 0) {
            entries.add(new PieEntry(acceptedCount, "Accepted"));
            colors.add(ContextCompat.getColor(requireContext(), R.color.chip_accepted));
        }

        if (entries.isEmpty()) {
            statusChart.clear();
            statusChart.setCenterText("No data");
            statusChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.on_background));

        PieData data = new PieData(dataSet);
        statusChart.setData(data);
        statusChart.setUsePercentValues(false);
        statusChart.getDescription().setEnabled(false);
        statusChart.setDrawEntryLabels(true);
        statusChart.setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.on_background));
        statusChart.setCenterText("Status Breakdown");
        statusChart.setCenterTextSize(14f);
        statusChart.invalidate();
    }

    private void exportApplicationsToCsv() {
        try {
            File exportDir = requireContext().getExternalFilesDir(null);
            if (exportDir == null) {
                Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show();
                return;
            }

            File exportFile = new File(exportDir, "applications_export.csv");
            String csvContent = buildCsv(cachedApplications);

            try (FileOutputStream outputStream = new FileOutputStream(exportFile)) {
                outputStream.write(csvContent.getBytes());
                outputStream.flush();
            }

            Uri contentUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                exportFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Applications Export");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share CSV"));
            Toast.makeText(requireContext(), "Exported successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show();
        }
    }

    private String buildCsv(List<Application> applications) {
        StringBuilder builder = new StringBuilder();
        builder.append("Company,Role,Status,Applied Date,Notes\n");

        if (applications == null) {
            return builder.toString();
        }

        for (Application app : applications) {
            if (app == null) {
                continue;
            }
            builder.append(escapeCsv(app.getCompany())).append(',')
                .append(escapeCsv(app.getPosition())).append(',')
                .append(escapeCsv(app.getStatus())).append(',')
                .append(escapeCsv(formatDate(app.getAppliedDate()))).append(',')
                .append(escapeCsv(app.getNotes()))
                .append('\n');
        }

        return builder.toString();
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
