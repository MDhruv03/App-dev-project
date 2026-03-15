package com.example.myapplication.ui.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.QuizQuestion;
import com.example.myapplication.util.QuizDataBank;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private LinearProgressIndicator quizProgress;
    private TextView tvQuizProgress;
    private TextView tvQuizTopic;
    private TextView tvQuizQuestion;
    private TextView tvExplanation;
    private MaterialButton btnOptionA;
    private MaterialButton btnOptionB;
    private MaterialButton btnOptionC;
    private MaterialButton btnOptionD;
    private MaterialButton btnNextQuestion;

    private final List<QuizQuestion> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;

    private ColorStateList defaultTint;
    private ColorStateList defaultTextColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        initializeViews();
        loadQuestions();
        setupListeners();
        showQuestion();
    }

    private void initializeViews() {
        quizProgress = findViewById(R.id.quizProgress);
        tvQuizProgress = findViewById(R.id.tvQuizProgress);
        tvQuizTopic = findViewById(R.id.tvQuizTopic);
        tvQuizQuestion = findViewById(R.id.tvQuizQuestion);
        tvExplanation = findViewById(R.id.tvExplanation);
        btnOptionA = findViewById(R.id.btnOptionA);
        btnOptionB = findViewById(R.id.btnOptionB);
        btnOptionC = findViewById(R.id.btnOptionC);
        btnOptionD = findViewById(R.id.btnOptionD);
        btnNextQuestion = findViewById(R.id.btnNextQuestion);

        defaultTint = btnOptionA.getBackgroundTintList();
        defaultTextColor = btnOptionA.getTextColors();
    }

    private void loadQuestions() {
        String skillsCsv = getIntent().getStringExtra("USER_SKILLS");
        List<String> skills = parseSkills(skillsCsv);
        questions.clear();
        questions.addAll(QuizDataBank.getQuestionsForSkills(skills));

        if (questions.isEmpty()) {
            Toast.makeText(this, "No quiz questions available for selected skills", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupListeners() {
        btnOptionA.setOnClickListener(v -> onOptionSelected("A", btnOptionA));
        btnOptionB.setOnClickListener(v -> onOptionSelected("B", btnOptionB));
        btnOptionC.setOnClickListener(v -> onOptionSelected("C", btnOptionC));
        btnOptionD.setOnClickListener(v -> onOptionSelected("D", btnOptionD));

        btnNextQuestion.setOnClickListener(v -> {
            currentIndex++;
            if (currentIndex >= questions.size()) {
                showResultDialog();
            } else {
                showQuestion();
            }
        });
    }

    private void showQuestion() {
        QuizQuestion question = questions.get(currentIndex);

        tvQuizProgress.setText("Question " + (currentIndex + 1) + " of " + questions.size());
        tvQuizTopic.setText(question.getTopic() + " - " + question.getDifficulty());
        tvQuizQuestion.setText(question.getQuestionText());

        btnOptionA.setText("A. " + question.getOptionA());
        btnOptionB.setText("B. " + question.getOptionB());
        btnOptionC.setText("C. " + question.getOptionC());
        btnOptionD.setText("D. " + question.getOptionD());

        int progressPercent = (int) (((currentIndex + 1) * 100.0f) / questions.size());
        quizProgress.setProgressCompat(progressPercent, true);

        resetOptionButtons();
        setOptionButtonsEnabled(true);
        tvExplanation.setVisibility(TextView.GONE);
        btnNextQuestion.setVisibility(MaterialButton.GONE);
    }

    private void onOptionSelected(String selected, MaterialButton selectedButton) {
        QuizQuestion question = questions.get(currentIndex);
        String correct = question.getCorrectAnswer();

        setOptionButtonsEnabled(false);

        MaterialButton correctButton = getButtonForAnswer(correct);
        if (correctButton != null) {
            tintButton(correctButton, R.color.success, true);
        }

        if (selected.equalsIgnoreCase(correct)) {
            score++;
        } else {
            tintButton(selectedButton, R.color.error, false);
        }

        tvExplanation.setText(question.getExplanation());
        tvExplanation.setVisibility(TextView.VISIBLE);

        btnNextQuestion.setText(currentIndex == questions.size() - 1 ? "Finish Quiz" : "Next Question");
        btnNextQuestion.setVisibility(MaterialButton.VISIBLE);
    }

    private void showResultDialog() {
        int total = questions.size();
        int percentage = total == 0 ? 0 : (int) Math.round((score * 100.0) / total);

        String performance;
        if (percentage >= 80) {
            performance = "Expert! 🏆";
        } else if (percentage >= 60) {
            performance = "Good! 👍";
        } else {
            performance = "Keep Learning! 📚";
        }

        String message = "Score: " + score + " / " + total + " correct\n"
                + "Percentage: " + percentage + "%\n"
                + "Performance: " + performance;

        new MaterialAlertDialogBuilder(this)
            .setTitle("Quiz Complete! 🎯")
            .setMessage(message)
            .setPositiveButton("Retake Quiz", (dialog, which) -> retakeQuiz())
            .setNegativeButton("Go Home", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    private void retakeQuiz() {
        currentIndex = 0;
        score = 0;
        showQuestion();
    }

    private void setOptionButtonsEnabled(boolean enabled) {
        btnOptionA.setEnabled(enabled);
        btnOptionB.setEnabled(enabled);
        btnOptionC.setEnabled(enabled);
        btnOptionD.setEnabled(enabled);
    }

    private void resetOptionButtons() {
        btnOptionA.setBackgroundTintList(defaultTint);
        btnOptionB.setBackgroundTintList(defaultTint);
        btnOptionC.setBackgroundTintList(defaultTint);
        btnOptionD.setBackgroundTintList(defaultTint);

        btnOptionA.setTextColor(defaultTextColor);
        btnOptionB.setTextColor(defaultTextColor);
        btnOptionC.setTextColor(defaultTextColor);
        btnOptionD.setTextColor(defaultTextColor);
    }

    private void tintButton(@NonNull MaterialButton button, int colorRes, boolean keepTextWhite) {
        int color = ContextCompat.getColor(this, colorRes);
        button.setBackgroundTintList(ColorStateList.valueOf(color));
        if (keepTextWhite) {
            button.setTextColor(ContextCompat.getColor(this, R.color.on_primary));
        }
    }

    private MaterialButton getButtonForAnswer(String answer) {
        if ("A".equalsIgnoreCase(answer)) return btnOptionA;
        if ("B".equalsIgnoreCase(answer)) return btnOptionB;
        if ("C".equalsIgnoreCase(answer)) return btnOptionC;
        if ("D".equalsIgnoreCase(answer)) return btnOptionD;
        return null;
    }

    private List<String> parseSkills(String csv) {
        List<String> skills = new ArrayList<>();
        if (csv == null || csv.trim().isEmpty()) {
            return skills;
        }

        String[] parts = csv.split(",");
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                skills.add(part.trim());
            }
        }
        return skills;
    }
}
