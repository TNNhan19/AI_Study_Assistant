package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.models.QuizQuestion;
import com.example.aistudyassistant.repositories.AIContentRepository;
import com.example.aistudyassistant.services.AIProcessingService;
import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private TextView tvQuestionNumber, tvQuestion, tvProgress, tvOptionA, tvOptionB, tvOptionC, tvOptionD;
    private TextView tvFeedbackTitle, tvExplanation, tvQuestionCount;
    private LinearLayout layoutOptionA, layoutOptionB, layoutOptionC, layoutOptionD;
    private LinearLayout layoutLoading;
    private CardView cardFeedback;
    private ProgressBar progressQuiz;
    private MaterialButton btnNext;
    private ImageButton btnBack;

    private List<QuizQuestion> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int correctCount = 0;
    private boolean hasAnswered = false;

    private String documentId;
    private String documentName;
    private String documentUrl;
    private String documentType;
    private String topicId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        documentId = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_ID);
        documentName = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_NAME);
        documentUrl = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_URL);
        documentType = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_TYPE);
        topicId = getIntent().getStringExtra(Constants.EXTRA_TOPIC_ID);

        initViews();
        setupClickListeners();
        loadQuestions();
    }

    private void initViews() {
        tvQuestionNumber = findViewById(R.id.tv_question_number);
        tvQuestion = findViewById(R.id.tv_question);
        tvOptionA = findViewById(R.id.tv_option_a);
        tvOptionB = findViewById(R.id.tv_option_b);
        tvOptionC = findViewById(R.id.tv_option_c);
        tvOptionD = findViewById(R.id.tv_option_d);
        layoutOptionA = findViewById(R.id.layout_option_a);
        layoutOptionB = findViewById(R.id.layout_option_b);
        layoutOptionC = findViewById(R.id.layout_option_c);
        layoutOptionD = findViewById(R.id.layout_option_d);
        tvFeedbackTitle = findViewById(R.id.tv_feedback_title);
        tvExplanation = findViewById(R.id.tv_explanation);
        tvQuestionCount = findViewById(R.id.tv_question_count);
        cardFeedback = findViewById(R.id.card_feedback);
        progressQuiz = findViewById(R.id.progress_quiz);
        btnNext = findViewById(R.id.btn_next);
        btnBack = findViewById(R.id.btn_back);
        layoutLoading = findViewById(R.id.layout_loading);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        layoutOptionA.setOnClickListener(v -> checkAnswer("A"));
        layoutOptionB.setOnClickListener(v -> checkAnswer("B"));
        layoutOptionC.setOnClickListener(v -> checkAnswer("C"));
        layoutOptionD.setOnClickListener(v -> checkAnswer("D"));

        btnNext.setOnClickListener(v -> {
            if (!hasAnswered) {
                Toast.makeText(this, "Please select an answer first", Toast.LENGTH_SHORT).show();
                return;
            }
            currentIndex++;
            if (currentIndex < questions.size()) {
                displayQuestion(currentIndex);
            } else {
                showQuizResult();
            }
        });
    }

    private void loadQuestions() {
        setLoading(true);
        String userId = SharedPrefManager.getInstance(this).getUserId();
        AIContentRepository.getInstance().getQuizQuestionsByDocument(
                userId, documentId,
                new ApiCallback<List<QuizQuestion>>() {
            @Override
            public void onSuccess(List<QuizQuestion> result) {
                runOnUiThread(() -> {
                    if (result.isEmpty()) {
                        generateQuiz();
                    } else {
                        setLoading(false);
                        showQuestions(result);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(QuizActivity.this,
                            errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void generateQuiz() {
        setLoading(true);
        AIProcessingService.getInstance(this).generateQuiz(
                buildDocument(), 10,
                new ApiCallback<List<QuizQuestion>>() {
            @Override
            public void onSuccess(List<QuizQuestion> result) {
                saveQuestions(result);
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                Toast.makeText(QuizActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onWaitingForNetwork() {
                Toast.makeText(QuizActivity.this,
                        "Mất kết nối. Quiz sẽ tự tạo lại khi có mạng.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveQuestions(List<QuizQuestion> generatedQuestions) {
        AIContentRepository.getInstance().saveQuizQuestions(
                buildDocument(), generatedQuestions,
                new ApiCallback<List<QuizQuestion>>() {
            @Override
            public void onSuccess(List<QuizQuestion> savedQuestions) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showQuestions(savedQuestions);
                    Toast.makeText(QuizActivity.this,
                            "Quiz saved", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(QuizActivity.this,
                            errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showQuestions(List<QuizQuestion> loadedQuestions) {
        questions.clear();
        questions.addAll(loadedQuestions);
        currentIndex = 0;
        correctCount = 0;
        displayQuestion(0);
    }

    private void displayQuestion(int index) {
        if (index >= questions.size()) return;
        hasAnswered = false;

        QuizQuestion q = questions.get(index);
        tvQuestionNumber.setText("Question " + (index + 1));
        tvQuestion.setText(q.getQuestion());
        tvOptionA.setText(q.getOptionA());
        tvOptionB.setText(q.getOptionB());
        tvOptionC.setText(q.getOptionC());
        tvOptionD.setText(q.getOptionD());

        // Reset option backgrounds
        resetOptions();

        // Update progress
        int progress = (int) ((index * 100.0) / questions.size());
        progressQuiz.setProgress(progress);
        tvQuestionCount.setText((index + 1) + "/" + questions.size());

        cardFeedback.setVisibility(View.GONE);
        btnNext.setText(index == questions.size() - 1 ? "Finish Quiz" : "Next Question");
    }

    private void checkAnswer(String selected) {
        if (hasAnswered || questions.isEmpty()) return;
        hasAnswered = true;

        QuizQuestion q = questions.get(currentIndex);
        boolean isCorrect = selected.equals(q.getCorrectAnswer());

        if (isCorrect) {
            correctCount++;
            highlightOption(selected, true);
            showFeedback(true, q.getExplanation());
        } else {
            highlightOption(selected, false);
            highlightOption(q.getCorrectAnswer(), true);
            showFeedback(false, q.getExplanation());
        }
    }

    private void highlightOption(String letter, boolean isCorrect) {
        LinearLayout layout = getOptionLayout(letter);
        if (layout == null) return;
        layout.setBackgroundResource(isCorrect
                ? R.drawable.bg_quiz_option_correct
                : R.drawable.bg_quiz_option_wrong);
    }

    private LinearLayout getOptionLayout(String letter) {
        switch (letter) {
            case "A": return layoutOptionA;
            case "B": return layoutOptionB;
            case "C": return layoutOptionC;
            case "D": return layoutOptionD;
            default: return null;
        }
    }

    private void resetOptions() {
        layoutOptionA.setBackgroundResource(R.drawable.bg_quiz_option);
        layoutOptionB.setBackgroundResource(R.drawable.bg_quiz_option);
        layoutOptionC.setBackgroundResource(R.drawable.bg_quiz_option);
        layoutOptionD.setBackgroundResource(R.drawable.bg_quiz_option);
    }

    private void showFeedback(boolean isCorrect, String explanation) {
        cardFeedback.setVisibility(View.VISIBLE);
        tvFeedbackTitle.setText(isCorrect ? "✅ Correct!" : "❌ Incorrect");
        tvFeedbackTitle.setTextColor(getResources().getColor(
                isCorrect ? R.color.quiz_correct : R.color.quiz_wrong));
        tvExplanation.setText(explanation);
        cardFeedback.setCardBackgroundColor(getResources().getColor(
                isCorrect ? R.color.quiz_correct_bg : R.color.quiz_wrong_bg));
    }

    private void showQuizResult() {
        Intent intent = new Intent(this, QuizResultActivity.class);
        intent.putExtra("total_questions", questions.size());
        intent.putExtra("correct_answers", correctCount);
        intent.putExtra(Constants.EXTRA_DOCUMENT_ID, documentId);
        intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, documentName);
        intent.putExtra(Constants.EXTRA_DOCUMENT_URL, documentUrl);
        intent.putExtra(Constants.EXTRA_DOCUMENT_TYPE, documentType);
        intent.putExtra(Constants.EXTRA_TOPIC_ID, topicId);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        layoutLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private Document buildDocument() {
        Document document = new Document();
        document.setId(documentId);
        document.setUserId(SharedPrefManager.getInstance(this).getUserId());
        document.setName(documentName);
        document.setFilePath(documentUrl);
        document.setFileType(documentType);
        document.setTopicId(topicId);
        return document;
    }
}
