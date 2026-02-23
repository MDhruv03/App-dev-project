package com.example.myapplication.util;

import com.example.myapplication.model.Opportunity;

import java.util.ArrayList;
import java.util.List;

/**
 * Advanced search utilities with fuzzy matching and ranking
 */
public class SearchUtils {
    
    /**
     * Search opportunities with multiple criteria
     */
    public static List<Opportunity> searchOpportunities(List<Opportunity> opportunities, String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(opportunities);
        }
        
        String lowerQuery = query.toLowerCase().trim();
        List<ScoredOpportunity> scoredResults = new ArrayList<>();
        
        for (Opportunity opp : opportunities) {
            int score = calculateSearchScore(opp, lowerQuery);
            if (score > 0) {
                scoredResults.add(new ScoredOpportunity(opp, score));
            }
        }
        
        // Sort by score (highest first)
        scoredResults.sort((a, b) -> Integer.compare(b.score, a.score));
        
        List<Opportunity> results = new ArrayList<>();
        for (ScoredOpportunity scored : scoredResults) {
            results.add(scored.opportunity);
        }
        
        return results;
    }
    
    /**
     * Calculate search score for relevance ranking
     */
    private static int calculateSearchScore(Opportunity opp, String query) {
        int score = 0;
        
        // Exact match in title (highest priority)
        if (opp.getTitle().toLowerCase().equals(query)) {
            score += 100;
        } else if (opp.getTitle().toLowerCase().contains(query)) {
            score += 50;
        }
        
        // Company match
        if (opp.getCompany().toLowerCase().equals(query)) {
            score += 80;
        } else if (opp.getCompany().toLowerCase().contains(query)) {
            score += 40;
        }
        
        // Role match
        if (opp.getRole() != null && opp.getRole().toLowerCase().contains(query)) {
            score += 30;
        }
        
        // Location match
        if (opp.getLocation() != null && opp.getLocation().toLowerCase().contains(query)) {
            score += 20;
        }
        
        // Type match
        if (opp.getType().toLowerCase().contains(query)) {
            score += 25;
        }
        
        // Skills match
        if (opp.getRequiredSkills() != null) {
            for (String skill : opp.getRequiredSkills()) {
                if (skill.toLowerCase().contains(query)) {
                    score += 15;
                }
            }
        }
        
        // Description match
        if (opp.getDescription() != null && opp.getDescription().toLowerCase().contains(query)) {
            score += 10;
        }
        
        return score;
    }
    
    /**
     * Filter opportunities by multiple criteria
     */
    public static List<Opportunity> filterOpportunities(List<Opportunity> opportunities,
                                                       String type,
                                                       Boolean remote,
                                                       Boolean paid,
                                                       String location) {
        List<Opportunity> filtered = new ArrayList<>();
        
        for (Opportunity opp : opportunities) {
            boolean matches = true;
            
            if (type != null && !type.isEmpty() && !type.equals("all")) {
                matches = opp.getType().equalsIgnoreCase(type);
            }
            
            if (matches && remote != null) {
                matches = opp.isRemote() == remote;
            }
            
            if (matches && paid != null) {
                matches = opp.isPaid() == paid;
            }
            
            if (matches && location != null && !location.isEmpty()) {
                matches = opp.getLocation().toLowerCase().contains(location.toLowerCase());
            }
            
            if (matches) {
                filtered.add(opp);
            }
        }
        
        return filtered;
    }
    
    /**
     * Sort opportunities by different criteria
     */
    public static List<Opportunity> sortOpportunities(List<Opportunity> opportunities, String sortBy) {
        List<Opportunity> sorted = new ArrayList<>(opportunities);
        
        switch (sortBy.toLowerCase()) {
            case "deadline":
                sorted.sort((a, b) -> {
                    if (a.getDeadline() == null) return 1;
                    if (b.getDeadline() == null) return -1;
                    return a.getDeadline().compareTo(b.getDeadline());
                });
                break;
            case "company":
                sorted.sort((a, b) -> a.getCompany().compareToIgnoreCase(b.getCompany()));
                break;
            case "match":
                sorted.sort((a, b) -> Integer.compare(b.getMatchPercentage(), a.getMatchPercentage()));
                break;
            case "popularity":
                sorted.sort((a, b) -> Integer.compare(b.getPopularityScore(), a.getPopularityScore()));
                break;
            case "recommendation":
            default:
                sorted.sort((a, b) -> Double.compare(b.getRecommendationScore(), a.getRecommendationScore()));
                break;
        }
        
        return sorted;
    }
    
    /**
     * Get suggestions based on partial query
     */
    public static List<String> getSuggestions(List<Opportunity> opportunities, String partialQuery) {
        List<String> suggestions = new ArrayList<>();
        
        if (partialQuery == null || partialQuery.length() < 2) {
            return suggestions;
        }
        
        String lowerQuery = partialQuery.toLowerCase();
        
        for (Opportunity opp : opportunities) {
            if (opp.getTitle().toLowerCase().startsWith(lowerQuery)) {
                if (!suggestions.contains(opp.getTitle())) {
                    suggestions.add(opp.getTitle());
                }
            }
            if (opp.getCompany().toLowerCase().startsWith(lowerQuery)) {
                if (!suggestions.contains(opp.getCompany())) {
                    suggestions.add(opp.getCompany());
                }
            }
        }
        
        return suggestions.subList(0, Math.min(5, suggestions.size()));
    }
    
    private static class ScoredOpportunity {
        Opportunity opportunity;
        int score;
        
        ScoredOpportunity(Opportunity opportunity, int score) {
            this.opportunity = opportunity;
            this.score = score;
        }
    }
}
