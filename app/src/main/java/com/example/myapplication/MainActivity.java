package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.myapplication.ui.fragments.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        
        try {
            setContentView(R.layout.activity_main);
            Log.d(TAG, "Layout set");
            
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            
            // Load default fragment
            if (savedInstanceState == null) {
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, new HomeFragment())
                    .commit();
            }
            
            // Setup bottom navigation
            bottomNav.setOnItemSelectedListener(item -> {
                Fragment fragment = null;
                int itemId = item.getItemId();
                
                if (itemId == R.id.navigation_home) {
                    fragment = new HomeFragment();
                } else if (itemId == R.id.navigation_saved) {
                    fragment = new com.example.myapplication.ui.fragments.SavedFragment();
                } else if (itemId == R.id.navigation_tracker) {
                    fragment = new com.example.myapplication.ui.fragments.TrackerFragment();
                } else if (itemId == R.id.navigation_ai) {
                    fragment = new com.example.myapplication.ui.fragments.AIInterviewFragment();
                } else if (itemId == R.id.navigation_analytics) {
                    fragment = new com.example.myapplication.ui.fragments.AnalyticsFragment();
                }
                
                if (fragment != null) {
                    getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.nav_host_fragment, fragment)
                        .commit();
                    return true;
                }
                return false;
            });
            
            Log.d(TAG, "MainActivity setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}