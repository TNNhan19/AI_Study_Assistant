package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.models.QuizResult;
import com.example.aistudyassistant.repositories.QuizRepository;
import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.button.MaterialButton;

public class QuizResultActivity extends AppCompatActivity {

    private TextView tvScore, tvPerformance, tvCorrectCount, tvWrongCount, tvPercent;
    private MaterialButton btnRetake, btnBackToDoc;

    private int totalQuestions;
    private int correctAnswers;
    private String documentId;
    private String documentName;
    private String documentUrl;
    private String documentType;
    private String topicId;
    private String quizId;
    private String projectId;
    private String quizSetId;
    private String difficulty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        totalQuestions = getIntent().getIntExtra("total_questions", 0);
        correctAnswers = getIntent().getIntExtra("correct_answers", 0);
        quizId = getIntent().getStringExtra(Constants.EXTRA_QUIZ_ID);
        documentId = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_ID);
        documentName = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_NAME);
        documentUrl = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_URL);
        documentType = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_TYPE);
        topicId = getIntent().getStringExtra(Constants.EXTRA_TOPIC_ID);
        projectId = getIntent().getStringExtra(Constants.EXTRA_PROJECT_ID);
        quizSetId = getIntent().getStringExtra(Constants.EXTRA_QUIZ_SET_ID);
        difficulty = getIntent().getStringExtra(Constants.EXTRA_QUIZ_DIFFICULTY);

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

        saveQuizResult(wrongAnswers);
    }

    private void saveQuizResult(int wrongAnswers) {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        if (userId == null || userId.isEmpty() || totalQuestions <= 0) return;

        QuizResult result = new QuizResult();
        result.setUserId(userId);
        result.setQuizId(quizId);
        result.setDocumentId(documentId);
        result.setProjectId(projectId);
        result.setScore(correctAnswers);
        result.setTotalQuestions(totalQuestions);
        result.setCorrectCount(correctAnswers);
        result.setWrongCount(wrongAnswers);
        result.setCompletedAt(System.currentTimeMillis());

        QuizRepository.getInstance().saveQuizResult(result, new ApiCallback<QuizResult>() {
            @Override
            public void onSuccess(QuizResult savedResult) {
                // Result screen already shows the score; no extra UI update needed.
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(
                        QuizResultActivity.this,
                        "Could not save quiz result: " + errorMessage,
                        Toast.LENGTH_LONG
                ).show());
            }
        });
    }

    private void setupClickListeners() {
        btnRetake.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra(Constants.EXTRA_DOCUMENT_ID, documentId);
            intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, documentName);
            intent.putExtra(Constants.EXTRA_DOCUMENT_URL, documentUrl);
            intent.putExtra(Constants.EXTRA_DOCUMENT_TYPE, documentType);
            intent.putExtra(Constants.EXTRA_PROJECT_ID, projectId);
            intent.putExtra(Constants.EXTRA_TOPIC_ID, topicId);
            intent.putExtra(Constants.EXTRA_QUIZ_SET_ID, quizSetId);
            intent.putExtra(Constants.EXTRA_QUIZ_DIFFICULTY, difficulty);
            startActivity(intent);
            finish();
        });

        btnBackToDoc.setOnClickListener(v -> finish());
    }
}
