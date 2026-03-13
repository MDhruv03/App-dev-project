package com.example.myapplication.util;

import android.content.Context;
import android.os.Build;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Global exception handler for crash reporting
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    
    private static final String TAG = "CrashHandler";
    private static final String CRASH_DIR = "crashes";
    
    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;
    
    public CrashHandler(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }
    
    /**
     * Install crash handler
     */
    public static void install(Context context) {
        CrashHandler handler = new CrashHandler(context);
        Thread.setDefaultUncaughtExceptionHandler(handler);
        Logger.d(TAG, "Crash handler installed");
    }
    
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        DiagnosticLogger.logError("uncaught_exception_thread:" + (thread == null ? "unknown" : thread.getName()), throwable);
        try {
            // Log crash
            saveCrashLog(throwable);
            
            // Track crash
            AnalyticsTracker tracker = AnalyticsTracker.getInstance(context);
            tracker.trackError("crash", throwable.getMessage());
            
        } catch (Exception e) {
            Logger.e(TAG, "Error saving crash log", e);
        } finally {
            // Call default handler
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        }
    }
    
    /**
     * Save crash log to file
     */
    private void saveCrashLog(Throwable throwable) {
        try {
            // Create crash directory
            File baseDir = context.getExternalFilesDir(null);
            if (baseDir == null) {
                baseDir = context.getFilesDir();
            }
            File crashDir = new File(baseDir, CRASH_DIR);
            if (!crashDir.exists()) {
                crashDir.mkdirs();
            }
            
            // Generate file name
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String timestamp = sdf.format(new Date());
            String fileName = "crash_" + timestamp + ".txt";
            
            File crashFile = new File(crashDir, fileName);
            
            // Write crash report
            try (FileWriter writer = new FileWriter(crashFile)) {
                writer.write("=== CRASH REPORT ===\n\n");
                writer.write("Time: " + new Date().toString() + "\n");
                writer.write("App Version: " + Constants.APP_VERSION + "\n\n");
                
                // Device info
                writer.write("=== DEVICE INFO ===\n");
                writer.write("Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n");
                writer.write("Android Version: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")\n");
                writer.write("Brand: " + Build.BRAND + "\n");
                writer.write("Product: " + Build.PRODUCT + "\n\n");
                
                // Stack trace
                writer.write("=== STACK TRACE ===\n");
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                writer.write(sw.toString());
                
                // Cause chain
                Throwable cause = throwable.getCause();
                if (cause != null) {
                    writer.write("\n=== CAUSED BY ===\n");
                    StringWriter csw = new StringWriter();
                    PrintWriter cpw = new PrintWriter(csw);
                    cause.printStackTrace(cpw);
                    writer.write(csw.toString());
                }
            }
            
            Logger.e(TAG, "Crash log saved: " + crashFile.getAbsolutePath());
            
        } catch (IOException e) {
            Logger.e(TAG, "Failed to save crash log", e);
        }
    }
    
    /**
     * Get crash log files
     */
    public static File[] getCrashLogs(Context context) {
        File crashDir = new File(context.getExternalFilesDir(null), CRASH_DIR);
        if (!crashDir.exists()) {
            return new File[0];
        }
        
        File[] files = crashDir.listFiles((dir, name) -> name.startsWith("crash_") && name.endsWith(".txt"));
        return files != null ? files : new File[0];
    }
    
    /**
     * Delete crash log
     */
    public static boolean deleteCrashLog(File crashFile) {
        return crashFile.delete();
    }
    
    /**
     * Clear all crash logs
     */
    public static void clearAllCrashLogs(Context context) {
        File[] crashLogs = getCrashLogs(context);
        for (File file : crashLogs) {
            file.delete();
        }
    }
}
