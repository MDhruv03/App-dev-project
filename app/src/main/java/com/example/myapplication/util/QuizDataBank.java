package com.example.myapplication.util;

import com.example.myapplication.model.QuizQuestion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class QuizDataBank {

    private QuizDataBank() {
    }

    public static List<QuizQuestion> getQuestionsForSkills(List<String> skills) {
        List<QuizQuestion> all = new ArrayList<>();
        if (skills == null || skills.isEmpty()) {
            return all;
        }

        for (String rawSkill : skills) {
            String skill = rawSkill == null ? "" : rawSkill.trim().toLowerCase(Locale.ROOT);
            if (skill.isEmpty()) {
                continue;
            }

            switch (skill) {
                case "java":
                    addJavaQuestions(all);
                    break;
                case "python":
                    addPythonQuestions(all);
                    break;
                case "android":
                    addAndroidQuestions(all);
                    break;
                case "dsa":
                    addDsaQuestions(all);
                    break;
                case "sql":
                    addSqlQuestions(all);
                    break;
                case "javascript":
                case "js":
                    addJavaScriptQuestions(all);
                    break;
                case "react":
                    addReactQuestions(all);
                    break;
                case "ml":
                case "machine learning":
                    addMlQuestions(all);
                    break;
                case "git":
                    addGitQuestions(all);
                    break;
                default:
                    break;
            }
        }

        Collections.shuffle(all);
        return all.size() > 15 ? new ArrayList<>(all.subList(0, 15)) : all;
    }

    private static void addJavaQuestions(List<QuizQuestion> list) {
        list.add(q("Java", "Which OOP principle allows one interface with many implementations?", "Encapsulation", "Inheritance", "Polymorphism", "Abstraction", "C", "Easy", "Polymorphism lets objects be treated as a common type while behaving differently."));
        list.add(q("Java", "Which collection does NOT allow duplicate elements?", "ArrayList", "HashSet", "LinkedList", "Vector", "B", "Easy", "HashSet stores unique elements using hashing."));
        list.add(q("Java", "Which method starts a new thread in Java?", "run()", "start()", "execute()", "init()", "B", "Easy", "start() creates a new call stack and then invokes run()."));
        list.add(q("Java", "Which block always executes, whether exception occurs or not?", "try", "catch", "throw", "finally", "D", "Easy", "finally is used for cleanup and is executed in normal and exceptional flows."));
        list.add(q("Java", "What is the main benefit of generics?", "Faster runtime", "Type safety at compile time", "Smaller bytecode", "No casting needed by JVM", "B", "Medium", "Generics catch type errors during compilation and reduce unsafe casts."));
    }

    private static void addPythonQuestions(List<QuizQuestion> list) {
        list.add(q("Python", "Which structure is mutable in Python?", "tuple", "str", "list", "frozenset", "C", "Easy", "Lists are mutable; tuples and strings are immutable."));
        list.add(q("Python", "A decorator in Python is used to:", "Create classes", "Modify function behavior", "Optimize loops", "Define modules", "B", "Easy", "Decorators wrap functions to add behavior without changing function source."));
        list.add(q("Python", "What does this produce: [x*x for x in range(3)]?", "[1,4,9]", "[0,1,4]", "[0,1,4,9]", "[1,2,3]", "B", "Easy", "range(3) gives 0,1,2, so squares are 0,1,4."));
        list.add(q("Python", "The GIL primarily affects:", "Memory usage", "CPU-bound threading", "File I/O", "Syntax parsing", "B", "Medium", "GIL allows one thread to execute Python bytecode at a time, limiting CPU-bound parallelism."));
        list.add(q("Python", "How do you model inheritance in Python?", "class Child implements Parent", "class Child(Parent)", "class Child extends Parent", "inherit Child Parent", "B", "Easy", "Python uses parentheses in class definition to specify base classes."));
    }

    private static void addAndroidQuestions(List<QuizQuestion> list) {
        list.add(q("Android", "Which callback is called first in Activity lifecycle?", "onStart", "onResume", "onCreate", "onRestart", "C", "Easy", "onCreate is the first initialization callback for activity instance."));
        list.add(q("Android", "Room is primarily used for:", "Network requests", "Local database persistence", "Image caching", "Animations", "B", "Easy", "Room is a persistence library over SQLite."));
        list.add(q("Android", "Why use ViewModel in MVVM?", "For XML styling", "To survive config changes and hold UI data", "To replace RecyclerView", "To fetch only network data", "B", "Medium", "ViewModel keeps UI state across rotations and separates UI from data logic."));
        list.add(q("Android", "RecyclerView performance relies on:", "Singleton Fragments", "ViewHolder pattern", "BroadcastReceiver", "ContentProvider", "B", "Easy", "ViewHolder avoids repeated findViewById calls and reuses item views efficiently."));
        list.add(q("Android", "Intent is used to:", "Draw custom views", "Communicate between app components", "Access SQLite directly", "Create themes", "B", "Easy", "Intents trigger activity/service/broadcast communication."));
    }

    private static void addDsaQuestions(List<QuizQuestion> list) {
        list.add(q("DSA", "What is random access time complexity for array?", "O(1)", "O(log n)", "O(n)", "O(n log n)", "A", "Easy", "Arrays provide direct index-based access in constant time."));
        list.add(q("DSA", "Binary search tree average search complexity is:", "O(1)", "O(log n)", "O(n)", "O(n^2)", "B", "Medium", "Balanced BST operations are logarithmic on average."));
        list.add(q("DSA", "Which traversal is commonly used for shortest path in unweighted graph?", "DFS", "BFS", "Dijkstra", "Topological sort", "B", "Easy", "BFS explores by levels and gives shortest path in unweighted graphs."));
        list.add(q("DSA", "Merge sort time complexity is:", "O(n)", "O(n log n)", "O(log n)", "O(n^2)", "B", "Easy", "Merge sort divides and merges leading to O(n log n)."));
        list.add(q("DSA", "Big-O of nested loops each over n items is usually:", "O(n)", "O(log n)", "O(n^2)", "O(2n)", "C", "Easy", "Two full nested loops over n produce n*n operations."));
    }

    private static void addSqlQuestions(List<QuizQuestion> list) {
        list.add(q("SQL", "Which join returns only matching rows from both tables?", "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "FULL JOIN", "C", "Easy", "INNER JOIN returns rows where join condition matches on both tables."));
        list.add(q("SQL", "Primary purpose of an index is to:", "Reduce table size", "Speed up queries", "Enforce foreign keys", "Store backups", "B", "Easy", "Indexes improve lookup speed at cost of extra storage/writes."));
        list.add(q("SQL", "Which normal form removes transitive dependency?", "1NF", "2NF", "3NF", "BCNF", "C", "Medium", "3NF requires non-key attributes depend only on key."));
        list.add(q("SQL", "ACID property ensuring all-or-nothing transaction is:", "Consistency", "Isolation", "Durability", "Atomicity", "D", "Easy", "Atomicity guarantees entire transaction commits or rolls back."));
        list.add(q("SQL", "A foreign key is used to:", "Create indexes", "Link rows between tables", "Encrypt columns", "Sort result sets", "B", "Easy", "Foreign keys enforce referential relationship with primary key in another table."));
    }

    private static void addJavaScriptQuestions(List<QuizQuestion> list) {
        list.add(q("JavaScript", "A closure is:", "A blocked promise", "Function with preserved lexical scope", "A class constructor", "A DOM event", "B", "Medium", "Closures allow function access to outer scope variables after outer function returns."));
        list.add(q("JavaScript", "Promises represent:", "Synchronous loops", "Future completion/failure of async operation", "DOM node trees", "Type declarations", "B", "Easy", "Promise encapsulates eventual async result."));
        list.add(q("JavaScript", "Hoisting in JS means:", "Moving files to CDN", "Variable/function declarations are processed before execution", "Code minification", "Runtime optimization only", "B", "Medium", "Declarations are hoisted to top of scope during compilation phase."));
        list.add(q("JavaScript", "Event loop handles:", "Only DOM painting", "Only network stack", "Scheduling callbacks and async tasks", "Only promises", "C", "Medium", "Event loop manages call stack and task queues to execute async callbacks."));
        list.add(q("JavaScript", "Which is an ES6 feature?", "var only", "callback hell", "let/const and arrow functions", "with statement", "C", "Easy", "ES6 introduced block scoping and arrow syntax among many features."));
    }

    private static void addReactQuestions(List<QuizQuestion> list) {
        list.add(q("React", "Which hook manages state in functional components?", "useMemo", "useState", "useRef", "useEffect", "B", "Easy", "useState stores local component state."));
        list.add(q("React", "Virtual DOM is used to:", "Replace browser DOM permanently", "Compute minimal UI updates", "Store API responses", "Handle routing", "B", "Medium", "React diffs virtual trees to apply efficient real DOM updates."));
        list.add(q("React", "Props in React are:", "Mutable internal state", "Read-only inputs to components", "Global reducers", "DOM events", "B", "Easy", "Props are passed from parent and should not be mutated by child."));
        list.add(q("React", "Which hook handles side effects?", "useEffect", "useState", "useContext", "useId", "A", "Easy", "useEffect runs side effects like fetches/subscriptions."));
        list.add(q("React", "In class components, lifecycle method for cleanup before unmount is:", "componentDidMount", "componentWillUnmount", "shouldComponentUpdate", "render", "B", "Medium", "componentWillUnmount is used to clean timers/listeners."));
    }

    private static void addMlQuestions(List<QuizQuestion> list) {
        list.add(q("ML", "Supervised learning uses:", "Only unlabeled data", "Labeled input-output data", "Random noise only", "No training data", "B", "Easy", "Supervised learning maps inputs to known targets."));
        list.add(q("ML", "Overfitting means model:", "Performs well on train but poorly on new data", "Has too little capacity", "Cannot converge", "Always underestimates", "A", "Easy", "Overfit models memorize training patterns and generalize poorly."));
        list.add(q("ML", "Gradient descent is used to:", "Increase loss", "Optimize model parameters", "Select hardware", "Normalize labels", "B", "Medium", "It iteratively updates weights in direction of negative gradient."));
        list.add(q("ML", "Neural network activation functions are important because they:", "Add non-linearity", "Store datasets", "Reduce epochs", "Replace optimizers", "A", "Medium", "Without non-linearity stacked layers collapse to linear mapping."));
        list.add(q("ML", "Which metric is useful for imbalanced classification?", "Accuracy only", "F1-score", "MSE", "R2", "B", "Medium", "F1 balances precision and recall and is more informative on imbalance."));
    }

    private static void addGitQuestions(List<QuizQuestion> list) {
        list.add(q("Git", "git merge does what?", "Deletes branch history", "Combines histories of branches", "Resets remote", "Stashes all files", "B", "Easy", "merge creates a merge commit or fast-forward to combine branches."));
        list.add(q("Git", "git rebase primarily:", "Rewrites commits onto another base", "Creates tags", "Deletes remote branch", "Installs hooks", "A", "Medium", "Rebase reapplies commits on top of target branch for linear history."));
        list.add(q("Git", "git cherry-pick is used to:", "Pick all branches", "Apply specific commit(s) onto current branch", "Undo merge", "Force push", "B", "Medium", "cherry-pick copies chosen commit changes by hash."));
        list.add(q("Git", "A branch in Git is:", "A full repo copy", "A movable pointer to commits", "A stash entry", "A remote only feature", "B", "Easy", "Branches are lightweight pointers enabling parallel work."));
        list.add(q("Git", "git stash is used to:", "Delete working tree", "Temporarily save uncommitted changes", "Rename branch", "Squash commits", "B", "Easy", "stash stores local modifications so you can switch tasks safely."));
    }

    private static QuizQuestion q(String topic, String questionText, String optionA, String optionB,
                                  String optionC, String optionD, String correctAnswer,
                                  String difficulty, String explanation) {
        return new QuizQuestion(topic, questionText, optionA, optionB, optionC, optionD,
                correctAnswer, difficulty, explanation);
    }
}
