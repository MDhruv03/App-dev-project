package com.example.myapplication.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Date and time utilities
 */
public class DateTimeUtils {
    
    // Date formats
    public static final String FORMAT_FULL_DATE = "MMMM dd, yyyy";
    public static final String FORMAT_SHORT_DATE = "MMM dd, yyyy";
    public static final String FORMAT_COMPACT_DATE = "MM/dd/yyyy";
    public static final String FORMAT_DATE_TIME = "MMM dd, yyyy hh:mm a";
    public static final String FORMAT_TIME = "hh:mm a";
    public static final String FORMAT_ISO = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    
    /**
     * Format date to string
     */
    public static String formatDate(Date date, String format) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(date);
    }
    
    /**
     * Parse string to date
     */
    public static Date parseDate(String dateString, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
            return sdf.parse(dateString);
        } catch (ParseException e) {
            Logger.e("DateTimeUtils", "Error parsing date: " + dateString, e);
            return null;
        }
    }
    
    /**
     * Get relative time string (e.g., "2 hours ago", "3 days ago")
     */
    public static String getRelativeTimeString(Date date) {
        if (date == null) return "";
        
        long diff = System.currentTimeMillis() - date.getTime();
        
        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Just now";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (diff < TimeUnit.DAYS.toMillis(7)) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + (days == 1 ? " day ago" : " days ago");
        } else if (diff < TimeUnit.DAYS.toMillis(30)) {
            long weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7;
            return weeks + (weeks == 1 ? " week ago" : " weeks ago");
        } else if (diff < TimeUnit.DAYS.toMillis(365)) {
            long months = TimeUnit.MILLISECONDS.toDays(diff) / 30;
            return months + (months == 1 ? " month ago" : " months ago");
        } else {
            long years = TimeUnit.MILLISECONDS.toDays(diff) / 365;
            return years + (years == 1 ? " year ago" : " years ago");
        }
    }
    
    /**
     * Get days until deadline
     */
    public static int getDaysUntil(Date deadline) {
        if (deadline == null) return 0;
        
        long diff = deadline.getTime() - System.currentTimeMillis();
        return (int) TimeUnit.MILLISECONDS.toDays(diff);
    }
    
    /**
     * Get deadline warning string
     */
    public static String getDeadlineWarning(Date deadline) {
        int days = getDaysUntil(deadline);
        
        if (days < 0) {
            return "Expired";
        } else if (days == 0) {
            return "Today!";
        } else if (days == 1) {
            return "Tomorrow!";
        } else if (days <= 7) {
            return days + " days left";
        } else {
            return formatDate(deadline, FORMAT_SHORT_DATE);
        }
    }
    
    /**
     * Check if deadline is approaching (within 7 days)
     */
    public static boolean isDeadlineApproaching(Date deadline) {
        int days = getDaysUntil(deadline);
        return days >= 0 && days <= 7;
    }
    
    /**
     * Check if date is today
     */
    public static boolean isToday(Date date) {
        if (date == null) return false;
        
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date);
        
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
    
    /**
     * Check if date is in the future
     */
    public static boolean isFuture(Date date) {
        return date != null && date.getTime() > System.currentTimeMillis();
    }
    
    /**
     * Check if date is in the past
     */
    public static boolean isPast(Date date) {
        return date != null && date.getTime() < System.currentTimeMillis();
    }
    
    /**
     * Get start of day
     */
    public static Date getStartOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    
    /**
     * Get end of day
     */
    public static Date getEndOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }
    
    /**
     * Add days to date
     */
    public static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }
    
    /**
     * Get date range string
     */
    public static String getDateRangeString(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) return "";
        
        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);
        
        if (start.get(Calendar.YEAR) == end.get(Calendar.YEAR)) {
            if (start.get(Calendar.MONTH) == end.get(Calendar.MONTH)) {
                return formatDate(startDate, "MMM dd") + " - " + formatDate(endDate, "dd, yyyy");
            } else {
                return formatDate(startDate, "MMM dd") + " - " + formatDate(endDate, "MMM dd, yyyy");
            }
        } else {
            return formatDate(startDate, "MMM dd, yyyy") + " - " + formatDate(endDate, "MMM dd, yyyy");
        }
    }
    
    /**
     * Get month name
     */
    public static String getMonthName(int month) {
        String[] months = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };
        
        if (month >= 0 && month < 12) {
            return months[month];
        }
        return "";
    }
    
    /**
     * Get current timestamp
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
    
    /**
     * Convert timestamp to date
     */
    public static Date timestampToDate(long timestamp) {
        return new Date(timestamp);
    }
}
