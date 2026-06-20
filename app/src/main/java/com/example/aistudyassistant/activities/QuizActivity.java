package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.GeminiClient;
import com.example.aistudyassistant.models.QuizQuestion;
import com.example.aistudyassistant.models.QuizResult;
import com.example.aistudyassistant.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private TextView tvQuestionNumber, tvQuestion, tvProgress, tvOptionA, tvOptionB, tvOptionC, tvOptionD;
    private TextView tvFeedbackTitle, tvExplanation, tvQuestionCount;
    private LinearLayout layoutOptionA, layoutOptionB, layoutOptionC, layoutOptionD;
    private LinearLayout layoutLoading;
    private CardView cardFeedback;
    private ProgressBar progressQuiz;
    private MaterialButton btnNext, btnGenerate;
    private ImageButton btnBack;

    private List<QuizQuestion> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int correctCount = 0;
    private boolean hasAnswered = false;

    private String documentId;
    private String documentName;
    private String documentUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        documentId = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_ID);
        documentName = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_NAME);
        documentUrl = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_URL);

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
        // TODO: First check Supabase for existing quiz questions
        // If not found, generate via Gemini

        // MOCK: Generate with mock data for now
        generateQuiz();
    }

    private void generateQuiz() {
        setLoading(true);

        new Thread(() -> {
            String documentText = "Sample document content."; // TODO: Load actual document
            String responseJson = GeminiClient.getInstance().generateQuiz(documentText, 10);

            runOnUiThread(() -> {
                setLoading(false);
                if (responseJson != null) {
                    parseAndDisplayQuiz(responseJson);
                } else {
                    // Load demo questions if API not configured
                    loadDemoQuestions();
                }
            });
        }).start();
    }

    private void parseAndDisplayQuiz(String json) {
        try {
            String cleaned = json.trim();
            if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
            if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);

            JsonArray array = JsonParser.parseString(cleaned.trim()).getAsJsonArray();
            questions.clear();
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                QuizQuestion q = new QuizQuestion(
                        obj.get("question").getAsString(),
                        obj.get("optionA").getAsString(),
                        obj.get("optionB").getAsString(),
                        obj.get("optionC").getAsString(),
                        obj.get("optionD").getAsString(),
                        obj.get("correctAnswer").getAsString(),
                        obj.get("explanation").getAsString()
                );
                questions.add(q);
            }
            currentIndex = 0;
            correctCount = 0;
            displayQuestion(0);
        } catch (Exception e) {
            loadDemoQuestions();
        }
    }

    private void loadDemoQuestions() {
        questions.clear();
        questions.add(new QuizQuestion(
                "What protocol ensures reliable data transmission?",
                "UDP", "TCP", "HTTP", "FTP",
                "B", "TCP (Transmission Control Protocol) is connection-oriented and ensures reliable delivery."));
        questions.add(new QuizQuestion(
                "Which layer of OSI model handles routing?",
                "Application", "Transport", "Network", "Data Link",
                "C", "The Network layer handles routing and logical addressing (IP)."));
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
        if (hasAnswered) return;
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
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        layoutLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
