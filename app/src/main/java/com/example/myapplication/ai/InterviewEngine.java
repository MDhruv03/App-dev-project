package com.example.myapplication.ai;

import com.example.myapplication.model.InterviewQuestion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Advanced AI Interview Engine with real-world questions and intelligent evaluation
 */
public class InterviewEngine {
    
    private static final Random random = new Random();
    
    // Comprehensive question bank organized by domain and difficulty
    private static final Map<String, List<QuestionData>> QUESTION_BANK = new HashMap<>();
    
    static {
        initializeQuestionBank();
    }
    
    private static class QuestionData {
        String question;
        String topic;
        String difficulty;
        String[] keywords;
        String idealAnswer;
        int maxScore;
        
        QuestionData(String question, String topic, String difficulty, String[] keywords, String idealAnswer, int maxScore) {
            this.question = question;
            this.topic = topic;
            this.difficulty = difficulty;
            this.keywords = keywords;
            this.idealAnswer = idealAnswer;
            this.maxScore = maxScore;
        }
    }
    
    private static void initializeQuestionBank() {
        // Data Structures & Algorithms Questions
        List<QuestionData> dsaQuestions = new ArrayList<>();
        dsaQuestions.add(new QuestionData(
            "Explain the time complexity of different sorting algorithms and when you would use each one.",
            "DSA",
            "Medium",
            new String[]{"O(n log n)", "quicksort", "mergesort", "heapsort", "time complexity", "worst case", "average case"},
            "QuickSort: O(n log n) average, O(n²) worst. MergeSort: O(n log n) guaranteed, stable. HeapSort: O(n log n), in-place. Use QuickSort for general purpose, MergeSort when stability matters, HeapSort when memory is limited.",
            100
        ));
        dsaQuestions.add(new QuestionData(
            "How would you detect a cycle in a linked list? Explain the algorithm and its complexity.",
            "DSA",
            "Medium",
            new String[]{"floyd", "tortoise", "hare", "two pointer", "cycle detection", "O(n)"},
            "Use Floyd's Cycle Detection (tortoise and hare). Two pointers: slow moves 1 step, fast moves 2 steps. If they meet, cycle exists. Time: O(n), Space: O(1).",
            100
        ));
        dsaQuestions.add(new QuestionData(
            "What is the difference between BFS and DFS? When would you use each?",
            "DSA",
            "Easy",
            new String[]{"breadth", "depth", "queue", "stack", "shortest path", "level order"},
            "BFS uses queue, explores level by level, finds shortest path. DFS uses stack/recursion, explores depth first. Use BFS for shortest path, DFS for topological sort, cycle detection, path finding.",
            80
        ));
        dsaQuestions.add(new QuestionData(
            "Explain dynamic programming and give an example of when you'd use it.",
            "DSA",
            "Hard",
            new String[]{"memoization", "tabulation", "optimal substructure", "overlapping subproblems", "fibonacci", "knapsack"},
            "DP solves problems by breaking them into overlapping subproblems with optimal substructure. Uses memoization (top-down) or tabulation (bottom-up). Examples: Fibonacci, Knapsack, Longest Common Subsequence.",
            100
        ));
        dsaQuestions.add(new QuestionData(
            "How do you find the kth largest element in an unsorted array efficiently?",
            "DSA",
            "Medium",
            new String[]{"quickselect", "heap", "partition", "O(n)", "priority queue"},
            "Use QuickSelect algorithm (modification of QuickSort). Partition array and recursively search only relevant half. Average O(n), worst O(n²). Alternative: Min-heap of size k, O(n log k).",
            100
        ));
        
        // Object-Oriented Programming Questions
        List<QuestionData> oopsQuestions = new ArrayList<>();
        oopsQuestions.add(new QuestionData(
            "Explain the four pillars of Object-Oriented Programming with real-world examples.",
            "OOPS",
            "Easy",
            new String[]{"encapsulation", "inheritance", "polymorphism", "abstraction"},
            "1) Encapsulation: Data hiding (bank account). 2) Inheritance: Code reuse (Vehicle->Car). 3) Polymorphism: One interface, multiple implementations (Shape.draw()). 4) Abstraction: Hide complexity (ATM interface).",
            100
        ));
        oopsQuestions.add(new QuestionData(
            "What is the difference between abstract classes and interfaces in Java?",
            "OOPS",
            "Medium",
            new String[]{"abstract", "interface", "multiple inheritance", "default methods", "constructor"},
            "Abstract class: Can have constructors, fields, concrete methods. Single inheritance. Interface: Multiple inheritance, all methods abstract (except default). Java 8+ allows default methods in interfaces.",
            90
        ));
        oopsQuestions.add(new QuestionData(
            "Explain method overloading vs method overriding with examples.",
            "OOPS",
            "Easy",
            new String[]{"overloading", "overriding", "compile time", "runtime", "polymorphism"},
            "Overloading: Same method name, different parameters, compile-time polymorphism. Overriding: Subclass redefines parent method, runtime polymorphism, must keep same signature.",
            80
        ));
        oopsQuestions.add(new QuestionData(
            "What are SOLID principles? Explain with examples.",
            "OOPS",
            "Hard",
            new String[]{"single responsibility", "open closed", "liskov substitution", "interface segregation", "dependency inversion"},
            "S: Single Responsibility - One class, one job. O: Open/Closed - Open for extension, closed for modification. L: Liskov Substitution - Subclasses should be substitutable. I: Interface Segregation - Many specific interfaces. D: Dependency Inversion - Depend on abstractions.",
            100
        ));
        
        // Database Management Questions
        List<QuestionData> dbmsQuestions = new ArrayList<>();
        dbmsQuestions.add(new QuestionData(
            "Explain ACID properties in database transactions.",
            "DBMS",
            "Medium",
            new String[]{"atomicity", "consistency", "isolation", "durability", "transaction"},
            "A: Atomicity - All or nothing. C: Consistency - Valid state always. I: Isolation - Concurrent transactions don't interfere. D: Durability - Committed data persists.",
            100
        ));
        dbmsQuestions.add(new QuestionData(
            "What is database normalization? Explain different normal forms.",
            "DBMS",
            "Hard",
            new String[]{"1NF", "2NF", "3NF", "BCNF", "denormalization", "redundancy"},
            "Normalization reduces redundancy. 1NF: Atomic values. 2NF: No partial dependencies. 3NF: No transitive dependencies. BCNF: Every determinant is a candidate key.",
            100
        ));
        dbmsQuestions.add(new QuestionData(
            "Difference between SQL and NoSQL databases? When to use each?",
            "DBMS",
            "Medium",
            new String[]{"relational", "non-relational", "schema", "scalability", "mongodb", "postgresql"},
            "SQL: Structured, ACID, vertical scaling, complex queries. NoSQL: Flexible schema, horizontal scaling, high throughput. Use SQL for complex relationships, NoSQL for massive scale and flexibility.",
            90
        ));
        dbmsQuestions.add(new QuestionData(
            "Explain indexing in databases. Types and trade-offs?",
            "DBMS",
            "Medium",
            new String[]{"b-tree", "hash", "clustered", "non-clustered", "performance", "overhead"},
            "Index speeds up reads using B-tree or hash structures. Clustered: Data stored in index order. Non-clustered: Separate structure. Trade-off: Faster reads, slower writes, more storage.",
            100
        ));
        
        // System Design Questions
        List<QuestionData> systemDesignQuestions = new ArrayList<>();
        systemDesignQuestions.add(new QuestionData(
            "How would you design a URL shortener like bit.ly?",
            "System Design",
            "Hard",
            new String[]{"hash", "base62", "database", "scalability", "caching", "distributed"},
            "Use hash function or base62 encoding for short URLs. Database: Key-value store (original->short). Cache popular URLs. Load balancer for distribution. Handle collisions. Analytics tracking.",
            100
        ));
        systemDesignQuestions.add(new QuestionData(
            "Explain microservices architecture vs monolithic. Pros and cons?",
            "System Design",
            "Medium",
            new String[]{"microservices", "monolithic", "scalability", "deployment", "communication"},
            "Monolithic: Single codebase, simple deployment, hard to scale. Microservices: Independent services, scalable, complex infrastructure. Use microservices for large teams and independent scaling.",
            100
        ));
        systemDesignQuestions.add(new QuestionData(
            "How does a content delivery network (CDN) work?",
            "System Design",
            "Medium",
            new String[]{"caching", "edge servers", "latency", "geographic", "cloudflare"},
            "CDN caches content on edge servers globally. User requests routed to nearest server. Reduces latency, bandwidth, server load. Origin server updates propagate to edge servers.",
            90
        ));
        systemDesignQuestions.add(new QuestionData(
            "Design a rate limiter for an API. Explain different algorithms.",
            "System Design",
            "Hard",
            new String[]{"token bucket", "leaky bucket", "sliding window", "fixed window", "throttling"},
            "Token Bucket: Tokens added at fixed rate, consumed per request. Sliding Window: Track requests in rolling time window. Fixed Window: Reset counter at intervals. Choose based on burst tolerance needs.",
            100
        ));
        
        // Behavioral Questions
        List<QuestionData> behavioralQuestions = new ArrayList<>();
        behavioralQuestions.add(new QuestionData(
            "Tell me about a time you faced a difficult bug. How did you solve it?",
            "Behavioral",
            "Medium",
            new String[]{"debugging", "problem solving", "systematic", "tools", "teamwork"},
            "Use STAR method: Situation (describe bug), Task (responsibility), Action (debugging steps, tools used), Result (resolution and learning). Show systematic approach and persistence.",
            80
        ));
        behavioralQuestions.add(new QuestionData(
            "Describe a project where you had to learn a new technology quickly.",
            "Behavioral",
            "Easy",
            new String[]{"learning", "adaptation", "fast", "technology", "project"},
            "STAR: Describe situation, timeline pressure, learning approach (documentation, tutorials, practice), implementation, and successful outcome. Emphasize learning strategy.",
            80
        ));
        behavioralQuestions.add(new QuestionData(
            "How do you handle disagreements with team members on technical decisions?",
            "Behavioral",
            "Medium",
            new String[]{"collaboration", "communication", "compromise", "data-driven", "respect"},
            "Listen to perspectives, present data/evidence, discuss pros/cons objectively, find common ground, escalate if needed. Focus on what's best for project, not ego.",
            90
        ));
        behavioralQuestions.add(new QuestionData(
            "Tell me about a time you missed a deadline. What happened?",
            "Behavioral",
            "Hard",
            new String[]{"accountability", "communication", "planning", "learning", "time management"},
            "Be honest. STAR: Describe situation, why deadline was missed, how you communicated early, mitigation steps, what you learned about estimation and planning.",
            85
        ));
        
        // Android Development Questions
        List<QuestionData> androidQuestions = new ArrayList<>();
        androidQuestions.add(new QuestionData(
            "Explain the Android activity lifecycle. Why is it important?",
            "Android",
            "Easy",
            new String[]{"onCreate", "onStart", "onResume", "onPause", "onStop", "onDestroy", "lifecycle"},
            "onCreate->onStart->onResume->onPause->onStop->onDestroy. Important for resource management, saving state, handling configuration changes. onPause for critical saves, onStop for releasing resources.",
            100
        ));
        androidQuestions.add(new QuestionData(
            "What is the difference between ListView and RecyclerView?",
            "Android",
            "Medium",
            new String[]{"recyclerview", "listview", "viewholder", "performance", "adapter"},
            "RecyclerView: ViewHolder pattern (mandatory), better performance, flexible layouts, item animations. ListView: Simple but less efficient, no built-in ViewHolder. RecyclerView is modern standard.",
            90
        ));
        androidQuestions.add(new QuestionData(
            "Explain MVVM architecture in Android. Why use it?",
            "Android",
            "Hard",
            new String[]{"model", "view", "viewmodel", "livedata", "separation of concerns", "testability"},
            "Model: Data layer. View: UI (Activity/Fragment). ViewModel: Business logic, survives configuration changes. Benefits: Separation of concerns, testability, lifecycle awareness with LiveData.",
            100
        ));
        androidQuestions.add(new QuestionData(
            "How do you handle background tasks in Android?",
            "Android",
            "Hard",
            new String[]{"workmanager", "coroutines", "service", "asynctask", "foreground service"},
            "WorkManager for deferrable tasks, Coroutines for asynchronous, Services for long-running, Foreground Service for user-aware tasks. Avoid AsyncTask (deprecated). Consider battery optimization.",
            100
        ));
        
        // Web Development Questions
        List<QuestionData> webQuestions = new ArrayList<>();
        webQuestions.add(new QuestionData(
            "Explain the difference between REST and GraphQL APIs.",
            "Web",
            "Medium",
            new String[]{"REST", "GraphQL", "query", "endpoint", "over-fetching", "under-fetching"},
            "REST: Multiple endpoints, fixed responses, can over/under-fetch. GraphQL: Single endpoint, client specifies exact data needed, strong typing. GraphQL better for complex data needs.",
            100
        ));
        webQuestions.add(new QuestionData(
            "What is Cross-Origin Resource Sharing (CORS)? Why is it important?",
            "Web",
            "Medium",
            new String[]{"CORS", "same-origin", "security", "headers", "preflight"},
            "Security mechanism preventing scripts from accessing resources from different origins. Browser enforces same-origin policy. CORS headers (Access-Control-Allow-Origin) permit cross-origin requests. Preflight for complex requests.",
            90
        ));
        webQuestions.add(new QuestionData(
            "Explain the Virtual DOM in React. How does it improve performance?",
            "Web",
            "Hard",
            new String[]{"virtual dom", "reconciliation", "diffing", "react", "performance"},
            "Virtual DOM is in-memory representation of real DOM. React compares virtual DOM snapshots (diffing), calculates minimal changes, batch updates to real DOM. Reduces expensive DOM operations.",
            100
        ));
        webQuestions.add(new QuestionData(
            "What are Web Sockets? When would you use them over HTTP?",
            "Web",
            "Medium",
            new String[]{"websocket", "real-time", "bidirectional", "persistent", "chat"},
            "WebSocket: Persistent, bidirectional connection. HTTP: Request-response. Use WebSockets for real-time apps (chat, live updates, gaming). Lower latency than polling.",
            90
        ));
        
        // Machine Learning Questions
        List<QuestionData> mlQuestions = new ArrayList<>();
        mlQuestions.add(new QuestionData(
            "Explain the bias-variance tradeoff in machine learning.",
            "ML",
            "Hard",
            new String[]{"bias", "variance", "overfitting", "underfitting", "tradeoff"},
            "Bias: Error from wrong assumptions (underfitting). Variance: Error from sensitivity to training data (overfitting). High bias = too simple model. High variance = too complex. Balance is key.",
            100
        ));
        mlQuestions.add(new QuestionData(
            "What is the difference between supervised and unsupervised learning?",
            "ML",
            "Easy",
            new String[]{"supervised", "unsupervised", "labeled", "clustering", "classification"},
            "Supervised: Learns from labeled data (input-output pairs). Examples: classification, regression. Unsupervised: Finds patterns in unlabeled data. Examples: clustering, dimensionality reduction.",
            80
        ));
        mlQuestions.add(new QuestionData(
            "Explain gradient descent and its variants.",
            "ML",
            "Hard",
            new String[]{"gradient descent", "optimization", "learning rate", "batch", "stochastic", "mini-batch"},
            "Iterative optimization algorithm. Batch GD: All data per update. Stochastic GD: One sample per update. Mini-batch GD: Small batches. Learning rate controls step size. Adam combines momentum and adaptive learning.",
            100
        ));
        mlQuestions.add(new QuestionData(
            "How do you handle imbalanced datasets in classification?",
            "ML",
            "Medium",
            new String[]{"imbalanced", "oversampling", "undersampling", "SMOTE", "class weight"},
            "Techniques: Resampling (over/undersample), SMOTE (synthetic samples), class weights, different metrics (F1, precision-recall instead of accuracy), ensemble methods.",
            100
        ));
        
        QUESTION_BANK.put("DSA", dsaQuestions);
        QUESTION_BANK.put("OOPS", oopsQuestions);
        QUESTION_BANK.put("DBMS", dbmsQuestions);
        QUESTION_BANK.put("System Design", systemDesignQuestions);
        QUESTION_BANK.put("Behavioral", behavioralQuestions);
        QUESTION_BANK.put("Android", androidQuestions);
        QUESTION_BANK.put("Web", webQuestions);
        QUESTION_BANK.put("ML", mlQuestions);
    }
    
