package com.example.myapplication.util;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

/**
 * UI utility class for common UI operations
 */
public class UIUtils {
    
    /**
     * Show a short toast message
     */
    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Show a long toast message
     */
    public static void showLongToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Show snackbar with action
     */
    public static void showSnackbar(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }
    
    /**
     * Show snackbar with action button
     */
    public static void showSnackbarWithAction(View view, String message, 
                                              String actionText, View.OnClickListener action) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setAction(actionText, action)
                .show();
    }
    
    /**
     * Hide keyboard
     */
    public static void hideKeyboard(Context context, View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) 
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
    
    /**
     * Show keyboard
     */
    public static void showKeyboard(Context context, View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) 
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                view.requestFocus();
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }
    
    /**
     * Convert dp to pixels
     */
    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    /**
     * Convert pixels to dp
     */
    public static int pxToDp(Context context, int px) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(px / density);
    }
}
