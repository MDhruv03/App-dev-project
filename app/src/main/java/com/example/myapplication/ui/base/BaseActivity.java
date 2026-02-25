package com.example.myapplication.ui.base;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.util.Logger;
import com.example.myapplication.util.UIUtils;

/**
 * Base activity with common functionality
 */
public abstract class BaseActivity extends AppCompatActivity {
    
    private static final String TAG = "BaseActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());
        
        Logger.d(TAG, getClass().getSimpleName() + " onCreate");
        
        setupToolbar();
        initializeViews();
        observeData();
    }
    
    /**
     * Get layout resource ID
     */
    @LayoutRes
    protected abstract int getLayoutResourceId();
    
    /**
     * Initialize views
     */
    protected abstract void initializeViews();
    
    /**
     * Observe LiveData and other data sources
     */
    protected void observeData() {
        // Override in subclasses if needed
    }
    
    /**
     * Setup toolbar if present
     */
    protected void setupToolbar() {
        Toolbar toolbar = findToolbar();
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }
    
    /**
     * Find toolbar - override if using different toolbar ID
     */
    protected Toolbar findToolbar() {
        return null; // Override in subclasses
    }
    
    /**
     * Enable back button in toolbar
     */
    protected void enableBackButton() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    
    /**
     * Set toolbar title
     */
    protected void setToolbarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Show loading indicator
     */
    protected void showLoading() {
        // Override in subclasses
    }
    
    /**
     * Hide loading indicator
     */
    protected void hideLoading() {
        // Override in subclasses
    }
    
    /**
     * Show error message
     */
    protected void showError(String message) {
        UIUtils.showToast(this, message);
    }
    
    /**
     * Show success message
     */
    protected void showSuccess(String message) {
        UIUtils.showToast(this, message);
    }
    
    /**
     * Hide keyboard
     */
    protected void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) view = new View(this);
        UIUtils.hideKeyboard(this, view);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        Logger.d(TAG, getClass().getSimpleName() + " onStart");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Logger.d(TAG, getClass().getSimpleName() + " onResume");
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Logger.d(TAG, getClass().getSimpleName() + " onPause");
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Logger.d(TAG, getClass().getSimpleName() + " onStop");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.d(TAG, getClass().getSimpleName() + " onDestroy");
    }
}
