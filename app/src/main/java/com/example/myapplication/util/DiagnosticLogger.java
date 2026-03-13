package com.example.myapplication.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Lightweight diagnostics logger for startup/runtime crash triage.
 */
public final class DiagnosticLogger {

    private static final String TAG = "StartupDiag";
    private static final String FILE_NAME = "startup_diagnostics.log";
    private static Context appContext;

    private DiagnosticLogger() {
    }

    public static synchronized void init(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
        log("diagnostics_initialized");
    }

    public static void log(String message) {
        String line = timestamp() + " | INFO | " + message;
        Log.i(TAG, line);
        appendToFile(line);
    }

    public static void logError(String message, Throwable throwable) {
        String line = timestamp() + " | ERROR | " + message + " | "
                + (throwable == null ? "no-throwable" : throwable.toString());
        Log.e(TAG, line, throwable);
        appendToFile(line);
    }

    private static String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
    }

    private static synchronized void appendToFile(String line) {
        if (appContext == null) {
            return;
        }

        File logFile = new File(appContext.getFilesDir(), FILE_NAME);
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(line);
            writer.write('\n');
        } catch (IOException ignored) {
            // Avoid crash loops caused by diagnostics failures.
        }
    }
}
