package com.example.myapplication.util;

import com.example.myapplication.model.InterviewQuestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InterviewDataGenerator {
    
    public static List<InterviewQuestion> generateQuestionsForDomain(String domain) {
        List<InterviewQuestion> questions = new ArrayList<>();
        
        switch (domain.toUpperCase()) {
            case "SDE":
                questions.addAll(getSDEQuestions());
                break;
            case "ML":
                questions.addAll(getMLQuestions());
                break;
            case "WEB":
                questions.addAll(getWebQuestions());
                break;
            case "ANDROID":
                questions.addAll(getAndroidQuestions());
                break;
            case "HR":
                questions.addAll(getHRQuestions());
                break;
            default:
                questions.addAll(getSDEQuestions());
        }
        
        return questions;
    }
    
    private static List<InterviewQuestion> getSDEQuestions() {
        List<InterviewQuestion> questions = new ArrayList<>();
        
        InterviewQuestion q1 = new InterviewQuestion("SDE", "DSA", 
            "Explain the difference between ArrayList and LinkedList in Java.",
            "Easy");
        q1.setExpectedKeywords(Arrays.asList("dynamic array", "node", "insertion", "deletion", "random access"));
        q1.setSampleAnswer("ArrayList uses a dynamic array internally, providing fast random access (O(1)) but slower insertions/deletions in the middle. LinkedList uses nodes with references, making insertions/deletions O(1) but random access O(n).");
        questions.add(q1);
        
        InterviewQuestion q2 = new InterviewQuestion("SDE", "DSA",
            "What is the time complexity of quicksort and explain when it performs worst?",
            "Medium");
        q2.setExpectedKeywords(Arrays.asList("O(n log n)", "pivot", "worst case", "O(n²)", "sorted array"));
        q2.setSampleAnswer("Quicksort has an average time complexity of O(n log n) but degrades to O(n²) in the worst case when the pivot consistently produces unbalanced partitions, such as when the array is already sorted.");
        questions.add(q2);
        
        InterviewQuestion q3 = new InterviewQuestion("SDE", "OOP",
            "Explain the SOLID principles in object-oriented programming.",
            "Hard");
        q3.setExpectedKeywords(Arrays.asList("Single Responsibility", "Open-Closed", "Liskov Substitution", "Interface Segregation", "Dependency Inversion"));
        q3.setSampleAnswer("SOLID includes: Single Responsibility (one class, one purpose), Open-Closed (open for extension, closed for modification), Liskov Substitution (subtypes must be substitutable), Interface Segregation (no forced dependencies), Dependency Inversion (depend on abstractions).");
        questions.add(q3);
        
        InterviewQuestion q4 = new InterviewQuestion("SDE", "System Design",
            "How would you design a URL shortening service like bit.ly?",
            "Hard");
        q4.setExpectedKeywords(Arrays.asList("hashing", "database", "scalability", "unique ID", "redirection", "caching"));
        q4.setSampleAnswer("Use a hash function or base62 encoding to generate short URLs, store mappings in a distributed database, implement caching for frequently accessed URLs, use load balancers for scalability, and handle collisions with unique ID generation.");
        questions.add(q4);
        
        InterviewQuestion q5 = new InterviewQuestion("SDE", "DBMS",
            "What is database normalization and why is it important?",
            "Medium");
        q5.setExpectedKeywords(Arrays.asList("redundancy", "1NF", "2NF", "3NF", "anomalies", "consistency"));
        q5.setSampleAnswer("Normalization is organizing database tables to reduce redundancy and dependency. It involves applying normal forms (1NF, 2NF, 3NF) to eliminate update anomalies and ensure data consistency.");
        questions.add(q5);
        
        return questions;
    }
    
    private static List<InterviewQuestion> getMLQuestions() {
        List<InterviewQuestion> questions = new ArrayList<>();
        
        InterviewQuestion q1 = new InterviewQuestion("ML", "Fundamentals",
            "Explain the difference between supervised and unsupervised learning.",
            "Easy");
        q1.setExpectedKeywords(Arrays.asList("labeled data", "unlabeled", "classification", "regression", "clustering"));
        q1.setSampleAnswer("Supervised learning uses labeled data to train models for prediction (classification/regression). Unsupervised learning finds patterns in unlabeled data through techniques like clustering and dimensionality reduction.");
        questions.add(q1);
        
        InterviewQuestion q2 = new InterviewQuestion("ML", "Algorithms",
            "What is overfitting and how can you prevent it?",
            "Medium");
        q2.setExpectedKeywords(Arrays.asList("training data", "generalization", "regularization", "cross-validation", "dropout"));
        q2.setSampleAnswer("Overfitting occurs when a model learns training data too well, losing generalization ability. Prevention methods include regularization (L1/L2), cross-validation, dropout, early stopping, and increasing training data.");
        questions.add(q2);
        
        InterviewQuestion q3 = new InterviewQuestion("ML", "Deep Learning",
            "Explain the vanishing gradient problem in deep neural networks.",
            "Hard");
        q3.setExpectedKeywords(Arrays.asList("backpropagation", "sigmoid", "gradient", "ReLU", "batch normalization"));
        q3.setSampleAnswer("Vanishing gradients occur during backpropagation when gradients become extremely small, preventing effective learning in early layers. Solutions include using ReLU activation, batch normalization, residual connections, and proper weight initialization.");
        questions.add(q3);
        
        return questions;
    }
    
    private static List<InterviewQuestion> getWebQuestions() {
        List<InterviewQuestion> questions = new ArrayList<>();
        
        InterviewQuestion q1 = new InterviewQuestion("WEB", "JavaScript",
            "Explain the difference between var, let, and const in JavaScript.",
            "Easy");
        q1.setExpectedKeywords(Arrays.asList("scope", "hoisting", "block scope", "reassignment", "const"));
        q1.setSampleAnswer("var is function-scoped and hoisted, let is block-scoped and not hoisted, const is block-scoped and cannot be reassigned. Modern JavaScript prefers let/const over var.");
        questions.add(q1);
        
        InterviewQuestion q2 = new InterviewQuestion("WEB", "React",
            "What are React hooks and why were they introduced?",
            "Medium");
        q2.setExpectedKeywords(Arrays.asList("useState", "useEffect", "functional components", "state management", "lifecycle"));
        q2.setSampleAnswer("Hooks allow functional components to use state and lifecycle methods without classes. Key hooks include useState for state and useEffect for side effects, making code more reusable and testable.");
        questions.add(q2);
        
        InterviewQuestion q3 = new InterviewQuestion("WEB", "Performance",
            "How would you optimize the performance of a slow-loading web application?",
            "Hard");
        q3.setExpectedKeywords(Arrays.asList("lazy loading", "code splitting", "caching", "CDN", "minification", "compression"));
        q3.setSampleAnswer("Optimize by implementing code splitting, lazy loading, image optimization, browser caching, using CDN, minimizing bundle size, enabling gzip compression, and reducing HTTP requests.");
        questions.add(q3);
        
        return questions;
    }
    
    private static List<InterviewQuestion> getAndroidQuestions() {
        List<InterviewQuestion> questions = new ArrayList<>();
        
        InterviewQuestion q1 = new InterviewQuestion("ANDROID", "Basics",
            "Explain the Android Activity lifecycle.",
            "Easy");
        q1.setExpectedKeywords(Arrays.asList("onCreate", "onStart", "onResume", "onPause", "onStop", "onDestroy"));
        q1.setSampleAnswer("Activity lifecycle includes onCreate (initialization), onStart (visible), onResume (interactive), onPause (losing focus), onStop (hidden), and onDestroy (cleanup). Understanding this is crucial for resource management.");
        questions.add(q1);
        
        InterviewQuestion q2 = new InterviewQuestion("ANDROID", "Architecture",
            "What is MVVM architecture and how does it benefit Android development?",
            "Medium");
        q2.setExpectedKeywords(Arrays.asList("Model", "View", "ViewModel", "separation of concerns", "LiveData", "testing"));
        q2.setSampleAnswer("MVVM separates UI (View) from business logic (ViewModel) and data (Model). ViewModel survives configuration changes, LiveData enables reactive UI, and separation improves testability and maintainability.");
        questions.add(q2);
        
        InterviewQuestion q3 = new InterviewQuestion("ANDROID", "Performance",
            "How would you prevent memory leaks in Android?",
            "Hard");
        q3.setExpectedKeywords(Arrays.asList("context", "static reference", "Handler", "AsyncTask", "WeakReference", "lifecycle"));
        q3.setSampleAnswer("Avoid static context references, use WeakReferences, properly cancel AsyncTasks/Handlers, clear listeners, use lifecycle-aware components, and leverage LeakCanary for detection.");
        questions.add(q3);
        
        return questions;
    }
    
    private static List<InterviewQuestion> getHRQuestions() {
        List<InterviewQuestion> questions = new ArrayList<>();
        
        InterviewQuestion q1 = new InterviewQuestion("HR", "Behavioral",
            "Tell me about a time when you faced a challenging problem and how you solved it.",
            "Medium");
        q1.setExpectedKeywords(Arrays.asList("problem", "solution", "action", "result", "learning"));
        q1.setSampleAnswer("Use the STAR method: Situation (context), Task (challenge), Action (what you did), Result (outcome). Include what you learned and how it improved your skills.");
        questions.add(q1);
        
        InterviewQuestion q2 = new InterviewQuestion("HR", "Motivation",
            "Why do you want to work for our company?",
            "Easy");
        q2.setExpectedKeywords(Arrays.asList("company", "values", "mission", "growth", "innovation"));
        q2.setSampleAnswer("Research the company's mission, values, and culture. Connect your skills and interests to their work, showing genuine enthusiasm for their products/services and growth opportunities.");
        questions.add(q2);
        
        InterviewQuestion q3 = new InterviewQuestion("HR", "Teamwork",
            "Describe a situation where you had to work with a difficult team member.",
            "Medium");
        q3.setExpectedKeywords(Arrays.asList("communication", "conflict resolution", "collaboration", "compromise"));
        q3.setSampleAnswer("Discuss how you maintained professionalism, communicated openly, found common ground, and focused on project goals rather than personal differences. Highlight positive outcomes.");
        questions.add(q3);
        
        return questions;
    }
}
