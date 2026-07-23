package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.adapters.QuizResultHistoryAdapter;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.models.Project;
import com.example.aistudyassistant.models.QuizResult;
import com.example.aistudyassistant.repositories.DocumentRepository;
import com.example.aistudyassistant.repositories.ProjectRepository;
import com.example.aistudyassistant.repositories.QuizRepository;
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

public class QuizHistoryActivity extends AppCompatActivity {

    private RecyclerView rvQuizHistory;
    private View layoutEmpty;
    private ProgressBar progressBar;
    private QuizResultHistoryAdapter adapter;

    private final List<QuizResult> quizResults = new ArrayList<>();
    private final Map<String, Document> documentsById = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_history);

        SupabaseClient.getInstance().setAccessToken(
                SharedPrefManager.getInstance(this).getAccessToken()
        );
        initViews();
        setupRecyclerView();
        loadQuizHistory();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        rvQuizHistory = findViewById(R.id.rv_quiz_history);
        layoutEmpty = findViewById(R.id.layout_empty);
        progressBar = findViewById(R.id.progress_bar);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new QuizResultHistoryAdapter(this, quizResults);
        adapter.setListener(this::openQuizForReview);
        rvQuizHistory.setLayoutManager(new LinearLayoutManager(this));
        rvQuizHistory.setAdapter(adapter);
    }

    private void loadQuizHistory() {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        if (isBlank(userId)) {
            renderResults();
            return;
        }

        setLoading(true);
        QuizRepository.getInstance().getQuizResults(userId, new ApiCallback<List<QuizResult>>() {
            @Override
            public void onSuccess(List<QuizResult> result) {
                quizResults.clear();
                quizResults.addAll(result);
                if (quizResults.isEmpty()) {
                    runOnUiThread(QuizHistoryActivity.this::renderResults);
                    return;
                }
                loadDocuments(userId);
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(
                            QuizHistoryActivity.this,
                            "Could not load quiz history",
                            Toast.LENGTH_SHORT
                    ).show();
                    renderResults();
                });
            }
        });
    }

    private void loadDocuments(String userId) {
        DocumentRepository.getInstance().getAllDocuments(userId, new ApiCallback<List<Document>>() {
            @Override
            public void onSuccess(List<Document> documents) {
                documentsById.clear();
                for (Document document : documents) {
                    documentsById.put(document.getId(), document);
                }
                enrichDocumentNames();
                loadProjects(userId);
            }

            @Override
            public void onError(String errorMessage) {
                loadProjects(userId);
            }
        });
    }

    private void loadProjects(String userId) {
        ProjectRepository.getInstance().getAllProjects(userId, new ApiCallback<List<Project>>() {
            @Override
            public void onSuccess(List<Project> projects) {
                Map<String, Project> projectsById = new HashMap<>();
                for (Project project : projects) {
                    projectsById.put(project.getId(), project);
                }
                enrichProjectNames(projectsById);
                loadQuizMetadata(userId);
            }

            @Override
            public void onError(String errorMessage) {
                loadQuizMetadata(userId);
            }
        });
    }

    private void loadQuizMetadata(String userId) {
        new Thread(() -> {
            try {
                Map<String, String> difficultyBySetId = new HashMap<>();
                String setResponse = SupabaseClient.getInstance().getFromTable(
                        Constants.TABLE_QUIZ_SETS,
                        "user_id=eq." + userId + "&select=id,difficulty");
                for (JsonElement row : parseRows(setResponse)) {
                    JsonObject json = row.getAsJsonObject();
                    difficultyBySetId.put(
                            readString(json, "id"),
                            readString(json, "difficulty"));
                }

                Map<String, String> setIdByQuestionId = new HashMap<>();
                Map<String, String> difficultyByQuestionId = new HashMap<>();
                String questionResponse = SupabaseClient.getInstance().getFromTable(
                        Constants.TABLE_QUIZZES,
                        "user_id=eq." + userId
                                + "&select=id,quiz_set_id,difficulty"
                                + "&limit=1000");
                for (JsonElement row : parseRows(questionResponse)) {
                    JsonObject json = row.getAsJsonObject();
                    String questionId = readString(json, "id");
                    setIdByQuestionId.put(questionId, readString(json, "quiz_set_id"));
                    difficultyByQuestionId.put(questionId, readString(json, "difficulty"));
                }

                enrichQuizDifficulty(
                        difficultyBySetId,
                        setIdByQuestionId,
                        difficultyByQuestionId);
            } catch (Exception ignored) {
                // Difficulty is helpful metadata; quiz history should still render without it.
            }
            runOnUiThread(QuizHistoryActivity.this::renderResults);
        }).start();
    }

    private void enrichDocumentNames() {
        for (QuizResult result : quizResults) {
            Document document = documentsById.get(result.getDocumentId());
            if (document == null) continue;

            result.setDocumentName(document.getName());
            if (isBlank(result.getProjectId())) {
                result.setProjectId(document.getProjectId());
            }
        }
    }

    private void enrichProjectNames(Map<String, Project> projectsById) {
        for (QuizResult result : quizResults) {
            Project project = projectsById.get(result.getProjectId());
            if (project != null) {
                result.setProjectName(project.getName());
            }
        }
    }

    private void enrichQuizDifficulty(
            Map<String, String> difficultyBySetId,
            Map<String, String> setIdByQuestionId,
            Map<String, String> difficultyByQuestionId) {
        for (QuizResult result : quizResults) {
            String quizSetId = result.getQuizSetId();
            if (isBlank(quizSetId) && !isBlank(result.getQuizId())) {
                quizSetId = setIdByQuestionId.get(result.getQuizId());
                result.setQuizSetId(quizSetId);
            }

            String difficulty = isBlank(quizSetId)
                    ? null
                    : difficultyBySetId.get(quizSetId);
            if (isBlank(difficulty) && !isBlank(result.getQuizId())) {
                difficulty = difficultyByQuestionId.get(result.getQuizId());
            }
            result.setDifficulty(difficulty);
        }
    }

    private void renderResults() {
        setLoading(false);
        adapter.updateResults(quizResults);
        layoutEmpty.setVisibility(quizResults.isEmpty() ? View.VISIBLE : View.GONE);
        rvQuizHistory.setVisibility(quizResults.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void openQuizForReview(QuizResult result) {
        Document document = documentsById.get(result.getDocumentId());
        Intent intent = new Intent(this, QuizReviewActivity.class);
        intent.putExtra(Constants.EXTRA_DOCUMENT_ID, result.getDocumentId());
        intent.putExtra(Constants.EXTRA_DOCUMENT_NAME,
                document == null || isBlank(document.getName())
                        ? result.getDocumentName()
                        : document.getName());
        intent.putExtra(Constants.EXTRA_PROJECT_ID, result.getProjectId());
        intent.putExtra(Constants.EXTRA_QUIZ_SET_ID, result.getQuizSetId());
        intent.putExtra(Constants.EXTRA_QUIZ_ANSWER_DATA, result.getAnswerData());
        intent.putExtra("score", result.getScore());
        intent.putExtra("total_questions", result.getTotalQuestions());
        intent.putExtra("correct_count", result.getCorrectCount());
        intent.putExtra("wrong_count", result.getWrongCount());
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private JsonArray parseRows(String response) {
        if (isBlank(response)) return new JsonArray();
        JsonElement root = JsonParser.parseString(response);
        return root.isJsonArray() ? root.getAsJsonArray() : new JsonArray();
    }

    private String readString(JsonObject json, String key) {
        return !json.has(key) || json.get(key).isJsonNull()
                ? ""
                : json.get(key).getAsString();
    }
}
