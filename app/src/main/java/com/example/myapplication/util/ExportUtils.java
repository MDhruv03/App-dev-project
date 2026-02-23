package com.example.myapplication.util;

import android.content.Context;
import android.os.Environment;

import com.example.myapplication.model.Application;
import com.example.myapplication.model.Opportunity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Export and backup utilities
 */
public class ExportUtils {
    
    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create();
    
    /**
     * Export data to JSON file
     */
    public static File exportToJSON(Context context, 
                                    List<Opportunity> opportunities,
                                    List<Application> applications) throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("exportDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
        data.put("opportunities", opportunities);
        data.put("applications", applications);
        data.put("version", "1.0");
        
        String json = gson.toJson(data);
        
        File exportDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "OpportunityHub");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        
        String fileName = "backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".json";
        File exportFile = new File(exportDir, fileName);
        
        try (FileWriter writer = new FileWriter(exportFile)) {
            writer.write(json);
        }
        
        return exportFile;
    }
    
    /**
     * Export applications to CSV
     */
    public static File exportApplicationsToCSV(Context context, List<Application> applications) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("Company,Position,Status,Applied Date,Interview Date,Notes\n");
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        
        for (Application app : applications) {
            csv.append(escapeCsv(app.getCompany())).append(",");
            csv.append(escapeCsv(app.getPosition())).append(",");
            csv.append(escapeCsv(app.getStatus())).append(",");
            csv.append(app.getAppliedDate() != null ? dateFormat.format(app.getAppliedDate()) : "").append(",");
            csv.append(app.getInterviewDate() != null ? dateFormat.format(app.getInterviewDate()) : "").append(",");
            csv.append(escapeCsv(app.getNotes())).append("\n");
        }
        
        File exportDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "OpportunityHub");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        
        String fileName = "applications_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".csv";
        File exportFile = new File(exportDir, fileName);
        
        try (FileWriter writer = new FileWriter(exportFile)) {
            writer.write(csv.toString());
        }
        
        return exportFile;
    }
    
    /**
     * Export opportunities to CSV
     */
    public static File exportOpportunitiesToCSV(Context context, List<Opportunity> opportunities) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("Title,Company,Type,Location,Remote,Paid,Deadline,Skills,Apply Link\n");
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        
        for (Opportunity opp : opportunities) {
            csv.append(escapeCsv(opp.getTitle())).append(",");
            csv.append(escapeCsv(opp.getCompany())).append(",");
            csv.append(escapeCsv(opp.getType())).append(",");
            csv.append(escapeCsv(opp.getLocation())).append(",");
            csv.append(opp.isRemote() ? "Yes" : "No").append(",");
            csv.append(opp.isPaid() ? "Yes" : "No").append(",");
            csv.append(opp.getDeadline() != null ? dateFormat.format(opp.getDeadline()) : "").append(",");
            csv.append(escapeCsv(opp.getRequiredSkills() != null ? String.join("; ", opp.getRequiredSkills()) : "")).append(",");
            csv.append(escapeCsv(opp.getApplyLink())).append("\n");
        }
        
        File exportDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "OpportunityHub");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        
        String fileName = "opportunities_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".csv";
        File exportFile = new File(exportDir, fileName);
        
        try (FileWriter writer = new FileWriter(exportFile)) {
            writer.write(csv.toString());
        }
        
        return exportFile;
    }
    
    /**
     * Escape CSV special characters
     */
    private static String escapeCsv(String value) {
        if (value == null) return "";
        
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }
    
    /**
     * Create analytics report
     */
    public static String generateAnalyticsReport(List<Application> applications, 
                                                 List<Opportunity> savedOpportunities) {
        StringBuilder report = new StringBuilder();
        report.append("=== OpportunityHub Analytics Report ===\n\n");
        report.append("Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())).append("\n\n");
        
        // Applications stats
        report.append("APPLICATIONS SUMMARY\n");
        report.append("Total Applications: ").append(applications.size()).append("\n");
        
        Map<String, Integer> statusBreakdown = AnalyticsCalculator.getStatusBreakdown(applications);
        report.append("  - Applied: ").append(statusBreakdown.get("Applied")).append("\n");
        report.append("  - Interview: ").append(statusBreakdown.get("Interview")).append("\n");
        report.append("  - Accepted: ").append(statusBreakdown.get("Accepted")).append("\n");
        report.append("  - Rejected: ").append(statusBreakdown.get("Rejected")).append("\n");
        
        report.append("Success Rate: ").append(String.format("%.1f%%", AnalyticsCalculator.calculateSuccessRate(applications))).append("\n");
        report.append("Interview Conversion: ").append(String.format("%.1f%%", AnalyticsCalculator.calculateInterviewConversionRate(applications))).append("\n");
        
        report.append("\nSAVED OPPORTUNITIES\n");
        report.append("Total Saved: ").append(savedOpportunities.size()).append("\n");
        
        return report.toString();
    }
}
