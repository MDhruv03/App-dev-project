package com.example.myapplication.ui.fragments;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.model.Application;
import com.example.myapplication.viewmodel.ApplicationViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

public class ApplicationDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_APPLICATION = "arg_application";

    private Application application;
    private ApplicationViewModel viewModel;

    public static ApplicationDetailBottomSheet newInstance(Application application) {
        ApplicationDetailBottomSheet sheet = new ApplicationDetailBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_APPLICATION, application);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_application_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) {
            dismiss();
            return;
        }

        Serializable serializable = args.getSerializable(ARG_APPLICATION);
        if (!(serializable instanceof Application)) {
            dismiss();
            return;
        }

        application = (Application) serializable;
        viewModel = new ViewModelProvider(requireActivity()).get(ApplicationViewModel.class);

        bindContent(view);
        bindStatusActions(view);
        bindDeleteAction(view);
    }

    private void bindContent(View root) {
        TextView tvTitle = root.findViewById(R.id.tvBottomSheetTitle);
        TextView tvNotes = root.findViewById(R.id.tvBottomSheetNotes);
        TextView tvDate = root.findViewById(R.id.tvBottomSheetDate);

        String company = safe(application.getCompany());
        String role = safe(application.getPosition());
        tvTitle.setText((company + " - " + role).trim());

        String notes = safe(application.getNotes());
        if (notes.isEmpty()) {
            tvNotes.setVisibility(View.GONE);
        } else {
            tvNotes.setVisibility(View.VISIBLE);
            tvNotes.setText(notes);
        }

        Date appliedDate = application.getAppliedDate();
        if (appliedDate == null) {
            tvDate.setText("Applied date: -");
        } else {
            String formatted = DateFormat.getDateInstance(DateFormat.MEDIUM).format(appliedDate);
            tvDate.setText("Applied date: " + formatted);
        }

        updateTimeline(root);
    }

    private void bindStatusActions(View root) {
        MaterialButton btnSaved = root.findViewById(R.id.btnStatusSaved);
        MaterialButton btnApplied = root.findViewById(R.id.btnStatusApplied);
        MaterialButton btnInterview = root.findViewById(R.id.btnStatusInterview);
        MaterialButton btnRejected = root.findViewById(R.id.btnStatusRejected);
        MaterialButton btnAccepted = root.findViewById(R.id.btnStatusAccepted);

        btnSaved.setOnClickListener(v -> updateStatusAndDismiss("saved"));
        btnApplied.setOnClickListener(v -> updateStatusAndDismiss("applied"));
        btnInterview.setOnClickListener(v -> updateStatusAndDismiss("interview"));
        btnRejected.setOnClickListener(v -> updateStatusAndDismiss("rejected"));
        btnAccepted.setOnClickListener(v -> updateStatusAndDismiss("accepted"));
    }

    private void bindDeleteAction(View root) {
        MaterialButton btnDelete = root.findViewById(R.id.btnBottomSheetDelete);
        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
            .setTitle("Delete Application?")
            .setMessage("This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                viewModel.deleteApplication(application);
                dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show());
    }

    private void updateStatusAndDismiss(String status) {
        application.setStatus(status);
        if ("applied".equals(status) && application.getAppliedAt() == null) {
            application.setAppliedAt(new Date());
        }
        viewModel.updateApplication(application);
        dismiss();
    }

    private void updateTimeline(View root) {
        View savedCircle = root.findViewById(R.id.stepSavedCircle);
        View appliedCircle = root.findViewById(R.id.stepAppliedCircle);
        View interviewCircle = root.findViewById(R.id.stepInterviewCircle);
        View rejectedCircle = root.findViewById(R.id.stepRejectedCircle);
        View acceptedCircle = root.findViewById(R.id.stepAcceptedCircle);

        TextView savedLabel = root.findViewById(R.id.stepSavedLabel);
        TextView appliedLabel = root.findViewById(R.id.stepAppliedLabel);
        TextView interviewLabel = root.findViewById(R.id.stepInterviewLabel);
        TextView rejectedLabel = root.findViewById(R.id.stepRejectedLabel);
        TextView acceptedLabel = root.findViewById(R.id.stepAcceptedLabel);

        String status = safe(application.getStatus()).toLowerCase();
        int milestone = getMilestone(status);

        applyStepState(savedCircle, savedLabel, milestone >= 0, "saved".equals(status));
        applyStepState(appliedCircle, appliedLabel, milestone >= 1, "applied".equals(status));
        applyStepState(interviewCircle, interviewLabel, milestone >= 2, "interview".equals(status));
        applyStepState(rejectedCircle, rejectedLabel, "rejected".equals(status), "rejected".equals(status));
        applyStepState(acceptedCircle, acceptedLabel, "accepted".equals(status), "accepted".equals(status));
    }

    private int getMilestone(String status) {
        if ("saved".equals(status)) {
            return 0;
        }
        if ("applied".equals(status)) {
            return 1;
        }
        if ("interview".equals(status)) {
            return 2;
        }
        if ("rejected".equals(status) || "accepted".equals(status)) {
            return 3;
        }
        return 0;
    }

    private void applyStepState(View circle, TextView label, boolean reached, boolean current) {
        int primary = ContextCompat.getColor(requireContext(), R.color.primary);
        int outline = ContextCompat.getColor(requireContext(), R.color.outline);
        int onSurface = ContextCompat.getColor(requireContext(), R.color.on_surface);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        if (reached) {
            drawable.setColor(primary);
            drawable.setStroke(0, primary);
        } else {
            drawable.setColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));
            drawable.setStroke(2, outline);
        }
        circle.setBackground(drawable);

        if (current) {
            label.setTextColor(primary);
            label.setTypeface(label.getTypeface(), Typeface.BOLD);
        } else {
            label.setTextColor(onSurface);
            label.setTypeface(label.getTypeface(), Typeface.NORMAL);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}