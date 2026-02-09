package com.example.myapplication;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.myapplication.ui.fragments.AIInterviewFragment;
import com.example.myapplication.ui.fragments.AnalyticsFragment;
import com.example.myapplication.ui.fragments.HomeFragment;
import com.example.myapplication.ui.fragments.SavedFragment;
import com.example.myapplication.ui.fragments.TrackerFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, new HomeFragment())
                .commit();
        }
        
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.navigation_saved) {
                fragment = new SavedFragment();
            } else if (itemId == R.id.navigation_tracker) {
                fragment = new TrackerFragment();
            } else if (itemId == R.id.navigation_ai) {
                fragment = new AIInterviewFragment();
            } else if (itemId == R.id.navigation_analytics) {
                fragment = new AnalyticsFragment();
            }
            
            if (fragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .commit();
                return true;
            }
            return false;
        });
    }
}
