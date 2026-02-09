package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.myapplication.ui.fragments.AIInterviewFragment;
import com.example.myapplication.ui.fragments.AnalyticsFragment;
import com.example.myapplication.ui.fragments.HomeFragment;
import com.example.myapplication.ui.fragments.SavedFragment;
import com.example.myapplication.ui.fragments.TestFragment;
import com.example.myapplication.ui.fragments.TrackerFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final boolean USE_TEST_FRAGMENT = true; // Set to false to use normal HomeFragment
    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        
        try {
            setContentView(R.layout.activity_main);
            Log.d(TAG, "Layout inflated successfully");
            
            initializeViews();
            setupBottomNavigation();
            
            // Load default fragment
            if (savedInstanceState == null) {
                if (USE_TEST_FRAGMENT) {
                    Log.d(TAG, "Loading TestFragment for diagnostics");
                    loadFragment(new TestFragment());
                } else {
                    loadFragment(new HomeFragment());
                }
            }
            Log.d(TAG, "onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView == null) {
            Log.e(TAG, "Bottom navigation view is null!");
            Toast.makeText(this, "Error: Bottom navigation not found", Toast.LENGTH_LONG).show();
            return;
        }
        fragmentManager = getSupportFragmentManager();
        Log.d(TAG, "Views initialized successfully");
    }
    
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_saved) {
                selectedFragment = new SavedFragment();
            } else if (itemId == R.id.navigation_tracker) {
                selectedFragment = new TrackerFragment();
            } else if (itemId == R.id.navigation_ai) {
                selectedFragment = new AIInterviewFragment();
            } else if (itemId == R.id.navigation_analytics) {
                selectedFragment = new AnalyticsFragment();
            }
            
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }
    
    private void loadFragment(Fragment fragment) {
        try {
            Log.d(TAG, "Loading fragment: " + fragment.getClass().getSimpleName());
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.nav_host_fragment, fragment);
            transaction.commitAllowingStateLoss();
            Log.d(TAG, "Fragment loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error loading fragment", e);
            Toast.makeText(this, "Error loading fragment: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}