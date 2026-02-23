package com.example.myapplication.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

/**
 * Notification manager for app notifications
 */
public class NotificationHelper {
    
    private static final String CHANNEL_ID_DEADLINES = "deadlines_channel";
    private static final String CHANNEL_ID_INTERVIEWS = "interviews_channel";
    private static final String CHANNEL_ID_GENERAL = "general_channel";
    
    private static final String CHANNEL_NAME_DEADLINES = "Application Deadlines";
    private static final String CHANNEL_NAME_INTERVIEWS = "Interview Reminders";
    private static final String CHANNEL_NAME_GENERAL = "General Notifications";
    
    /**
     * Create notification channels
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            
            // Deadlines channel (High priority)
            NotificationChannel deadlinesChannel = new NotificationChannel(
                CHANNEL_ID_DEADLINES,
                CHANNEL_NAME_DEADLINES,
                NotificationManager.IMPORTANCE_HIGH
            );
            deadlinesChannel.setDescription("Notifications for upcoming application deadlines");
            manager.createNotificationChannel(deadlinesChannel);
            
            // Interviews channel (High priority)
            NotificationChannel interviewsChannel = new NotificationChannel(
                CHANNEL_ID_INTERVIEWS,
                CHANNEL_NAME_INTERVIEWS,
                NotificationManager.IMPORTANCE_HIGH
            );
            interviewsChannel.setDescription("Reminders for upcoming interviews");
            manager.createNotificationChannel(interviewsChannel);
            
            // General channel (Default priority)
            NotificationChannel generalChannel = new NotificationChannel(
                CHANNEL_ID_GENERAL,
                CHANNEL_NAME_GENERAL,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("General app notifications");
            manager.createNotificationChannel(generalChannel);
        }
    }
    
    /**
     * Show deadline notification
     */
    public static void showDeadlineNotification(Context context, String opportunityTitle, 
                                               String company, int hoursRemaining) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                PendingIntent.FLAG_IMMUTABLE : 0
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_DEADLINES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ Application Deadline Approaching")
            .setContentText(opportunityTitle + " at " + company + " - " + hoursRemaining + " hours left!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(opportunityTitle + " at " + company + " expires in " + hoursRemaining + " hours. Don't miss out!"));
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(generateNotificationId(), builder.build());
    }
    
    /**
     * Show interview reminder notification
     */
    public static void showInterviewReminderNotification(Context context, String company, 
                                                        String position, String dateTime) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("navigate_to", "tracker");
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                PendingIntent.FLAG_IMMUTABLE : 0
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_INTERVIEWS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📅 Interview Reminder")
            .setContentText(position + " at " + company + " - " + dateTime)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("Your interview for " + position + " at " + company + " is scheduled for " + dateTime + ". Good luck!"));
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(generateNotificationId(), builder.build());
    }
    
    /**
     * Show new opportunity notification
     */
    public static void showNewOpportunityNotification(Context context, String title, String company) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("navigate_to", "home");
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                PendingIntent.FLAG_IMMUTABLE : 0
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎯 New Opportunity Match!")
            .setContentText(title + " at " + company)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(generateNotificationId(), builder.build());
    }
    
    /**
     * Show application status update notification
     */
    public static void showStatusUpdateNotification(Context context, String company, 
                                                    String position, String newStatus) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("navigate_to", "tracker");
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                PendingIntent.FLAG_IMMUTABLE : 0
        );
        
        String emoji = getStatusEmoji(newStatus);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(emoji + " Application Status Update")
            .setContentText(position + " at " + company + " - " + newStatus)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(generateNotificationId(), builder.build());
    }
    
    /**
     * Show daily summary notification
     */
    public static void showDailySummaryNotification(Context context, int newOpportunities, 
                                                   int upcomingDeadlines, int interviews) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                PendingIntent.FLAG_IMMUTABLE : 0
        );
        
        StringBuilder summary = new StringBuilder();
        summary.append(newOpportunities).append(" new opportunities • ");
        summary.append(upcomingDeadlines).append(" deadlines this week • ");
        summary.append(interviews).append(" upcoming interviews");
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📊 Daily Summary")
            .setContentText(summary.toString())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(summary.toString()));
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(generateNotificationId(), builder.build());
    }
    
    private static String getStatusEmoji(String status) {
        switch (status.toLowerCase()) {
            case "accepted": return "🎉";
            case "interview": return "📞";
            case "rejected": return "😔";
            default: return "📬";
        }
    }
    
    private static int generateNotificationId() {
        return (int) System.currentTimeMillis();
    }
}