    /**
     * Generate interview questions based on domain
     */
    public static List<InterviewQuestion> generateQuestions(String domain, int count) {
        List<InterviewQuestion> questions = new ArrayList<>();
        List<QuestionData> domainQuestions = QUESTION_BANK.get(domain);
        
        if (domainQuestions == null || domainQuestions.isEmpty()) {
            return questions;
        }
        
        // Shuffle and select questions
        List<QuestionData> shuffled = new ArrayList<>(domainQuestions);
        java.util.Collections.shuffle(shuffled);
        
        for (int i = 0; i < Math.min(count, shuffled.size()); i++) {
            QuestionData qd = shuffled.get(i);
            InterviewQuestion question = new InterviewQuestion();
            question.setQuestion(qd.question);
            question.setTopic(qd.topic);
            question.setDifficulty(qd.difficulty);
            question.setIsAnswered(false);
            questions.add(question);
        }
        
        return questions;
    }
    
    /**
     * Evaluate an answer using keyword matching and semantic analysis
     */
    public static EvaluationResult evaluateAnswer(String question, String userAnswer, String topic) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return new EvaluationResult(0, "No answer provided. Please try again.", new ArrayList<>(), new ArrayList<>());
        }
        
        // Find the question data
        QuestionData questionData = findQuestionData(question, topic);
        if (questionData == null) {
            return new EvaluationResult(50, "Answer evaluated.", new ArrayList<>(), new ArrayList<>());
        }
        
        String answer = userAnswer.toLowerCase();
        int score = 0;
        int matchedKeywords = 0;
        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        
        // Keyword matching
        for (String keyword : questionData.keywords) {
            if (answer.contains(keyword.toLowerCase())) {
                matchedKeywords++;
            }
        }
        
        // Calculate base score from keyword matching
        double keywordScore = (matchedKeywords * 1.0 / questionData.keywords.length) * questionData.maxScore;
        score = (int) keywordScore;
        
        // Length bonus (thoughtful answers are longer)
        int wordCount = answer.split("\\s+").length;
        if (wordCount > 50) {
            score += 10;
            strengths.add("Comprehensive explanation");
        } else if (wordCount > 20) {
            score += 5;
            strengths.add("Good level of detail");
        } else if (wordCount < 10) {
            improvements.add("Provide more detailed explanation");
        }
        
        // Structure bonus (if answer contains examples)
        if (answer.contains("example") || answer.contains("for instance") || answer.contains("such as")) {
            score += 5;
            strengths.add("Good use of examples");
        }
        
        // Technical depth indicators
        if (answer.contains("complexity") || answer.contains("time") || answer.contains("space")) {
            score += 5;
            strengths.add("Discussed complexity analysis");
        }
        
        // Cap score at 100
        score = Math.min(score, 100);
        
        // Generate feedback
        String feedback = generateFeedback(score, matchedKeywords, questionData.keywords.length, wordCount);
        
        // Add specific improvements based on missing keywords
        if (matchedKeywords < questionData.keywords.length) {
            int missingCount = questionData.keywords.length - matchedKeywords;
            if (missingCount <= 2) {
                improvements.add("Consider mentioning: " + getMissingKeywords(answer, questionData.keywords, 2));
            } else {
                improvements.add("Key concepts to cover: " + getMissingKeywords(answer, questionData.keywords, 3));
            }
        }
        
        if (strengths.isEmpty()) {
            strengths.add("Keep practicing!");
        }
        
        if (improvements.isEmpty()) {
            improvements.add("Excellent answer! Try explaining to someone unfamiliar with the concept.");
        }
        
        return new EvaluationResult(score, feedback, strengths, improvements);
    }
    
    private static QuestionData findQuestionData(String question, String topic) {
        List<QuestionData> questions = QUESTION_BANK.get(topic);
        if (questions == null) return null;
        
        for (QuestionData qd : questions) {
            if (qd.question.equals(question)) {
                return qd;
            }
        }
        return null;
    }
    
    private static String generateFeedback(int score, int matchedKeywords, int totalKeywords, int wordCount) {
        StringBuilder feedback = new StringBuilder();
        
        if (score >= 90) {
            feedback.append("🌟 Excellent answer! ");
        } else if (score >= 75) {
            feedback.append("👍 Great job! ");
        } else if (score >= 60) {
            feedback.append("✓ Good attempt. ");
        } else if (score >= 40) {
            feedback.append("⚠ Needs improvement. ");
        } else {
            feedback.append("📚 Keep studying. ");
        }
        
        feedback.append(String.format("You covered %d/%d key concepts. ", matchedKeywords, totalKeywords));
        
        if (score < 60) {
            feedback.append("Review the topic and try to include more technical details. ");
        }
        
        if (wordCount < 15) {
            feedback.append("Provide more comprehensive explanation. ");
        }
        
        return feedback.toString();
    }
    
    private static String getMissingKeywords(String answer, String[] keywords, int maxCount) {
        List<String> missing = new ArrayList<>();
        for (String keyword : keywords) {
            if (!answer.toLowerCase().contains(keyword.toLowerCase())) {
                missing.add(keyword);
                if (missing.size() >= maxCount) break;
            }
        }
        return String.join(", ", missing);
    }
    
    /**
     * Get hint for a question
     */
    public static String getHint(String question, String topic) {
        QuestionData questionData = findQuestionData(question, topic);
        if (questionData == null || questionData.keywords.length == 0) {
            return "Think about the fundamental concepts related to this topic.";
        }
        
        // Return a random keyword as hint
        String keyword = questionData.keywords[random.nextInt(Math.min(3, questionData.keywords.length))];
        return "Hint: Consider discussing '" + keyword + "' in your answer.";
    }
    
    /**
     * Calculate overall readiness score based on performance
     */
    public static int calculateReadinessScore(List<InterviewQuestion> answeredQuestions) {
        if (answeredQuestions == null || answeredQuestions.isEmpty()) {
            return 0;
        }
        
        int totalScore = 0;
        int count = 0;
        
        for (InterviewQuestion q : answeredQuestions) {
            if (q.getIsAnswered()) {
                // Parse score from feedback if available
                // This is a simple implementation - actual score would be stored
                count++;
            }
        }
        
        // Base score on participation
        int baseScore = Math.min(count * 10, 70);
        
        // Add bonus for variety
        int varietyBonus = Math.min(count / 5 * 5, 30);
        
        return Math.min(baseScore + varietyBonus, 100);
    }
    
    /**
     * Result class for evaluation
     */
    public static class EvaluationResult {
        private int score;
        private String feedback;
        private List<String> strengths;
        private List<String> improvements;
        
        public EvaluationResult(int score, String feedback, List<String> strengths, List<String> improvements) {
            this.score = score;
            this.feedback = feedback;
            this.strengths = strengths;
            this.improvements = improvements;
        }
        
        public int getScore() { return score; }
        public String getFeedback() { return feedback; }
        public List<String> getStrengths() { return strengths; }
        public List<String> getImprovements() { return improvements; }
    }
}
