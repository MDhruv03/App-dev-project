package com.example.myapplication.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

/**
 * Intent utilities for common app operations
 */
public class IntentUtils {
    
    /**
     * Open URL in browser
     */
    public static void openUrl(Context context, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        } catch (Exception e) {
            Logger.e("IntentUtils", "Error opening URL: " + url, e);
            UIUtils.showToast(context, "Cannot open link");
        }
    }
    
    /**
     * Send email
     */
    public static void sendEmail(Context context, String email, String subject, String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + email));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        
        try {
            context.startActivity(Intent.createChooser(intent, "Send Email"));
        } catch (Exception e) {
            Logger.e("IntentUtils", "Error sending email", e);
            UIUtils.showToast(context, "No email app found");
        }
    }
    
    /**
     * Make phone call
     */
    public static void makePhoneCall(Context context, String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Logger.e("IntentUtils", "Error making phone call", e);
            UIUtils.showToast(context, "Cannot make call");
        }
    }
    
    /**
     * Share text
     */
    public static void shareText(Context context, String text, String title) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        
        try {
            context.startActivity(Intent.createChooser(intent, title));
        } catch (Exception e) {
            Logger.e("IntentUtils", "Error sharing text", e);
            UIUtils.showToast(context, "Cannot share");
        }
    }
    
    /**
     * Share file
     */
    public static void shareFile(Context context, Uri fileUri, String mimeType, String title) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {
            context.startActivity(Intent.createChooser(intent, title));
        } catch (Exception e) {
            Logger.e("IntentUtils", "Error sharing file", e);
            UIUtils.showToast(context, "Cannot share file");
        }
    }
    
    /**
     * Open app in Play Store
     */
    public static void openAppInPlayStore(Context context) {
        String packageName = context.getPackageName();
        
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("market://details?id=" + packageName));
            context.startActivity(intent);
        } catch (Exception e) {
            // Fallback to browser
            Intent intent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            context.startActivity(intent);
        }
    }
    
    /**
     * Open app settings
     */
    public static void openAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Logger.e("IntentUtils", "Error opening app settings", e);
        }
    }
    
    /**
     * Open LinkedIn profile
     */
    public static void openLinkedInProfile(Context context, String profileUrl) {
        openUrl(context, profileUrl);
    }
    
    /**
     * Open GitHub profile
     */
    public static void openGitHubProfile(Context context, String profileUrl) {
        openUrl(context, profileUrl);
    }
    
    /**
     * Open company website
     */
    public static void openCompanyWebsite(Context context, String websiteUrl) {
        if (!websiteUrl.startsWith("http://") && !websiteUrl.startsWith("https://")) {
            websiteUrl = "https://" + websiteUrl;
        }
        openUrl(context, websiteUrl);
    }
}
