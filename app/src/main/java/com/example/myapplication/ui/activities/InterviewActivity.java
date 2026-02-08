package com.example.myapplication.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.model.InterviewProgress;
import com.example.myapplication.model.InterviewQuestion;
import com.example.myapplication.viewmodel.InterviewViewModel;
import com.google.android.material.button.MaterialButton;
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
                showFeedback(progress);
                btnSubmit.setEnabled(true);
            });
        });
    }
    
    private void showFeedback(InterviewProgress progress) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Score: " + String.format("%.1f", progress.getScore()) + "/100");
        builder.setMessage(progress.getFeedback());
        builder.setPositiveButton("Continue", (dialog, which) -> {
            btnSubmit.setVisibility(View.GONE);
            
            if (viewModel.getCurrentQuestionNumber() < viewModel.getTotalQuestions()) {
                btnNext.setVisibility(View.VISIBLE);
            } else {
                btnFinish.setVisibility(View.VISIBLE);
            }
        });
        builder.setCancelable(false);
        builder.show();
    }
    
    private void nextQuestion() {
        boolean hasNext = viewModel.nextQuestion();
        if (!hasNext) {
            btnNext.setVisibility(View.GONE);
            btnFinish.setVisibility(View.VISIBLE);
        }
    }
    
    private void finishInterview() {
        new AlertDialog.Builder(this)
            .setTitle("Interview Complete!")
            .setMessage("Great job! Your answers have been recorded. Check the Analytics tab to see your progress.")
            .setPositiveButton("Finish", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }
    
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
            .setTitle("Exit Interview?")
            .setMessage("Your progress will be lost if you exit now.")
            .setPositiveButton("Exit", (dialog, which) -> finish())
            .setNegativeButton("Continue", null)
            .show();
    }
}
