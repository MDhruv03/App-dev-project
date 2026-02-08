package com.example.myapplication.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateUtils {
    
    private static final SimpleDateFormat DATE_FORMAT = 
        new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    
    private static final SimpleDateFormat SHORT_DATE_FORMAT = 
        new SimpleDateFormat("MMM dd", Locale.getDefault());
    
    private static final SimpleDateFormat TIME_FORMAT = 
        new SimpleDateFormat("hh:mm a", Locale.getDefault());
    
    public static String formatDate(Date date) {
        if (date == null) return "";
        return DATE_FORMAT.format(date);
    }
    
    public static String formatShortDate(Date date) {
        if (date == null) return "";
        return SHORT_DATE_FORMAT.format(date);
    }
    
    public static String formatTime(Date date) {
        if (date == null) return "";
        return TIME_FORMAT.format(date);
    }
    
    public static long getDaysBetween(Date start, Date end) {
        if (start == null || end == null) return 0;
        long diff = end.getTime() - start.getTime();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }
    
    public static String getTimeAgo(Date date) {
        if (date == null) return "";
        
        long diff = new Date().getTime() - date.getTime();
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        
        if (seconds < 60) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " min ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (days < 7) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else {
            return formatShortDate(date);
        }
    }
    
    public static boolean isDeadlineNear(Date deadline) {
        if (deadline == null) return false;
        long daysUntil = getDaysBetween(new Date(), deadline);
        return daysUntil <= 7 && daysUntil >= 0;
    }
}
