package com.example.myapplication.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 2000L;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable navigateToMain = () -> {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.postDelayed(navigateToMain, SPLASH_DELAY_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(navigateToMain);
        super.onDestroy();
    }
}