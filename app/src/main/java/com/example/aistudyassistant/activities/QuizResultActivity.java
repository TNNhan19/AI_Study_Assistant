package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.utils.Constants;
import com.google.android.material.button.MaterialButton;

public class QuizResultActivity extends AppCompatActivity {

    private TextView tvScore, tvPerformance, tvCorrectCount, tvWrongCount, tvPercent;
    private MaterialButton btnRetake, btnBackToDoc;

    private int totalQuestions;
    private int correctAnswers;
    private String documentId;
    private String documentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        totalQuestions = getIntent().getIntExtra("total_questions", 0);
        correctAnswers = getIntent().getIntExtra("correct_answers", 0);
        documentId = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_ID);
        documentName = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_NAME);

        initViews();
        displayResults();
        setupClickListeners();
    }

    private void initViews() {
        tvScore = findViewById(R.id.tv_score);
        tvPerformance = findViewById(R.id.tv_performance);
        tvCorrectCount = findViewById(R.id.tv_correct_count);
        tvWrongCount = findViewById(R.id.tv_wrong_count);
        tvPercent = findViewById(R.id.tv_percent);
        btnRetake = findViewById(R.id.btn_retake);
        btnBackToDoc = findViewById(R.id.btn_back_to_doc);
    }

    private void displayResults() {
        int wrongAnswers = totalQuestions - correctAnswers;
        int percent = totalQuestions > 0 ? (int) ((correctAnswers * 100.0) / totalQuestions) : 0;

        tvScore.setText(correctAnswers + "/" + totalQuestions);
        tvCorrectCount.setText(String.valueOf(correctAnswers));
        tvWrongCount.setText(String.valueOf(wrongAnswers));
        tvPercent.setText(percent + "%");

        // Performance label
        String performance;
        if (percent >= 90) performance = "🎉 Excellent!";
        else if (percent >= 70) performance = "👍 Good Job!";
        else if (percent >= 50) performance = "📚 Keep Practicing!";
        else performance = "💪 Try Again!";
        tvPerformance.setText(performance);

        // TODO: Save quiz result to Supabase
    }

    private void setupClickListeners() {
        btnRetake.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra(Constants.EXTRA_DOCUMENT_ID, documentId);
            intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, documentName);
            startActivity(intent);
            finish();
        });

        btnBackToDoc.setOnClickListener(v -> finish());
    }
}
