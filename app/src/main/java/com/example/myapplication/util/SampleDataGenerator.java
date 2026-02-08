package com.example.myapplication.util;

import com.example.myapplication.model.Opportunity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class SampleDataGenerator {
    
    private static final String[] COMPANIES = {
        "Google", "Microsoft", "Amazon", "Meta", "Apple",
        "Netflix", "Tesla", "Adobe", "Salesforce", "Oracle",
        "IBM", "Intel", "NVIDIA", "Uber", "Airbnb"
    };
    
    private static final String[] ROLES = {
        "Software Engineer", "Backend Developer", "Frontend Developer",
        "Full Stack Developer", "Mobile Developer", "DevOps Engineer",
        "Data Scientist", "ML Engineer", "Cloud Architect", "Security Engineer"
    };
    
    private static final String[] LOCATIONS = {
        "San Francisco, CA", "New York, NY", "Seattle, WA",
        "Austin, TX", "Boston, MA", "Remote", "Hybrid"
    };
    
    private static final String[] TYPES = {"internship", "job", "hackathon"};
    
    private static final List<String> TECH_SKILLS = Arrays.asList(
        "Java", "Python", "JavaScript", "React", "Node.js",
        "Spring Boot", "Docker", "Kubernetes", "AWS", "Azure",
        "Machine Learning", "Data Structures", "Algorithms", "SQL", "MongoDB"
    );
    
    private static final Random random = new Random();
    
    public static List<Opportunity> generateOpportunities(int count) {
        List<Opportunity> opportunities = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Opportunity opportunity = new Opportunity();
            
            String company = COMPANIES[random.nextInt(COMPANIES.length)];
            String role = ROLES[random.nextInt(ROLES.length)];
            String type = TYPES[random.nextInt(TYPES.length)];
            String location = LOCATIONS[random.nextInt(LOCATIONS.length)];
            
            opportunity.setTitle(role + (type.equals("internship") ? " Intern" : ""));
            opportunity.setCompany(company);
            opportunity.setType(type);
            opportunity.setRole(role);
            opportunity.setLocation(location);
            opportunity.setRemote(location.equals("Remote") || location.equals("Hybrid"));
            opportunity.setPaid(random.nextBoolean() || type.equals("job"));
            
            // Description
            opportunity.setDescription("Exciting opportunity to work on cutting-edge technologies. "
                + "Join our team and make an impact on millions of users worldwide.");
            
            // Required skills (3-5 random skills)
            List<String> skills = new ArrayList<>();
            int skillCount = 3 + random.nextInt(3);
            for (int j = 0; j < skillCount; j++) {
                String skill = TECH_SKILLS.get(random.nextInt(TECH_SKILLS.size()));
                if (!skills.contains(skill)) {
                    skills.add(skill);
                }
            }
            opportunity.setRequiredSkills(skills);
            
            // Deadline (within next 30-90 days)
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 30 + random.nextInt(60));
            opportunity.setDeadline(calendar.getTime());
            
            // Apply link
            opportunity.setApplyLink("https://careers." + company.toLowerCase() + ".com");
            
            // Recommendation score (50-100)
            opportunity.setRecommendationScore(50 + random.nextDouble() * 50);
            
            // Match percentage (60-100)
            opportunity.setMatchPercentage(60 + random.nextInt(41));
            
            // Popularity score (100-1000)
            opportunity.setPopularityScore(100 + random.nextInt(900));
            
            opportunities.add(opportunity);
        }
        
        // Sort by recommendation score
        opportunities.sort((o1, o2) -> 
            Double.compare(o2.getRecommendationScore(), o1.getRecommendationScore())
        );
        
        return opportunities;
    }
    
    public static List<Opportunity> getRecommendedOpportunities() {
        return generateOpportunities(5);
    }
    
    public static List<Opportunity> getAllOpportunities() {
        return generateOpportunities(20);
    }
}
