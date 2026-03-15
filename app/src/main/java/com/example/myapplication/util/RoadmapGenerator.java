package com.example.myapplication.util;

import com.example.myapplication.model.InterviewProgress;
import com.example.myapplication.model.RoadmapTopic;
import com.example.myapplication.model.UserProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RoadmapGenerator {

    private RoadmapGenerator() {
    }

    public static List<RoadmapTopic> generateRoadmap(UserProfile profile, List<InterviewProgress> progress) {
        List<RoadmapTopic> topics = new ArrayList<>();
        Set<String> existingTitles = new HashSet<>();
        int nextId = 1;

        String skillsRaw = profile == null ? "" : safe(profile.getSkills());
        String rolesRaw = profile == null ? "" : safe(profile.getPreferredRoles());
        Set<String> userSkills = toNormalizedSet(skillsRaw);
        Set<String> preferredRoles = toNormalizedSet(rolesRaw);

        Set<String> weakTopics = findWeakTopics(progress);
        for (String weak : weakTopics) {
            RoadmapTopic weakTopic = buildWeakTopic(nextId++, weak);
            if (weakTopic != null && existingTitles.add(weakTopic.getTitle())) {
                topics.add(weakTopic);
            }
        }

        Set<String> roleRequiredSkills = getRoleRequiredSkills(preferredRoles);
        for (String requiredSkill : roleRequiredSkills) {
            if (!containsSimilarSkill(userSkills, requiredSkill)) {
                RoadmapTopic skillGapTopic = buildSkillGapTopic(nextId++, requiredSkill, preferredRoles);
                if (skillGapTopic != null && existingTitles.add(skillGapTopic.getTitle())) {
                    topics.add(skillGapTopic);
                }
            }
        }

        List<RoadmapTopic> growthGoals = buildGrowthGoals(nextId, preferredRoles);
        for (RoadmapTopic growthGoal : growthGoals) {
            if (existingTitles.add(growthGoal.getTitle())) {
                topics.add(growthGoal);
                nextId++;
            }
        }

        Collections.sort(topics, (a, b) -> {
            int byPriority = Integer.compare(a.getPriority(), b.getPriority());
            if (byPriority != 0) {
                return byPriority;
            }
            return a.getTitle().compareToIgnoreCase(b.getTitle());
        });

        return topics;
    }

    private static Set<String> findWeakTopics(List<InterviewProgress> progressList) {
        if (progressList == null || progressList.isEmpty()) {
            return new HashSet<>();
        }

        Map<String, double[]> aggregates = new HashMap<>();
        for (InterviewProgress progress : progressList) {
            if (progress == null) {
                continue;
            }

            Set<String> inferredTopics = inferTopics(progress);
            for (String topic : inferredTopics) {
                double[] scoreAndCount = aggregates.computeIfAbsent(topic, k -> new double[]{0.0, 0.0});
                scoreAndCount[0] += progress.getScore();
                scoreAndCount[1] += 1.0;
            }
        }

        Set<String> weakTopics = new HashSet<>();
        for (Map.Entry<String, double[]> entry : aggregates.entrySet()) {
            double[] scoreAndCount = entry.getValue();
            if (scoreAndCount[1] == 0) {
                continue;
            }
            double average = scoreAndCount[0] / scoreAndCount[1];
            if (average < 60.0) {
                weakTopics.add(entry.getKey());
            }
        }

        return weakTopics;
    }

    private static Set<String> inferTopics(InterviewProgress progress) {
        Set<String> topics = new HashSet<>();

        if (progress.getWeakTopics() != null) {
            for (String topic : progress.getWeakTopics()) {
                String normalized = normalizeTopic(topic);
                if (!normalized.isEmpty()) {
                    topics.add(normalized);
                }
            }
        }

        String combined = (safe(progress.getFeedback()) + " " + safe(progress.getImprovements())).toLowerCase(Locale.US);
        if (combined.contains("dsa") || combined.contains("data structure") || combined.contains("algorithm")) {
            topics.add("DSA");
        }
        if (combined.contains("oops") || combined.contains("oop") || combined.contains("object-oriented")) {
            topics.add("OOPS");
        }
        if (combined.contains("dbms") || combined.contains("sql") || combined.contains("database")) {
            topics.add("DBMS");
        }
        if (combined.contains("system design") || combined.contains("scalability") || combined.contains("distributed")) {
            topics.add("System Design");
        }

        return topics;
    }

    private static RoadmapTopic buildWeakTopic(int id, String weakTopic) {
        switch (weakTopic) {
            case "DSA":
                return new RoadmapTopic(
                    id,
                    "Practice 50 LeetCode problems",
                    "Focus on arrays, strings, recursion, and dynamic programming patterns.",
                    "DSA",
                    1,
                    14,
                    "https://www.youtube.com/watch?v=PKYkVt0r8OQ",
                    false,
                    "Weak Area"
                );
            case "OOPS":
                return new RoadmapTopic(
                    id,
                    "Master OOP Concepts in Java",
                    "Revise encapsulation, abstraction, inheritance, and polymorphism with examples.",
                    "OOPS",
                    1,
                    10,
                    "https://www.youtube.com/watch?v=6T_HgnjoYwM",
                    false,
                    "Weak Area"
                );
            case "DBMS":
                return new RoadmapTopic(
                    id,
                    "Complete SQL Masterclass",
                    "Practice joins, indexing, query optimization, and schema design.",
                    "DBMS",
                    1,
                    12,
                    "https://www.youtube.com/watch?v=HXV3zeQKqGY",
                    false,
                    "Weak Area"
                );
            case "System Design":
                return new RoadmapTopic(
                    id,
                    "System Design Interview Prep",
                    "Learn HLD/LLD fundamentals, trade-offs, and scalable architecture patterns.",
                    "System Design",
                    1,
                    15,
                    "https://www.youtube.com/watch?v=UzLMhqg3_Wc",
                    false,
                    "Weak Area"
                );
            default:
                return null;
        }
    }

    private static Set<String> getRoleRequiredSkills(Set<String> preferredRoles) {
        Set<String> required = new HashSet<>();

        for (String role : preferredRoles) {
            if (role.contains("android")) {
                required.addAll(Arrays.asList("Android", "Kotlin", "System Design"));
            }
            if (role.contains("sde") || role.contains("software") || role.contains("backend")) {
                required.addAll(Arrays.asList("DSA", "OOPS", "DBMS", "System Design"));
            }
            if (role.contains("ml") || role.contains("machine learning") || role.contains("ai")) {
                required.addAll(Arrays.asList("ML", "Python", "Statistics", "SQL"));
            }
        }

        if (required.isEmpty()) {
            required.addAll(Arrays.asList("DSA", "OOPS", "DBMS"));
        }

        return required;
    }

    private static RoadmapTopic buildSkillGapTopic(int id, String skill, Set<String> preferredRoles) {
        String normalized = skill.toLowerCase(Locale.US);

        if (normalized.contains("android")) {
            return new RoadmapTopic(
                id,
                "Android Development Roadmap 2024",
                "Build core Android app architecture and production-ready UI fundamentals.",
                "Android",
                2,
                20,
                "https://www.youtube.com/watch?v=EfAo7bpyTek",
                false,
                "Skill Gap"
            );
        }

        if (normalized.equals("dsa") || normalized.contains("algorithm")) {
            return new RoadmapTopic(
                id,
                "Complete DSA Course",
                "Strengthen problem-solving fundamentals needed for coding rounds.",
                "DSA",
                2,
                25,
                "https://www.youtube.com/watch?v=WQoB2z67hvY",
                false,
                "Skill Gap"
            );
        }

        if (normalized.equals("ml") || normalized.contains("machine learning")) {
            return new RoadmapTopic(
                id,
                "Machine Learning Full Course",
                "Cover ML basics, supervised learning, and model evaluation.",
                "ML",
                2,
                28,
                "https://www.youtube.com/watch?v=jGwO_UgTS7I",
                false,
                "Skill Gap"
            );
        }

        String roleHint = preferredRoles.isEmpty() ? "target role" : "target roles";
        return new RoadmapTopic(
            id,
            "Build " + skill + " Fundamentals",
            "Close this skill gap to better match your " + roleHint + ".",
            skill,
            2,
            10,
            "https://roadmap.sh",
            false,
            "Skill Gap"
        );
    }

    private static List<RoadmapTopic> buildGrowthGoals(int startId, Set<String> preferredRoles) {
        List<RoadmapTopic> goals = new ArrayList<>();
        int id = startId;

        goals.add(new RoadmapTopic(
            id++,
            "Build one interview-ready project",
            "Create a polished project and document architecture, trade-offs, and impact.",
            "Portfolio",
            3,
            14,
            "https://github.com/topics,https://roadmap.sh",
            false,
            "Career Goal"
        ));

        if (containsRole(preferredRoles, "android")) {
            goals.add(new RoadmapTopic(
                id++,
                "Publish an Android app end-to-end",
                "Ship one complete app with testing, analytics, and release notes.",
                "Android",
                3,
                21,
                "https://developer.android.com/distribute",
                false,
                "Career Goal"
            ));
        } else if (containsRole(preferredRoles, "ml") || containsRole(preferredRoles, "ai")) {
            goals.add(new RoadmapTopic(
                id++,
                "Deploy an ML model demo",
                "Serve a model with a simple API and monitor basic metrics.",
                "ML",
                3,
                18,
                "https://www.tensorflow.org/tutorials",
                false,
                "Career Goal"
            ));
        } else {
            goals.add(new RoadmapTopic(
                id++,
                "System design whiteboard practice",
                "Practice explaining architecture clearly under 20 minutes.",
                "System Design",
                3,
                12,
                "https://github.com/donnemartin/system-design-primer",
                false,
                "Career Goal"
            ));
        }

        goals.add(new RoadmapTopic(
            id,
            "Weekly mock interview cycle",
            "Run one mock interview every week and track improvement metrics.",
            "Interview",
            3,
            30,
            "https://www.pramp.com",
            false,
            "Career Goal"
        ));

        return goals;
    }

    private static boolean containsRole(Set<String> roles, String keyword) {
        for (String role : roles) {
            if (role.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> toNormalizedSet(String csv) {
        Set<String> set = new HashSet<>();
        if (csv == null || csv.trim().isEmpty()) {
            return set;
        }

        String[] parts = csv.split(",");
        for (String part : parts) {
            String cleaned = part == null ? "" : part.trim().toLowerCase(Locale.US);
            if (!cleaned.isEmpty()) {
                set.add(cleaned);
            }
        }
        return set;
    }

    private static String normalizeTopic(String topic) {
        String normalized = safe(topic).toLowerCase(Locale.US);
        if (normalized.contains("dsa") || normalized.contains("data structure") || normalized.contains("algorithm")) {
            return "DSA";
        }
        if (normalized.contains("oops") || normalized.contains("oop") || normalized.contains("object")) {
            return "OOPS";
        }
        if (normalized.contains("dbms") || normalized.contains("database") || normalized.contains("sql")) {
            return "DBMS";
        }
        if (normalized.contains("system")) {
            return "System Design";
        }
        return "";
    }

    private static boolean containsSimilarSkill(Set<String> userSkills, String requiredSkill) {
        String required = requiredSkill.toLowerCase(Locale.US);
        for (String userSkill : userSkills) {
            if (userSkill.equals(required)) {
                return true;
            }
            if (required.equals("ml") && (userSkill.contains("machine learning") || userSkill.equals("ai"))) {
                return true;
            }
            if (required.equals("dbms") && (userSkill.contains("sql") || userSkill.contains("database"))) {
                return true;
            }
            if (required.equals("dsa") && (userSkill.contains("algorithm") || userSkill.contains("data structure"))) {
                return true;
            }
        }
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
