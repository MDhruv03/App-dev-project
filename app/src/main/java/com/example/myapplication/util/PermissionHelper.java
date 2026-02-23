package com.example.myapplication.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Permissions helper for runtime permission requests
 */
public class PermissionHelper {
    
    // Common permissions
    public static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    public static final String PERMISSION_READ_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
    public static final String PERMISSION_WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    public static final String PERMISSION_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
    
    /**
     * Check if permission is granted
     */
    public static boolean isPermissionGranted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Check if all permissions are granted
     */
    public static boolean arePermissionsGranted(Context context, String... permissions) {
        for (String permission : permissions) {
            if (!isPermissionGranted(context, permission)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Request single permission
     */
    public static void requestPermission(Activity activity, String permission, int requestCode) {
        ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
    }
    
    /**
     * Request multiple permissions
     */
    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }
    
    /**
     * Check if should show rationale
     */
    public static boolean shouldShowRationale(Activity activity, String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }
    
    /**
     * Get denied permissions from array
     */
    public static List<String> getDeniedPermissions(Context context, String... permissions) {
        List<String> denied = new ArrayList<>();
        for (String permission : permissions) {
            if (!isPermissionGranted(context, permission)) {
                denied.add(permission);
            }
        }
        return denied;
    }
    
    /**
     * Handle permission result
     */
    public static boolean handlePermissionResult(String[] permissions, int[] grantResults) {
        if (grantResults.length == 0) {
            return false;
        }
        
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Request storage permissions
     */
    public static void requestStoragePermissions(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ doesn't need storage permissions for app-specific directories
            // Only request if accessing shared storage
            requestPermission(activity, PERMISSION_READ_STORAGE, requestCode);
        } else {
            requestPermissions(activity, new String[]{
                PERMISSION_READ_STORAGE,
                PERMISSION_WRITE_STORAGE
            }, requestCode);
        }
    }
    
    /**
     * Request notification permission (Android 13+)
     */
    public static void requestNotificationPermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermission(activity, PERMISSION_NOTIFICATIONS, requestCode);
        }
    }
    
    /**
     * Check if notification permission is granted
     */
    public static boolean isNotificationPermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return isPermissionGranted(context, PERMISSION_NOTIFICATIONS);
        }
        return true; // No permission needed before Android 13
    }
    
    /**
     * Get permission name for display
     */
    public static String getPermissionName(String permission) {
        if (PERMISSION_CAMERA.equals(permission)) {
            return "Camera";
        } else if (PERMISSION_READ_STORAGE.equals(permission) || PERMISSION_WRITE_STORAGE.equals(permission)) {
            return "Storage";
        } else if (PERMISSION_NOTIFICATIONS.equals(permission)) {
            return "Notifications";
        } else {
            return "Unknown";
        }
    }
    
    /**
     * Get permission rationale message
     */
    public static String getPermissionRationale(String permission) {
        if (PERMISSION_CAMERA.equals(permission)) {
            return "Camera permission is required to scan documents and take profile photos.";
        } else if (PERMISSION_READ_STORAGE.equals(permission) || PERMISSION_WRITE_STORAGE.equals(permission)) {
            return "Storage permission is required to save and load files.";
        } else if (PERMISSION_NOTIFICATIONS.equals(permission)) {
            return "Notification permission is required to receive deadline reminders and interview alerts.";
        } else {
            return "This permission is required for the app to function properly.";
        }
    }
}
