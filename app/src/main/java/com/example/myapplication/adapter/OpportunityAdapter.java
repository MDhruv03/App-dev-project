package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Opportunity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OpportunityAdapter extends RecyclerView.Adapter<OpportunityAdapter.OpportunityViewHolder> {
    
    private List<Opportunity> opportunities;
    private OnOpportunityClickListener listener;
    
    public interface OnOpportunityClickListener {
        void onApplyClick(Opportunity opportunity);
        void onSaveClick(Opportunity opportunity);
        void onOpportunityClick(Opportunity opportunity);
    }
    
    public OpportunityAdapter() {
        this.opportunities = new ArrayList<>();
    }
    
    public void setOpportunities(List<Opportunity> opportunities) {
        this.opportunities = opportunities;
        notifyDataSetChanged();
    }
    
    public void setOnOpportunityClickListener(OnOpportunityClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public OpportunityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_opportunity, parent, false);
        return new OpportunityViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull OpportunityViewHolder holder, int position) {
        Opportunity opportunity = opportunities.get(position);
        holder.bind(opportunity);
    }
    
    @Override
    public int getItemCount() {
        return opportunities.size();
    }
    
    class OpportunityViewHolder extends RecyclerView.ViewHolder {
        
        TextView companyInitial;
        TextView tvTitle;
        TextView tvCompany;
        Chip chipType;
        Chip chipLocation;
        Chip chipPaid;
        TextView tvMatchPercentage;
        TextView tvDeadline;
        MaterialButton btnApply;
        ImageButton btnSave;
        
        public OpportunityViewHolder(@NonNull View itemView) {
            super(itemView);
            
            companyInitial = itemView.findViewById(R.id.company_initial);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvCompany = itemView.findViewById(R.id.tv_company);
            chipType = itemView.findViewById(R.id.chip_type);
            chipLocation = itemView.findViewById(R.id.chip_location);
            chipPaid = itemView.findViewById(R.id.chip_paid);
            tvMatchPercentage = itemView.findViewById(R.id.tv_match_percentage);
            tvDeadline = itemView.findViewById(R.id.tv_deadline);
            btnApply = itemView.findViewById(R.id.btn_apply);
            btnSave = itemView.findViewById(R.id.btn_save);
        }
        
        public void bind(Opportunity opportunity) {
            // Set company initial
            if (opportunity.getCompany() != null && !opportunity.getCompany().isEmpty()) {
                companyInitial.setText(String.valueOf(opportunity.getCompany().charAt(0)));
            }
            
            // Set texts
            tvTitle.setText(opportunity.getTitle());
            tvCompany.setText(opportunity.getCompany());
            
            // Set chips
            chipType.setText(opportunity.getType());
            chipLocation.setText(opportunity.isRemote() ? "Remote" : opportunity.getLocation());
            chipPaid.setVisibility(opportunity.isPaid() ? View.VISIBLE : View.GONE);
            
            // Set match percentage
            tvMatchPercentage.setText(opportunity.getMatchPercentage() + "%");
            
            // Set deadline
            if (opportunity.getDeadline() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvDeadline.setText("Deadline: " + dateFormat.format(opportunity.getDeadline()));
            }
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOpportunityClick(opportunity);
                }
            });
            
            btnApply.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onApplyClick(opportunity);
                }
            });
            
            btnSave.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSaveClick(opportunity);
                }
            });
        }
    }
}
