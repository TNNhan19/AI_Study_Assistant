package com.example.aistudyassistant.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.adapters.QuizReviewAdapter;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.QuizQuestion;
import com.example.aistudyassistant.repositories.AIContentRepository;
import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizReviewActivity extends AppCompatActivity {

    private TextView tvDocument, tvSummary, tvEmpty;
    private ProgressBar progressBar;
    private RecyclerView rvQuizReview;
    private QuizReviewAdapter adapter;

    private String documentId;
    private String documentName;
    private String quizSetId;
    private String answerData;
    private int score;
    private int totalQuestions;
    private int correctCount;
    private int wrongCount;
    private boolean triedDocumentFallback;

    private final Map<String, String> answersByQuestionId = new HashMap<>();
    private final Map<Integer, String> answersByOrderIndex = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_review);

        SupabaseClient.getInstance().setAccessToken(
                SharedPrefManager.getInstance(this).getAccessToken()
        );
        readExtras();
        initViews();
        setupRecyclerView();
        renderSummary();
        parseAnswerData();
        loadQuestions();
    }

    private void readExtras() {
        documentId = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_ID);
        documentName = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_NAME);
        quizSetId = getIntent().getStringExtra(Constants.EXTRA_QUIZ_SET_ID);
        answerData = getIntent().getStringExtra(Constants.EXTRA_QUIZ_ANSWER_DATA);
        score = getIntent().getIntExtra("score", 0);
        totalQuestions = getIntent().getIntExtra("total_questions", 0);
        correctCount = getIntent().getIntExtra("correct_count", 0);
        wrongCount = getIntent().getIntExtra("wrong_count", 0);
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        tvDocument = findViewById(R.id.tv_review_document);
        tvSummary = findViewById(R.id.tv_review_summary);
        tvEmpty = findViewById(R.id.tv_review_empty);
        progressBar = findViewById(R.id.progress_bar);
        rvQuizReview = findViewById(R.id.rv_quiz_review);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new QuizReviewAdapter(this);
        rvQuizReview.setLayoutManager(new LinearLayoutManager(this));
        rvQuizReview.setAdapter(adapter);
    }

    private void renderSummary() {
        tvDocument.setText(TextUtils.isEmpty(documentName)
                ? getString(R.string.quiz_unknown_document)
                : documentName);
        tvSummary.setText(getString(
                R.string.quiz_review_summary_format,
                score,
                totalQuestions,
                correctCount,
                wrongCount));
    }

    private void parseAnswerData() {
        answersByQuestionId.clear();
        answersByOrderIndex.clear();
        if (TextUtils.isEmpty(answerData)) return;

        try {
            JsonElement root = JsonParser.parseString(answerData);
            if (!root.isJsonArray()) return;

            JsonArray rows = root.getAsJsonArray();
            for (JsonElement row : rows) {
                if (!row.isJsonObject()) continue;
                JsonObject object = row.getAsJsonObject();
                String questionId = readString(object, "question_id");
                String selected = readString(object, "selected_answer");
                int orderIndex = readInt(object, "order_index");
                if (!TextUtils.isEmpty(questionId)) {
                    answersByQuestionId.put(questionId, selected);
                }
                answersByOrderIndex.put(orderIndex, selected);
            }
        } catch (Exception ignored) {
            // Old or malformed answer_data should not block review of correct answers.
        }
    }

    private void loadQuestions() {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        if (TextUtils.isEmpty(userId)) {
            renderQuestions(new ArrayList<>());
            return;
        }

        setLoading(true);
        if (!TextUtils.isEmpty(quizSetId)) {
            AIContentRepository.getInstance().getQuizQuestionsBySet(
                    userId, quizSetId, questionCallback(userId));
        } else {
            loadQuestionsByDocument(userId);
        }
    }

    private ApiCallback<List<QuizQuestion>> questionCallback(String userId) {
        return new ApiCallback<List<QuizQuestion>>() {
            @Override
            public void onSuccess(List<QuizQuestion> questions) {
                if ((questions == null || questions.isEmpty()) && !triedDocumentFallback) {
                    loadQuestionsByDocument(userId);
                    return;
                }
                runOnUiThread(() -> renderQuestions(questions));
            }

            @Override
            public void onError(String errorMessage) {
                if (!triedDocumentFallback) {
                    loadQuestionsByDocument(userId);
                    return;
                }
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(
                            QuizReviewActivity.this,
                            errorMessage,
                            Toast.LENGTH_SHORT).show();
                    renderQuestions(new ArrayList<>());
                });
            }
        };
    }

    private void loadQuestionsByDocument(String userId) {
        triedDocumentFallback = true;
        if (TextUtils.isEmpty(documentId)) {
            runOnUiThread(() -> renderQuestions(new ArrayList<>()));
            return;
        }
        AIContentRepository.getInstance().getQuizQuestionsByDocument(
                userId, documentId, questionCallback(userId));
    }

    private void renderQuestions(List<QuizQuestion> questions) {
        setLoading(false);
        List<QuizReviewAdapter.ReviewItem> items = new ArrayList<>();
        if (questions != null) {
            for (int i = 0; i < questions.size(); i++) {
                QuizQuestion question = questions.get(i);
                String selected = answersByQuestionId.get(question.getId());
                boolean recorded = selected != null;
                if (!recorded && answersByOrderIndex.containsKey(i)) {
                    selected = answersByOrderIndex.get(i);
                    recorded = true;
                }
                items.add(new QuizReviewAdapter.ReviewItem(
                        question, selected, recorded));
            }
        }
        adapter.updateItems(items);
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        rvQuizReview.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private String readString(JsonObject object, String key) {
        return !object.has(key) || object.get(key).isJsonNull()
                ? ""
                : object.get(key).getAsString();
    }

    private int readInt(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) return -1;
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return -1;
        }
    }
}
