package com.example.myapplication;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.myapplication.util.DiagnosticLogger;
import com.example.myapplication.util.PreferencesManager;
import com.example.myapplication.ui.fragments.AIInterviewFragment;
import com.example.myapplication.ui.fragments.AnalyticsFragment;
import com.example.myapplication.ui.fragments.HomeFragment;
import com.example.myapplication.ui.fragments.ProfileFragment;
import com.example.myapplication.ui.fragments.SavedFragment;
import com.example.myapplication.ui.fragments.TrackerFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_DARK_MODE = "dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DiagnosticLogger.log("main_activity_onCreate_start");
        try {
            setContentView(R.layout.activity_main);

            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            ImageButton btnToggleDarkMode = findViewById(R.id.btnToggleDarkMode);
            PreferencesManager preferencesManager = new PreferencesManager(this);

            btnToggleDarkMode.setOnClickListener(v -> {
                int currentMode = getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                boolean currentlyDark = currentMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                int targetMode = currentlyDark
                    ? AppCompatDelegate.MODE_NIGHT_NO
                    : AppCompatDelegate.MODE_NIGHT_YES;

                preferencesManager.putBoolean(KEY_DARK_MODE, !currentlyDark);
                AppCompatDelegate.setDefaultNightMode(targetMode);
            });

            if (savedInstanceState == null) {
                DiagnosticLogger.log("main_activity_default_fragment_home");
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
                } else if (itemId == R.id.navigation_profile) {
                    fragment = new ProfileFragment();
                }

                if (fragment != null) {
                    DiagnosticLogger.log("main_activity_nav_select:" + itemId);
                    getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, fragment)
                        .commit();
                    return true;
                }
                return false;
            });
            DiagnosticLogger.log("main_activity_onCreate_complete");
        } catch (Throwable throwable) {
            DiagnosticLogger.logError("main_activity_onCreate_failure", throwable);
            throw throwable;
        }
    }
}
