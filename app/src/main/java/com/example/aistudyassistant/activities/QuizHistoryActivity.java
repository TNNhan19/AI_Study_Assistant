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
                runOnUiThread(QuizHistoryActivity.this::renderResults);
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(QuizHistoryActivity.this::renderResults);
            }
        });
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

    private void renderResults() {
        setLoading(false);
        adapter.updateResults(quizResults);
        layoutEmpty.setVisibility(quizResults.isEmpty() ? View.VISIBLE : View.GONE);
        rvQuizHistory.setVisibility(quizResults.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void openQuizForReview(QuizResult result) {
        Document document = documentsById.get(result.getDocumentId());
        if (document != null) {
            startQuiz(document, result);
            return;
        }

        if (isBlank(result.getDocumentId())) {
            Toast.makeText(this, "This quiz result has no document.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        DocumentRepository.getInstance().getDocumentById(
                result.getDocumentId(),
                new ApiCallback<Document>() {
                    @Override
                    public void onSuccess(Document document) {
                        documentsById.put(document.getId(), document);
                        runOnUiThread(() -> {
                            setLoading(false);
                            startQuiz(document, result);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(
                                    QuizHistoryActivity.this,
                                    "Document is no longer available.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }
                }
        );
    }

    private void startQuiz(Document document, QuizResult result) {
        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtra(Constants.EXTRA_DOCUMENT_ID, document.getId());
        intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, document.getName());
        intent.putExtra(Constants.EXTRA_DOCUMENT_URL, document.getFilePath());
        intent.putExtra(Constants.EXTRA_DOCUMENT_TYPE, document.getFileType());
        intent.putExtra(Constants.EXTRA_PROJECT_ID,
                isBlank(document.getProjectId()) ? result.getProjectId() : document.getProjectId());
        intent.putExtra(Constants.EXTRA_TOPIC_ID, document.getTopicId());
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
