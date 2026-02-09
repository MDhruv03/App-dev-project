package com.example.myapplication.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#F9FAFB"));
        layout.setPadding(48, 48, 48, 48);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));
        
        TextView title = new TextView(requireContext());
        title.setText("OpportunityHub");
        title.setTextSize(28);
        title.setTextColor(Color.parseColor("#111827"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 32);
        layout.addView(title);
        
        TextView message = new TextView(requireContext());
        message.setText("Home Fragment\n\nApp is working!\nUI will be enhanced next.");
        message.setTextSize(16);
        message.setTextColor(Color.parseColor("#374151"));
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 0, 0, 24);
        layout.addView(message);
        
        ProgressBar progressBar = new ProgressBar(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        progressBar.setLayoutParams(params);
        layout.addView(progressBar);
        
        return layout;
    }
}
