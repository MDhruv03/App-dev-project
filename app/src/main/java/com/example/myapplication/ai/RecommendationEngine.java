package com.example.myapplication.ai;

import com.example.myapplication.model.Opportunity;
import com.example.myapplication.model.User;
import com.example.myapplication.model.UserProfile;
import com.example.myapplication.model.UserPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

/**
 * Advanced recommendation engine using multi-factor scoring
 */
public class RecommendationEngine {
    
    // Scoring weights
    private static final int ROLE_MATCH_WEIGHT = 3;
    private static final int LOCATION_MATCH_WEIGHT = 2;
    private static final int PAY_PREFERENCE_WEIGHT = 2;
    private static final int SKILL_MATCH_WEIGHT = 1;
    private static final int DEADLINE_PROXIMITY_WEIGHT = 1;
    private static final int PROFILE_TYPE_MATCH_WEIGHT = 3;
    private static final int PROFILE_LOCATION_MATCH_WEIGHT = 2;
    private static final int PROFILE_PAID_MATCH_WEIGHT = 2;
    private static final int PROFILE_SKILL_MATCH_WEIGHT = 1;
    private static final int PROFILE_REMOTE_MATCH_WEIGHT = 2;
    
    /**
     * Calculate recommendation score for an opportunity
     */
    public static int calculateScore(Opportunity opportunity, User user, UserPreferences preferences) {
        int score = 0;
        
        // Role matching
        if (preferences != null && preferences.getPreferredRoles() != null) {
            for (String preferredRole : preferences.getPreferredRoles()) {
                if (opportunity.getRole().toLowerCase().contains(preferredRole.toLowerCase())) {
                    score += ROLE_MATCH_WEIGHT;
                    break;
                }
            }
        }
        
        // Location matching
        if (preferences != null && preferences.getPreferredLocations() != null) {
            for (String preferredLocation : preferences.getPreferredLocations()) {
                if (opportunity.getLocation().toLowerCase().contains(preferredLocation.toLowerCase()) ||
                    preferredLocation.equalsIgnoreCase("remote") && opportunity.isRemote()) {
                    score += LOCATION_MATCH_WEIGHT;
                    break;
                }
            }
        }
        
        // Pay preference (paid vs unpaid)
        if (preferences != null && preferences.isOnlyPaid() && opportunity.isPaid()) {
            score += PAY_PREFERENCE_WEIGHT;
        }
        
        // Skill matching
        if (user != null && user.getSkills() != null && opportunity.getRequiredSkills() != null) {
            int skillMatches = 0;
            for (String userSkill : user.getSkills().keySet()) {
                for (String requiredSkill : opportunity.getRequiredSkills()) {
                    if (userSkill.equalsIgnoreCase(requiredSkill)) {
                        skillMatches++;
                    }
                }
            }
            score += skillMatches * SKILL_MATCH_WEIGHT;
        }
        
        // Deadline proximity (bonus for urgent deadlines)
        long daysUntilDeadline = getDaysUntilDeadline(opportunity.getDeadline());
        if (daysUntilDeadline > 0 && daysUntilDeadline <= 7) {
            score += DEADLINE_PROXIMITY_WEIGHT * 2; // High priority
        } else if (daysUntilDeadline > 7 && daysUntilDeadline <= 30) {
            score += DEADLINE_PROXIMITY_WEIGHT;
        }
        
        // Type preference
        if (preferences != null && preferences.getInterestedTypes() != null) {
            for (String type : preferences.getInterestedTypes()) {
                if (opportunity.getType().equalsIgnoreCase(type)) {
                    score += 2;
                    break;
                }
            }
        }
        
        return score;
    }
    
    /**
     * Get ranked opportunities based on user preferences
     */
    public static List<Opportunity> getRankedOpportunities(List<Opportunity> opportunities, 
                                                           User user, 
                                                           UserPreferences preferences) {
        // Create a map of opportunities to their scores
        Map<Opportunity, Integer> scoredOpportunities = new HashMap<>();
        
        for (Opportunity opp : opportunities) {
            int score = calculateScore(opp, user, preferences);
            scoredOpportunities.put(opp, score);
        }
        
        // Sort by score
        List<Opportunity> ranked = new ArrayList<>(opportunities);
        Collections.sort(ranked, new Comparator<Opportunity>() {
            @Override
            public int compare(Opportunity o1, Opportunity o2) {
                int score1 = scoredOpportunities.get(o1);
                int score2 = scoredOpportunities.get(o2);
                return Integer.compare(score2, score1); // Descending order
            }
        });
        
        return ranked;
    }
    
    /**
     * Get top N recommended opportunities
     */
    public static List<Opportunity> getTopRecommendations(List<Opportunity> opportunities,
                                                          User user,
                                                          UserPreferences preferences,
                                                          int count) {
        List<Opportunity> ranked = getRankedOpportunities(opportunities, user, preferences);
        return ranked.subList(0, Math.min(count, ranked.size()));
    }
    
