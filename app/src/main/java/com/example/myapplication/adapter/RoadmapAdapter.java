package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.RoadmapTopic;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class RoadmapAdapter extends RecyclerView.Adapter<RoadmapAdapter.RoadmapViewHolder> {

    public interface OnTopicActionListener {
        void onResourceClick(RoadmapTopic topic);

        void onCompletionChanged(RoadmapTopic topic, boolean completed);
    }

    private final List<RoadmapTopic> topics = new ArrayList<>();
    private final OnTopicActionListener listener;

    public RoadmapAdapter(OnTopicActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<RoadmapTopic> newTopics) {
        topics.clear();
        if (newTopics != null) {
            topics.addAll(newTopics);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoadmapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_roadmap_topic, parent, false);
        return new RoadmapViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoadmapViewHolder holder, int position) {
        RoadmapTopic topic = topics.get(position);

        holder.tvTitle.setText(topic.getTitle());
        holder.tvDescription.setText(topic.getDescription());
        holder.chipDays.setText("~" + topic.getEstimatedDays() + " days");
        holder.chipCategory.setText(topic.getCategory());

        holder.cbCompleted.setOnCheckedChangeListener(null);
        holder.cbCompleted.setChecked(topic.isCompleted());
        holder.cbCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            topic.setCompleted(isChecked);
            if (listener != null) {
                listener.onCompletionChanged(topic, isChecked);
            }
        });

        holder.btnResource.setOnClickListener(v -> {
            if (listener != null) {
                listener.onResourceClick(topic);
            }
        });

        int colorRes;
        if (topic.getPriority() == 1) {
            colorRes = android.R.color.holo_red_dark;
        } else if (topic.getPriority() == 2) {
            colorRes = android.R.color.holo_orange_dark;
        } else {
            colorRes = android.R.color.holo_green_dark;
        }
        holder.priorityBar.setBackgroundResource(colorRes);
    }

    @Override
    public int getItemCount() {
        return topics.size();
    }

    static class RoadmapViewHolder extends RecyclerView.ViewHolder {
        View priorityBar;
        TextView tvTitle;
        TextView tvDescription;
        Chip chipDays;
        Chip chipCategory;
        MaterialButton btnResource;
        CheckBox cbCompleted;

        RoadmapViewHolder(@NonNull View itemView) {
            super(itemView);
            priorityBar = itemView.findViewById(R.id.viewPriorityBar);
            tvTitle = itemView.findViewById(R.id.tvRoadmapTitle);
            tvDescription = itemView.findViewById(R.id.tvRoadmapDescription);
            chipDays = itemView.findViewById(R.id.chipEstimatedDays);
            chipCategory = itemView.findViewById(R.id.chipCategory);
            btnResource = itemView.findViewById(R.id.btnOpenResource);
            cbCompleted = itemView.findViewById(R.id.cbCompleted);
        }
    }
}
