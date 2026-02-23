package com.example.myapplication.util;

import com.example.myapplication.model.Application;
import com.example.myapplication.model.Opportunity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive analytics calculator
 */
public class AnalyticsCalculator {
    
    /**
     * Calculate application success rate
     */
    public static double calculateSuccessRate(List<Application> applications) {
        if (applications == null || applications.isEmpty()) {
            return 0.0;
        }
        
        int total = applications.size();
        int successful = 0;
        
        for (Application app : applications) {
            if ("Accepted".equalsIgnoreCase(app.getStatus())) {
                successful++;
            }
        }
        
        return (successful * 100.0) / total;
    }
    
    /**
     * Calculate interview conversion rate
     */
    public static double calculateInterviewConversionRate(List<Application> applications) {
        if (applications == null || applications.isEmpty()) {
            return 0.0;
        }
        
        int interviewed = 0;
        int accepted = 0;
        
        for (Application app : applications) {
            if ("Interview".equalsIgnoreCase(app.getStatus()) || 
                "Accepted".equalsIgnoreCase(app.getStatus())) {
                interviewed++;
                if ("Accepted".equalsIgnoreCase(app.getStatus())) {
                    accepted++;
                }
            }
        }
        
        if (interviewed == 0) return 0.0;
        return (accepted * 100.0) / interviewed;
    }
    
    /**
     * Get application status breakdown
     */
    public static Map<String, Integer> getStatusBreakdown(List<Application> applications) {
        Map<String, Integer> breakdown = new HashMap<>();
        breakdown.put("Applied", 0);
        breakdown.put("Interview", 0);
        breakdown.put("Rejected", 0);
        breakdown.put("Accepted", 0);
        
        if (applications != null) {
            for (Application app : applications) {
                String status = app.getStatus();
                breakdown.put(status, breakdown.getOrDefault(status, 0) + 1);
            }
        }
        
        return breakdown;
    }
    
    /**
     * Get applications timeline (grouped by week/month)
     */
    public static Map<String, Integer> getApplicationsTimeline(List<Application> applications) {
        Map<String, Integer> timeline = new HashMap<>();
        
        if (applications == null) {
            return timeline;
        }
        
        for (Application app : applications) {
            Date date = app.getAppliedDate();
            if (date != null) {
                String week = getWeekKey(date);
                timeline.put(week, timeline.getOrDefault(week, 0) + 1);
            }
        }
        
        return timeline;
    }
    
    /**
     * Get most applied companies
     */
    public static Map<String, Integer> getTopCompanies(List<Application> applications, int topN) {
        Map<String, Integer> companyCount = new HashMap<>();
        
        if (applications != null) {
            for (Application app : applications) {
                String company = app.getCompany();
                companyCount.put(company, companyCount.getOrDefault(company, 0) + 1);
            }
        }
        
        // Sort and get top N (simplified - would use actual sorting in production)
        return companyCount;
    }
    
    /**
     * Get average days to response
     */
    public static double getAverageDaysToResponse(List<Application> applications) {
        if (applications == null || applications.isEmpty()) {
            return 0.0;
        }
        
        int count = 0;
        long totalDays = 0;
        
        for (Application app : applications) {
            if (app.getAppliedDate() != null && app.getResponseDate() != null) {
                long diff = app.getResponseDate().getTime() - app.getAppliedDate().getTime();
                long days = diff / (1000 * 60 * 60 * 24);
                totalDays += days;
                count++;
            }
        }
        
        if (count == 0) return 0.0;
        return totalDays * 1.0 / count;
    }
    
    /**
     * Get upcoming interviews count
     */
    public static int getUpcomingInterviewsCount(List<Application> applications) {
        if (applications == null) {
            return 0;
        }
        
        int count = 0;
        Date now = new Date();
        
        for (Application app : applications) {
            if ("Interview".equalsIgnoreCase(app.getStatus()) && 
                app.getInterviewDate() != null &&
                app.getInterviewDate().after(now)) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Get opportunities expiring soon count
     */
    public static int getExpiringOpportunitiesCount(List<Opportunity> opportunities, int daysThreshold) {
        if (opportunities == null) {
            return 0;
        }
        
        int count = 0;
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, daysThreshold);
        Date threshold = cal.getTime();
        
        for (Opportunity opp : opportunities) {
            if (opp.getDeadline() != null &&
                opp.getDeadline().after(now) &&
                opp.getDeadline().before(threshold)) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Calculate skill coverage percentage
     */
    public static double calculateSkillCoverage(List<String> userSkills, List<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return 100.0;
        }
        
        if (userSkills == null || userSkills.isEmpty()) {
            return 0.0;
        }
        
        int matches = 0;
        for (String required : requiredSkills) {
            for (String userSkill : userSkills) {
                if (userSkill.equalsIgnoreCase(required)) {
                    matches++;
                    break;
                }
            }
        }
        
        return (matches * 100.0) / requiredSkills.size();
    }
    
    /**
     * Get activity summary for dashboard
     */
    public static ActivitySummary getActivitySummary(List<Application> applications, 
                                                      List<Opportunity> savedOpportunities) {
        ActivitySummary summary = new ActivitySummary();
        
        summary.totalApplications = applications != null ? applications.size() : 0;
        summary.successRate = calculateSuccessRate(applications);
        summary.upcomingInterviews = getUpcomingInterviewsCount(applications);
        summary.statusBreakdown = getStatusBreakdown(applications);
        summary.savedOpportunities = savedOpportunities != null ? savedOpportunities.size() : 0;
        
        return summary;
    }
    
    /**
     * Activity summary data class
     */
    public static class ActivitySummary {
        public int totalApplications;
        public double successRate;
        public int upcomingInterviews;
        public Map<String, Integer> statusBreakdown;
        public int savedOpportunities;
    }
    
    private static String getWeekKey(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int week = cal.get(Calendar.WEEK_OF_YEAR);
        int year = cal.get(Calendar.YEAR);
        return "Week " + week + ", " + year;
    }
}
