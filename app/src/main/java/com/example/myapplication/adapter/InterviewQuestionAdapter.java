package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.InterviewQuestion;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class InterviewQuestionAdapter extends RecyclerView.Adapter<InterviewQuestionAdapter.QuestionViewHolder> {
    
    private List<InterviewQuestion> questions;
    private OnQuestionClickListener listener;
    
    public interface OnQuestionClickListener {
        void onClick(InterviewQuestion question);
    }
    
    public InterviewQuestionAdapter() {
        this.questions = new ArrayList<>();
    }
    
    public void setQuestions(List<InterviewQuestion> questions) {
        this.questions = questions;
        notifyDataSetChanged();
    }
    
    public void setOnQuestionClickListener(OnQuestionClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_interview_question, parent, false);
        return new QuestionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        InterviewQuestion question = questions.get(position);
        holder.bind(question, position + 1);
    }
    
    @Override
    public int getItemCount() {
        return questions.size();
    }
    
    class QuestionViewHolder extends RecyclerView.ViewHolder {
        
        TextView tvQuestionNumber;
        TextView tvQuestion;
        Chip chipTopic;
        Chip chipDifficulty;
        
        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            
            tvQuestionNumber = itemView.findViewById(R.id.tv_question_number);
            tvQuestion = itemView.findViewById(R.id.tv_question);
            chipTopic = itemView.findViewById(R.id.chip_topic);
            chipDifficulty = itemView.findViewById(R.id.chip_difficulty);
        }
        
        public void bind(InterviewQuestion question, int questionNumber) {
            tvQuestionNumber.setText("Q" + questionNumber);
            tvQuestion.setText(question.getQuestion());
            chipTopic.setText(question.getTopic());
            
            // Set difficulty chip
            chipDifficulty.setText(question.getDifficulty());
            chipDifficulty.setChipBackgroundColorResource(getDifficultyColor(question.getDifficulty()));
            
            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(question);
                }
            });
        }
        
        private int getDifficultyColor(String difficulty) {
            switch (difficulty.toLowerCase()) {
                case "easy": return R.color.difficulty_easy;
                case "medium": return R.color.difficulty_medium;
                case "hard": return R.color.difficulty_hard;
                default: return R.color.chip_default;
            }
        }
    }
}
