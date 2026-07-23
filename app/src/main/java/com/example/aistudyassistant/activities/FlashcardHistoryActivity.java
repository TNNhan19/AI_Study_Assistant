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
import com.example.aistudyassistant.adapters.FlashcardSetHistoryAdapter;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.models.Flashcard;
import com.example.aistudyassistant.models.FlashcardSet;
import com.example.aistudyassistant.models.Project;
import com.example.aistudyassistant.repositories.AIContentRepository;
import com.example.aistudyassistant.repositories.DocumentRepository;
import com.example.aistudyassistant.repositories.ProjectRepository;
import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlashcardHistoryActivity extends AppCompatActivity {

    private RecyclerView rvFlashcardHistory;
    private View layoutEmpty;
    private ProgressBar progressBar;
    private FlashcardSetHistoryAdapter adapter;

    private final List<FlashcardSet> flashcardSets = new ArrayList<>();
    private final Map<String, Document> documentsById = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_history);

        SupabaseClient.getInstance().setAccessToken(
                SharedPrefManager.getInstance(this).getAccessToken()
        );
        initViews();
        setupRecyclerView();
        loadFlashcardHistory();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        rvFlashcardHistory = findViewById(R.id.rv_flashcard_history);
        layoutEmpty = findViewById(R.id.layout_empty);
        progressBar = findViewById(R.id.progress_bar);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new FlashcardSetHistoryAdapter(this, flashcardSets);
        adapter.setListener(this::openFlashcardsForReview);
        rvFlashcardHistory.setLayoutManager(new LinearLayoutManager(this));
        rvFlashcardHistory.setAdapter(adapter);
    }

    private void loadFlashcardHistory() {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        if (isBlank(userId)) {
            renderSets();
            return;
        }

        setLoading(true);
        AIContentRepository.getInstance().getFlashcardsByUser(
                userId,
                new ApiCallback<List<Flashcard>>() {
                    @Override
                    public void onSuccess(List<Flashcard> result) {
                        flashcardSets.clear();
                        flashcardSets.addAll(buildFlashcardSets(result));
                        if (flashcardSets.isEmpty()) {
                            runOnUiThread(FlashcardHistoryActivity.this::renderSets);
                            return;
                        }
                        loadDocuments(userId);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(
                                    FlashcardHistoryActivity.this,
                                    "Could not load flashcard history",
                                    Toast.LENGTH_SHORT
                            ).show();
                            renderSets();
                        });
                    }
                }
        );
    }

    private List<FlashcardSet> buildFlashcardSets(List<Flashcard> flashcards) {
        Map<String, FlashcardSet> setsByDocument = new HashMap<>();
        for (Flashcard flashcard : flashcards) {
            if (flashcard == null || isBlank(flashcard.getDocumentId())) continue;

            FlashcardSet set = setsByDocument.get(flashcard.getDocumentId());
            if (set == null) {
                set = new FlashcardSet();
                set.setDocumentId(flashcard.getDocumentId());
                set.setTopicId(flashcard.getTopicId());
                setsByDocument.put(flashcard.getDocumentId(), set);
            }
            set.setCardCount(set.getCardCount() + 1);
            if (flashcard.getCreatedAt() > set.getCreatedAt()) {
                set.setCreatedAt(flashcard.getCreatedAt());
            }
        }

        List<FlashcardSet> sets = new ArrayList<>(setsByDocument.values());
        Collections.sort(sets, (left, right) -> Long.compare(right.getCreatedAt(), left.getCreatedAt()));
        return sets;
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
                runOnUiThread(FlashcardHistoryActivity.this::renderSets);
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(FlashcardHistoryActivity.this::renderSets);
            }
        });
    }

    private void enrichDocumentNames() {
        for (FlashcardSet set : flashcardSets) {
            Document document = documentsById.get(set.getDocumentId());
            if (document == null) continue;

            set.setDocumentName(document.getName());
            set.setTopicId(document.getTopicId());
            set.setProjectId(document.getProjectId());
        }
    }

    private void enrichProjectNames(Map<String, Project> projectsById) {
        for (FlashcardSet set : flashcardSets) {
            Project project = projectsById.get(set.getProjectId());
            if (project != null) {
                set.setProjectName(project.getName());
            }
        }
    }

    private void renderSets() {
        setLoading(false);
        adapter.updateSets(flashcardSets);
        layoutEmpty.setVisibility(flashcardSets.isEmpty() ? View.VISIBLE : View.GONE);
        rvFlashcardHistory.setVisibility(flashcardSets.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void openFlashcardsForReview(FlashcardSet set) {
        Document document = documentsById.get(set.getDocumentId());
        if (document != null) {
            startFlashcards(document, set);
            return;
        }

        if (isBlank(set.getDocumentId())) {
            Toast.makeText(this, "This flashcard set has no document.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        DocumentRepository.getInstance().getDocumentById(
                set.getDocumentId(),
                new ApiCallback<Document>() {
                    @Override
                    public void onSuccess(Document document) {
                        documentsById.put(document.getId(), document);
                        runOnUiThread(() -> {
                            setLoading(false);
                            startFlashcards(document, set);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(
                                    FlashcardHistoryActivity.this,
                                    "Document is no longer available.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }
                }
        );
    }

    private void startFlashcards(Document document, FlashcardSet set) {
        Intent intent = new Intent(this, FlashcardsActivity.class);
        intent.putExtra(Constants.EXTRA_DOCUMENT_ID, document.getId());
        intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, document.getName());
        intent.putExtra(Constants.EXTRA_DOCUMENT_URL, document.getFilePath());
        intent.putExtra(Constants.EXTRA_DOCUMENT_TYPE, document.getFileType());
        intent.putExtra(Constants.EXTRA_PROJECT_ID,
                isBlank(document.getProjectId()) ? set.getProjectId() : document.getProjectId());
        intent.putExtra(Constants.EXTRA_TOPIC_ID,
                isBlank(document.getTopicId()) ? set.getTopicId() : document.getTopicId());
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
