package com.example.myapplication.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * Custom loading view
 */
public class LoadingView extends LinearLayout {
    
    private ProgressBar progressBar;
    private TextView messageTextView;
    
    public LoadingView(Context context) {
        super(context);
        init(context);
    }
    
    public LoadingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public LoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        setOrientation(VERTICAL);
        setGravity(android.view.Gravity.CENTER);
        
        progressBar = new ProgressBar(context);
        LayoutParams progressParams = new LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        );
        progressParams.setMargins(0, 32, 0, 16);
        progressBar.setLayoutParams(progressParams);
        
        messageTextView = new TextView(context);
        messageTextView.setText("Loading...");
        messageTextView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        LayoutParams textParams = new LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(0, 0, 0, 32);
        messageTextView.setLayoutParams(textParams);
        
        addView(progressBar);
        addView(messageTextView);
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