    /**
     * Calculate match percentage for display
     */
    public static int getMatchPercentage(Opportunity opportunity, User user, UserPreferences preferences) {
        int score = calculateScore(opportunity, user, preferences);
        int maxPossibleScore = 15; // Maximum realistic score
        
        int percentage = (int) ((score * 100.0) / maxPossibleScore);
        
        // Ensure percentage is between 0 and 100
        percentage = Math.max(0, Math.min(100, percentage));
        
        // Add some baseline if there's any match
        if (percentage < 60 && score > 0) {
            percentage = 60 + (percentage / 3);
        }
        
        return percentage;
    }
    
    /**
     * Get skill match count
     */
    public static int getSkillMatchCount(Opportunity opportunity, User user) {
        if (user == null || user.getSkills() == null || 
            opportunity.getRequiredSkills() == null) {
            return 0;
        }
        
        int matches = 0;
        for (String userSkill : user.getSkills().keySet()) {
            for (String requiredSkill : opportunity.getRequiredSkills()) {
                if (userSkill.equalsIgnoreCase(requiredSkill)) {
                    matches++;
                }
            }
        }
        
        return matches;
    }
    
    /**
     * Check if opportunity matches user's critical criteria
     */
    public static boolean isRelevant(Opportunity opportunity, UserPreferences preferences) {
        if (preferences == null) return true;
        
        // Must match paid preference if user wants only paid
        if (preferences.isOnlyPaid() && !opportunity.isPaid()) {
            return false;
        }
        
        // Must match remote preference if user wants only remote
        if (preferences.isRemoteOnly() && !opportunity.isRemote()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Filter opportunities based on preferences
     */
    public static List<Opportunity> filterOpportunities(List<Opportunity> opportunities,
                                                        UserPreferences preferences) {
        if (preferences == null) return opportunities;
        
        List<Opportunity> filtered = new ArrayList<>();
        for (Opportunity opp : opportunities) {
            if (isRelevant(opp, preferences)) {
                filtered.add(opp);
            }
        }
        
        return filtered;
    }
    
    private static long getDaysUntilDeadline(Date deadline) {
        if (deadline == null) return Long.MAX_VALUE;
        
        long diff = deadline.getTime() - System.currentTimeMillis();
        return diff / (1000 * 60 * 60 * 24);
    }

    public static List<Opportunity> getRecommended(List<Opportunity> all, UserProfile profile) {
        if (all == null || all.isEmpty()) {
            return new ArrayList<>();
        }

        List<Opportunity> ranked = new ArrayList<>(all);
        for (Opportunity opportunity : ranked) {
            int score = calculateProfileScore(opportunity, profile);
            opportunity.setRecommendationScore(score);
        }

        ranked.sort((o1, o2) -> Double.compare(o2.getRecommendationScore(), o1.getRecommendationScore()));
        return ranked.subList(0, Math.min(10, ranked.size()));
    }

    private static int calculateProfileScore(Opportunity opportunity, UserProfile profile) {
        if (opportunity == null || profile == null) {
            return 0;
        }

        int score = 0;

        String opportunityType = safeLower(opportunity.getType());
        for (String preferredRole : splitCsv(profile.getPreferredRoles())) {
            String role = safeLower(preferredRole);
            if (!role.isEmpty() && !opportunityType.isEmpty() &&
                    (role.contains(opportunityType) || opportunityType.contains(role))) {
                score += PROFILE_TYPE_MATCH_WEIGHT;
                break;
            }
        }

        String preferredLocation = safeLower(profile.getPreferredLocation());
        String opportunityLocation = safeLower(opportunity.getLocation());
        if (!preferredLocation.isEmpty() && opportunityLocation.contains(preferredLocation)) {
            score += PROFILE_LOCATION_MATCH_WEIGHT;
        }

        if (profile.isPaidPreference() && opportunity.isPaid()) {
            score += PROFILE_PAID_MATCH_WEIGHT;
        }

        String title = safeLower(opportunity.getTitle());
        String description = safeLower(opportunity.getDescription());
        for (String skill : splitCsv(profile.getSkills())) {
            String normalizedSkill = safeLower(skill);
            if (!normalizedSkill.isEmpty() &&
                    (title.contains(normalizedSkill) || description.contains(normalizedSkill))) {
                score += PROFILE_SKILL_MATCH_WEIGHT;
            }
        }

        if ("remote".equalsIgnoreCase(profile.getJobTypePreference()) && opportunity.isRemote()) {
            score += PROFILE_REMOTE_MATCH_WEIGHT;
        }

        return score;
    }

    private static List<String> splitCsv(String csv) {
        List<String> result = new ArrayList<>();
        if (csv == null || csv.trim().isEmpty()) {
            return result;
        }

        String[] parts = csv.split(",");
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                result.add(part.trim());
            }
        }
        return result;
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
