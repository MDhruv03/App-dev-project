package com.example.myapplication.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

/**
 * Share utilities for sharing opportunities, reports, etc.
 */
public class ShareUtils {
    
    /**
     * Share opportunity details
     */
    public static void shareOpportunity(Context context, String title, String company, 
                                       String link, String description) {
        String shareText = buildOpportunityShareText(title, company, link, description);
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title + " at " + company);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        context.startActivity(Intent.createChooser(shareIntent, "Share Opportunity"));
    }
    
    /**
     * Share application progress
     */
    public static void shareApplicationProgress(Context context, String company, 
                                                String position, String status) {
        String shareText = "I just updated my application for " + position + 
                          " at " + company + " to: " + status + 
                          "\n\nTracking my career progress with OpportunityHub!";
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Application Update");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        context.startActivity(Intent.createChooser(shareIntent, "Share Progress"));
    }
    
    /**
     * Share analytics report
     */
    public static void shareAnalyticsReport(Context context, String reportText) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My OpportunityHub Analytics Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, reportText);
        
        context.startActivity(Intent.createChooser(shareIntent, "Share Report"));
    }
    
    /**
     * Share file (CSV, JSON export)
     */
    public static void shareFile(Context context, File file, String mimeType) {
        Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            context.getPackageName() + ".fileprovider",
            file
        );
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType);
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        context.startActivity(Intent.createChooser(shareIntent, "Share File"));
    }
    
    /**
     * Open opportunity link in browser
     */
    public static void openLink(Context context, String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(browserIntent);
    }
    
    /**
     * Share app with friends
     */
    public static void shareApp(Context context) {
        String shareText = "Check out OpportunityHub - the best app for finding internships, " +
                          "jobs, and hackathons! Track applications and practice interviews all in one place.\n\n" +
                          "Download now: [App Store Link]";
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "OpportunityHub - Career App");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        context.startActivity(Intent.createChooser(shareIntent, "Share App"));
    }
    
    /**
     * Invite team members to hackathon
     */
    public static void inviteToHackathon(Context context, String hackathonName, 
                                        String deadline, String link) {
        String inviteText = "Hey! I'm participating in " + hackathonName + 
                          ". Want to join my team?\n\n" +
                          "Deadline: " + deadline + "\n" +
                          "Details: " + link;
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Hackathon Team Invite");
        shareIntent.putExtra(Intent.EXTRA_TEXT, inviteText);
        
        context.startActivity(Intent.createChooser(shareIntent, "Invite Team"));
    }
    
    private static String buildOpportunityShareText(String title, String company, 
                                                   String link, String description) {
        StringBuilder text = new StringBuilder();
        text.append("🎯 ").append(title).append("\n");
        text.append("🏢 ").append(company).append("\n\n");
        
        if (description != null && !description.isEmpty()) {
            String shortDesc = description.length() > 150 ? 
                description.substring(0, 150) + "..." : description;
            text.append(shortDesc).append("\n\n");
        }
        
        text.append("Apply here: ").append(link).append("\n\n");
        text.append("Found via OpportunityHub");
        
        return text.toString();
    }
}
