package com.example.myapplication.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resume parser for extracting skills, experience, and keywords
 */
public class ResumeParser {
    
    // Comprehensive skill database
    private static final Set<String> TECH_SKILLS = new HashSet<>(Arrays.asList(
        // Programming Languages
        "Java", "Python", "JavaScript", "TypeScript", "C++", "C#", "Go", "Rust", "Kotlin", 
        "Swift", "Ruby", "PHP", "Scala", "R", "MATLAB", "Dart",
        
        // Web Technologies
        "HTML", "CSS", "React", "Angular", "Vue", "Node.js", "Express", "Django", "Flask",
        "Spring Boot", "ASP.NET", "Laravel", "Ruby on Rails", "Next.js", "Nuxt.js",
        
        // Mobile Development
        "Android", "iOS", "React Native", "Flutter", "Xamarin", "Ionic",
        
        // Databases
        "MySQL", "PostgreSQL", "MongoDB", "Redis", "Cassandra", "Oracle", "SQL Server",
        "DynamoDB", "Firebase", "Elasticsearch", "Neo4j",
        
        // Cloud & DevOps
        "AWS", "Azure", "GCP", "Docker", "Kubernetes", "Jenkins", "CI/CD", "Terraform",
        "Ansible", "Chef", "Puppet", "CloudFormation",
        
        // Data Science & ML
        "TensorFlow", "PyTorch", "Scikit-learn", "Pandas", "NumPy", "Keras", "OpenCV",
        "NLP", "Deep Learning", "Machine Learning", "Data Analysis", "Tableau", "Power BI",
        
        // Tools & Frameworks
        "Git", "GitHub", "GitLab", "Jira", "Confluence", "Postman", "Swagger",
        "GraphQL", "REST API", "Microservices", "Agile", "Scrum",
        
        // Other
        "Linux", "Windows", "MacOS", "Bash", "PowerShell", "Selenium", "JUnit",
        "Testing", "Unit Testing", "Integration Testing", "System Design"
    ));
    
    private static final Set<String> SOFT_SKILLS = new HashSet<>(Arrays.asList(
        "Leadership", "Communication", "Problem Solving", "Team Work", "Critical Thinking",
        "Time Management", "Adaptability", "Creativity", "Collaboration", "Analytical"
    ));
    
    /**
     * Extract skills from resume text
     */
    public static List<String> extractSkills(String resumeText) {
        Set<String> foundSkills = new HashSet<>();
        
        if (resumeText == null || resumeText.trim().isEmpty()) {
            return new ArrayList<>(foundSkills);
        }
        
        String lowerText = resumeText.toLowerCase();
        
        // Check for technical skills
        for (String skill : TECH_SKILLS) {
            if (lowerText.contains(skill.toLowerCase())) {
                foundSkills.add(skill);
            }
        }
        
        // Check for soft skills
        for (String skill : SOFT_SKILLS) {
            if (lowerText.contains(skill.toLowerCase())) {
                foundSkills.add(skill);
            }
        }
        
        return new ArrayList<>(foundSkills);
    }
    
    /**
     * Extract email from resume
     */
    public static String extractEmail(String resumeText) {
        if (resumeText == null) return null;
        
        Pattern emailPattern = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        );
        Matcher matcher = emailPattern.matcher(resumeText);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return null;
    }
    
    /**
     * Extract phone number from resume
     */
    public static String extractPhone(String resumeText) {
        if (resumeText == null) return null;
        
        // Pattern for various phone number formats
        Pattern phonePattern = Pattern.compile(
            "\\+?\\d{1,3}?[-.\\s]?\\(?\\d{1,4}?\\)?[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,9}"
        );
        Matcher matcher = phonePattern.matcher(resumeText);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return null;
    }
    
    /**
     * Extract years of experience
     */
    public static int extractExperienceYears(String resumeText) {
        if (resumeText == null) return 0;
        
        Pattern yearPattern = Pattern.compile(
            "(\\d+)\\s*(?:years?|yrs?)\\s*(?:of\\s*)?(?:experience|exp)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = yearPattern.matcher(resumeText);
        
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        
        return 0;
    }
    
    /**
     * Extract education level
     */
    public static String extractEducation(String resumeText) {
        if (resumeText == null) return "Not specified";
        
        String lowerText = resumeText.toLowerCase();
        
        if (lowerText.contains("ph.d") || lowerText.contains("doctorate")) {
            return "Ph.D.";
        } else if (lowerText.contains("master") || lowerText.contains("m.s") || 
                   lowerText.contains("mba") || lowerText.contains("m.tech")) {
            return "Master's";
        } else if (lowerText.contains("bachelor") || lowerText.contains("b.s") || 
                   lowerText.contains("b.tech") || lowerText.contains("b.e")) {
            return "Bachelor's";
        }
        
        return "Not specified";
    }
    
    /**
     * Calculate skill match percentage with opportunity
     */
    public static int calculateSkillMatch(List<String> resumeSkills, List<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return 100;
        }
        
        if (resumeSkills == null || resumeSkills.isEmpty()) {
            return 0;
        }
        
        Set<String> resumeSkillSet = new HashSet<>();
        for (String skill : resumeSkills) {
            resumeSkillSet.add(skill.toLowerCase());
        }
        
        int matches = 0;
        for (String required : requiredSkills) {
            if (resumeSkillSet.contains(required.toLowerCase())) {
                matches++;
            }
        }
        
        return (int) ((matches * 100.0) / requiredSkills.size());
    }
    
    /**
     * Extract GitHub profile
     */
    public static String extractGitHub(String resumeText) {
        if (resumeText == null) return null;
        
        Pattern githubPattern = Pattern.compile(
            "github\\.com/([a-zA-Z0-9_-]+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = githubPattern.matcher(resumeText);
        
        if (matcher.find()) {
            return "https://github.com/" + matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Extract LinkedIn profile
     */
    public static String extractLinkedIn(String resumeText) {
        if (resumeText == null) return null;
        
        Pattern linkedinPattern = Pattern.compile(
            "linkedin\\.com/in/([a-zA-Z0-9_-]+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = linkedinPattern.matcher(resumeText);
        
        if (matcher.find()) {
            return "https://linkedin.com/in/" + matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Generate resume summary
     */
    public static ResumeSummary parseResume(String resumeText) {
        ResumeSummary summary = new ResumeSummary();
        summary.setSkills(extractSkills(resumeText));
        summary.setEmail(extractEmail(resumeText));
        summary.setPhone(extractPhone(resumeText));
        summary.setExperienceYears(extractExperienceYears(resumeText));
        summary.setEducation(extractEducation(resumeText));
        summary.setGitHub(extractGitHub(resumeText));
        summary.setLinkedIn(extractLinkedIn(resumeText));
        
        return summary;
    }
    
    /**
     * Resume summary data class
     */
    public static class ResumeSummary {
        private List<String> skills;
        private String email;
        private String phone;
        private int experienceYears;
        private String education;
        private String gitHub;
        private String linkedIn;
        
        // Getters and setters
        public List<String> getSkills() { return skills; }
        public void setSkills(List<String> skills) { this.skills = skills; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        
        public int getExperienceYears() { return experienceYears; }
        public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }
        
        public String getEducation() { return education; }
        public void setEducation(String education) { this.education = education; }
        
        public String getGitHub() { return gitHub; }
        public void setGitHub(String gitHub) { this.gitHub = gitHub; }
        
        public String getLinkedIn() { return linkedIn; }
        public void setLinkedIn(String linkedIn) { this.linkedIn = linkedIn; }
    }
}
