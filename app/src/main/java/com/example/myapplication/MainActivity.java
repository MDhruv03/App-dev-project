package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        
        try {
            // Create a simple test layout programmatically
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.setBackgroundColor(Color.WHITE);
            
            TextView textView = new TextView(this);
            textView.setText("✓ APP IS RUNNING!\n\nIf you see this message,\nthe app launched successfully.\n\nThe issue was with complex layouts/fragments.");
            textView.setTextSize(20);
            textView.setTextColor(Color.BLACK);
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(48, 48, 48, 48);
            
            layout.addView(textView);
            setContentView(layout);
            
            Toast.makeText(this, "MainActivity loaded successfully!", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Simple layout created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}