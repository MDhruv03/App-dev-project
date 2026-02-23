package com.example.myapplication.util;

import android.util.Log;

/**
 * Logger utility for consistent logging across the app
 */
public class Logger {
    
    public enum LogLevel {
        DEBUG, INFO, WARNING, ERROR, NONE
    }
    
    private static final String TAG_PREFIX = "OpportunityHub_";
    private static LogLevel currentLogLevel = LogLevel.DEBUG;
    private static final boolean DEBUG = true; // Set to false in production
    
    public static void setLogLevel(LogLevel level) {
        currentLogLevel = level;
    }
    
    public static LogLevel getLogLevel() {
        return currentLogLevel;
    }
    
    public static void d(String tag, String message) {
        if (DEBUG && currentLogLevel.ordinal() <= LogLevel.DEBUG.ordinal()) {
            Log.d(TAG_PREFIX + tag, message);
        }
    }
    
    public static void i(String tag, String message) {
        if (DEBUG && currentLogLevel.ordinal() <= LogLevel.INFO.ordinal()) {
            Log.i(TAG_PREFIX + tag, message);
        }
    }
    
    public static void w(String tag, String message) {
        Log.w(TAG_PREFIX + tag, message);
    }
    
    public static void e(String tag, String message) {
        Log.e(TAG_PREFIX + tag, message);
    }
    
    public static void e(String tag, String message, Throwable throwable) {
        Log.e(TAG_PREFIX + tag, message, throwable);
    }
    
    public static void wtf(String tag, String message) {
        Log.wtf(TAG_PREFIX + tag, message);
    }
}
