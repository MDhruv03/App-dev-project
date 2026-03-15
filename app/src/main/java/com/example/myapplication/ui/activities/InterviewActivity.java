package com.example.myapplication.ui.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.model.InterviewProgress;
import com.example.myapplication.model.InterviewQuestion;
import com.example.myapplication.viewmodel.InterviewViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

public class InterviewActivity extends AppCompatActivity {
    
    private InterviewViewModel viewModel;
    
    private TextView tvProgress;
    private TextView tvQuestionNumber;
    private TextView tvQuestion;
    private TextView tvTopic;
    private TextView tvDifficulty;
    private TextInputEditText etAnswer;
    private MaterialButton btnSubmit;
    private MaterialButton btnNext;
    private MaterialButton btnFinish;
    private LinearProgressIndicator progressIndicator;
    private MaterialCardView cardFeedback;
    private TextView tvFeedbackScore;
    private Chip chipVerdict;
    private Chip chipEvaluationTag;
    private TextView tvFeedbackText;
    private TextView tvStrengths;
    private TextView tvImprovements;

    private double sessionTotalScore = 0.0;
    private int sessionAnsweredCount = 0;
    
    private String selectedDomain;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interview);
        
        selectedDomain = getIntent().getStringExtra("DOMAIN");
        if (selectedDomain == null) {
            selectedDomain = "SDE";
        }
        
        initializeViews();
        setupViewModel();
        setupListeners();
        setupBackPress();
        
        viewModel.loadQuestionsByDomain(selectedDomain);
    }
    
    private void initializeViews() {
        tvProgress = findViewById(R.id.tv_progress);
        tvQuestionNumber = findViewById(R.id.tv_question_number);
        tvQuestion = findViewById(R.id.tv_question);
        tvTopic = findViewById(R.id.tv_topic);
        tvDifficulty = findViewById(R.id.tv_difficulty);
        etAnswer = findViewById(R.id.et_answer);
        btnSubmit = findViewById(R.id.btn_submit);
        btnNext = findViewById(R.id.btn_next);
        btnFinish = findViewById(R.id.btn_finish);
        progressIndicator = findViewById(R.id.progress_indicator);
        cardFeedback = findViewById(R.id.card_feedback);
        tvFeedbackScore = findViewById(R.id.tv_feedback_score);
        chipVerdict = findViewById(R.id.chip_verdict);
        chipEvaluationTag = findViewById(R.id.chip_evaluation_tag);
        tvFeedbackText = findViewById(R.id.tv_feedback_text);
        tvStrengths = findViewById(R.id.tv_strengths);
        tvImprovements = findViewById(R.id.tv_improvements);
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(InterviewViewModel.class);
        
        viewModel.getCurrentQuestion().observe(this, this::displayQuestion);
        
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                progressIndicator.setVisibility(View.VISIBLE);
            } else {
                progressIndicator.setVisibility(View.GONE);
            }
        });
    }
    
    private void setupListeners() {
        btnSubmit.setOnClickListener(v -> submitAnswer());
        btnNext.setOnClickListener(v -> nextQuestion());
        btnFinish.setOnClickListener(v -> finishInterview());
    }
    
    private void displayQuestion(InterviewQuestion question) {
        if (question == null) return;
        
        int currentNum = viewModel.getCurrentQuestionNumber();
        int total = viewModel.getTotalQuestions();
        
        tvProgress.setText(currentNum + " of " + total);
        tvQuestionNumber.setText("Question " + currentNum);
        tvQuestion.setText(question.getQuestion());
        tvTopic.setText(question.getTopic());
        tvDifficulty.setText(question.getDifficulty());
        
        etAnswer.setText("");
        etAnswer.setEnabled(true);
        
        btnSubmit.setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.GONE);
        btnFinish.setVisibility(View.GONE);
        cardFeedback.setVisibility(View.GONE);
        
        progressIndicator.setMax(total);
        progressIndicator.setProgress(currentNum);
    }
    
    private void submitAnswer() {
        String answer = etAnswer.getText().toString().trim();
        
        if (answer.isEmpty()) {
            Toast.makeText(this, "Please provide an answer", Toast.LENGTH_SHORT).show();
            return;
        }
        
        etAnswer.setEnabled(false);
        btnSubmit.setEnabled(false);
        
        viewModel.submitAnswer(answer, progress -> {
            runOnUiThread(() -> {
                sessionTotalScore += progress.getScore();
                sessionAnsweredCount++;
                showFeedback(progress);
                btnSubmit.setEnabled(true);
            });
        });
    }
    
    private void showFeedback(InterviewProgress progress) {
        cardFeedback.setVisibility(View.VISIBLE);

        tvFeedbackScore.setText(String.format("%.1f / 100", progress.getScore()));
        chipVerdict.setText(progress.getVerdict() == null ? "Fair" : progress.getVerdict());
        chipEvaluationTag.setText(progress.isAiEvaluated() ? "AI Evaluated ✨" : "Local Evaluation");
        tvFeedbackText.setText(progress.getFeedback());
        tvStrengths.setText("✔ " + safe(progress.getStrengths(), "You gave a valid attempt with relevant context."));
        tvImprovements.setText("⚠ " + safe(progress.getImprovements(), "Add key concepts and specific examples."));

        styleVerdictChip(progress.getVerdict());
        styleSourceChip(progress.isAiEvaluated());

        btnSubmit.setVisibility(View.GONE);
        if (viewModel.getCurrentQuestionNumber() < viewModel.getTotalQuestions()) {
            btnNext.setVisibility(View.VISIBLE);
        } else {
            btnFinish.setVisibility(View.VISIBLE);
        }
    }

    private void styleVerdictChip(String verdict) {
        String normalized = verdict == null ? "Fair" : verdict.trim();
        int backgroundColor;
        switch (normalized) {
            case "Excellent":
                backgroundColor = Color.parseColor("#1E8E3E");
                break;
            case "Good":
                backgroundColor = Color.parseColor("#1565C0");
                break;
            case "Poor":
                backgroundColor = Color.parseColor("#C62828");
                break;
            case "Fair":
            default:
                backgroundColor = Color.parseColor("#EF6C00");
                break;
        }
        chipVerdict.setChipBackgroundColor(ColorStateList.valueOf(backgroundColor));
        chipVerdict.setTextColor(Color.WHITE);
    }

    private void styleSourceChip(boolean aiEvaluated) {
        int backgroundColor = aiEvaluated ? Color.parseColor("#7B1FA2") : Color.parseColor("#455A64");
        chipEvaluationTag.setChipBackgroundColor(ColorStateList.valueOf(backgroundColor));
        chipEvaluationTag.setTextColor(Color.WHITE);
    }

    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
    
    private void nextQuestion() {
        boolean hasNext = viewModel.nextQuestion();
        if (!hasNext) {
            btnNext.setVisibility(View.GONE);
            btnFinish.setVisibility(View.VISIBLE);
        }
    }
    
    private void finishInterview() {
        int totalQuestions = viewModel.getTotalQuestions();
        int totalPossiblePoints = totalQuestions * 100;
        int earnedPoints = (int) Math.round(sessionTotalScore);
        double percentage = totalPossiblePoints > 0 ? (sessionTotalScore / totalPossiblePoints) * 100.0 : 0.0;

        String performanceLabel;
        if (percentage >= 80.0) {
            performanceLabel = "Excellent! 🔥";
        } else if (percentage >= 50.0) {
            performanceLabel = "Good Job! 👍";
        } else {
            performanceLabel = "Keep Practicing! 💪";
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_interview_result, null);
        TextView tvFinalScore = dialogView.findViewById(R.id.tvFinalScore);
        TextView tvPerformanceLabel = dialogView.findViewById(R.id.tvPerformanceLabel);
        TextView tvScoreBreakdown = dialogView.findViewById(R.id.tvScoreBreakdown);
        MaterialButton btnPracticeAgain = dialogView.findViewById(R.id.btnPracticeAgain);
        MaterialButton btnGoHome = dialogView.findViewById(R.id.btnGoHome);

        tvFinalScore.setText(earnedPoints + " / " + totalPossiblePoints + " pts");
        tvPerformanceLabel.setText(performanceLabel);
        tvScoreBreakdown.setText(sessionAnsweredCount + " out of " + totalQuestions + " questions answered");

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create();

        btnPracticeAgain.setOnClickListener(v -> {
            dialog.dismiss();
            Intent restartIntent = getIntent();
            finish();
            startActivity(restartIntent);
        });

        btnGoHome.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }
    
    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(InterviewActivity.this)
                    .setTitle("Exit Interview?")
                    .setMessage("Your progress will be lost if you exit now.")
                    .setPositiveButton("Exit", (dialog, which) -> finish())
                    .setNegativeButton("Continue", null)
                    .show();
            }
        });
    }
}
