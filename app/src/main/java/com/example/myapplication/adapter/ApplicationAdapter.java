package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Application;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ApplicationAdapter extends RecyclerView.Adapter<ApplicationAdapter.ApplicationViewHolder> {
    
    private List<Application> applications;
    private OnApplicationClickListener listener;
    
    public interface OnApplicationClickListener {
        void onClick(Application application);
        void onLongClick(Application application);
    }
    
    public ApplicationAdapter() {
        this.applications = new ArrayList<>();
    }
    
    public void setApplications(List<Application> applications) {
        this.applications = applications;
        notifyDataSetChanged();
    }
    
    public void setOnApplicationClickListener(OnApplicationClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_application, parent, false);
        return new ApplicationViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ApplicationViewHolder holder, int position) {
        Application application = applications.get(position);
        holder.bind(application);
    }
    
    @Override
    public int getItemCount() {
        return applications.size();
    }
    
    class ApplicationViewHolder extends RecyclerView.ViewHolder {
        
        TextView tvOpportunityId;
        TextView tvStatus;
        Chip chipStatus;
        TextView tvAppliedDate;
        TextView tvInterviewDate;
        TextView tvNotes;
        
        public ApplicationViewHolder(@NonNull View itemView) {
            super(itemView);
            
            tvOpportunityId = itemView.findViewById(R.id.tv_opportunity_id);
            tvStatus = itemView.findViewById(R.id.tv_status);
            chipStatus = itemView.findViewById(R.id.chip_status);
            tvAppliedDate = itemView.findViewById(R.id.tv_applied_date);
            tvInterviewDate = itemView.findViewById(R.id.tv_interview_date);
            tvNotes = itemView.findViewById(R.id.tv_notes);
        }
        
        public void bind(Application application) {
            // Set opportunity ID (this would normally show company/title)
            tvOpportunityId.setText("Application #" + application.getId());
            
            // Set status
            String status = application.getStatus();
            tvStatus.setText(getStatusText(status));
            chipStatus.setText(status.toUpperCase());
            chipStatus.setChipBackgroundColorResource(getStatusColor(status));
            
            // Set dates
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            if (application.getAppliedAt() != null) {
                tvAppliedDate.setText("Applied: " + dateFormat.format(application.getAppliedAt()));
            } else {
                tvAppliedDate.setText("Saved: " + dateFormat.format(application.getSavedAt()));
            }
            
            // Interview date
            if (application.getInterviewDate() != null) {
                tvInterviewDate.setVisibility(View.VISIBLE);
                tvInterviewDate.setText("Interview: " + dateFormat.format(application.getInterviewDate()));
            } else {
                tvInterviewDate.setVisibility(View.GONE);
            }
            
            // Notes
            if (application.getNotes() != null && !application.getNotes().isEmpty()) {
                tvNotes.setVisibility(View.VISIBLE);
                tvNotes.setText(application.getNotes());
            } else {
                tvNotes.setVisibility(View.GONE);
            }
            
            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(application);
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onLongClick(application);
                }
                return true;
            });
        }
        
        private String getStatusText(String status) {
            switch (status) {
                case "saved": return "Saved for later";
                case "applied": return "Application submitted";
                case "interview": return "Interview scheduled";
                case "rejected": return "Not selected";
                case "accepted": return "Offer received!";
                default: return status;
            }
        }
        
        private int getStatusColor(String status) {
            switch (status) {
                case "saved": return R.color.chip_saved;
                case "applied": return R.color.chip_applied;
                case "interview": return R.color.chip_interview;
                case "rejected": return R.color.chip_rejected;
                case "accepted": return R.color.chip_accepted;
                default: return R.color.chip_default;
            }
        }
    }
}
