package com.example.myapplication.ui.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.util.Logger;
import com.example.myapplication.util.UIUtils;

/**
 * Base fragment with common functionality
 */
public abstract class BaseFragment extends Fragment {
    
    private static final String TAG = "BaseFragment";
    
    protected View rootView;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(getLayoutResourceId(), container, false);
        }
        
        Logger.d(TAG, getClass().getSimpleName() + " onCreateView");
        
        return rootView;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
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
    protected abstract void initializeViews(View view);
    
    /**
     * Observe LiveData and other data sources
     */
    protected void observeData() {
        // Override in subclasses if needed
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
        if (getContext() != null) {
            UIUtils.showToast(getContext(), message);
        }
    }
    
    /**
     * Show success message
     */
    protected void showSuccess(String message) {
        if (getContext() != null) {
            UIUtils.showToast(getContext(), message);
        }
    }
    
    /**
     * Hide keyboard
     */
    protected void hideKeyboard() {
        if (getActivity() != null) {
            UIUtils.hideKeyboard(getActivity());
        }
    }
    
    /**
     * Set toolbar title
     */
    protected void setToolbarTitle(String title) {
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).setToolbarTitle(title);
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Logger.d(TAG, getClass().getSimpleName() + " onStart");
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Logger.d(TAG, getClass().getSimpleName() + " onResume");
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Logger.d(TAG, getClass().getSimpleName() + " onPause");
    }
    
    @Override
    public void onStop() {
        super.onStop();
        Logger.d(TAG, getClass().getSimpleName() + " onStop");
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Logger.d(TAG, getClass().getSimpleName() + " onDestroyView");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d(TAG, getClass().getSimpleName() + " onDestroy");
    }
}
