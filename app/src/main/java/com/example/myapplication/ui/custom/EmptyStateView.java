package com.example.myapplication.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * Custom empty state view
 */
public class EmptyStateView extends LinearLayout {
    
    private TextView titleTextView;
    private TextView messageTextView;
    
    public EmptyStateView(Context context) {
        super(context);
        init(context);
    }
    
    public EmptyStateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public EmptyStateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        setOrientation(VERTICAL);
        setGravity(android.view.Gravity.CENTER);
        
        titleTextView = new TextView(context);
        titleTextView.setTextSize(20);
        titleTextView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        titleTextView.setPadding(32, 32, 32, 8);
        
        messageTextView = new TextView(context);
        messageTextView.setTextSize(14);
        messageTextView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        messageTextView.setPadding(32, 8, 32, 32);
        messageTextView.setAlpha(0.7f);
        
        addView(titleTextView);
        addView(messageTextView);
    }
    
    public void setTitle(String title) {
        titleTextView.setText(title);
    }
    
    public void setMessage(String message) {
        messageTextView.setText(message);
    }
    
    public void show() {
        setVisibility(VISIBLE);
    }
    
    public void hide() {
        setVisibility(GONE);
    }
}
