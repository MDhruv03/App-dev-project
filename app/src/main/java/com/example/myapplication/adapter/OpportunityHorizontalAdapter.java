package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Opportunity;
import com.example.myapplication.util.DateUtils;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class OpportunityHorizontalAdapter extends RecyclerView.Adapter<OpportunityHorizontalAdapter.ViewHolder> {
    
    private List<Opportunity> opportunities;
    private OnOpportunityClickListener listener;
    
    public interface OnOpportunityClickListener {
        void onOpportunityClick(Opportunity opportunity);
    }
    
    public OpportunityHorizontalAdapter() {
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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_opportunity_horizontal, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Opportunity opportunity = opportunities.get(position);
        holder.bind(opportunity);
    }
    
    @Override
    public int getItemCount() {
        return opportunities.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        
        TextView companyInitial;
        TextView tvTitle;
        TextView tvCompany;
        TextView tvLocation;
        Chip chipType;
        TextView tvDeadline;
        TextView tvMatchBadge;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            
            companyInitial = itemView.findViewById(R.id.company_initial);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvCompany = itemView.findViewById(R.id.tv_company);
            tvLocation = itemView.findViewById(R.id.tv_location);
            chipType = itemView.findViewById(R.id.chip_type);
            tvDeadline = itemView.findViewById(R.id.tv_deadline);
            tvMatchBadge = itemView.findViewById(R.id.tv_match_badge);
        }
        
        public void bind(Opportunity opportunity) {
            // Set company initial
            if (opportunity.getCompany() != null && !opportunity.getCompany().isEmpty()) {
                companyInitial.setText(String.valueOf(opportunity.getCompany().charAt(0)));
            }
            
            // Set texts
            tvTitle.setText(opportunity.getTitle());
            tvCompany.setText(opportunity.getCompany());
            tvLocation.setText(opportunity.isRemote() ? "Remote" : opportunity.getLocation());
            
            // Set type chip
            String type = opportunity.getType();
            if (type != null) {
                chipType.setText(type.substring(0, 1).toUpperCase() + type.substring(1));
            }
            
            // Set match badge
            tvMatchBadge.setText(opportunity.getMatchPercentage() + "% Match");
            
            // Set deadline
            if (opportunity.getDeadline() != null) {
                tvDeadline.setText("Deadline: " + DateUtils.formatShortDate(opportunity.getDeadline()));
            }
            
            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOpportunityClick(opportunity);
                }
            });
        }
    }
}
