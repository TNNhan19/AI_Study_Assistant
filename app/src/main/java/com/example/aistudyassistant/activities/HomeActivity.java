package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.adapters.DocumentAdapter;
import com.example.aistudyassistant.adapters.ScheduleAdapter;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.models.Flashcard;
import com.example.aistudyassistant.models.FlashcardSet;
import com.example.aistudyassistant.models.Project;
import com.example.aistudyassistant.models.QuizResult;
import com.example.aistudyassistant.models.Schedule;
import com.example.aistudyassistant.repositories.AIContentRepository;
import com.example.aistudyassistant.repositories.DocumentRepository;
import com.example.aistudyassistant.repositories.ProjectRepository;
import com.example.aistudyassistant.repositories.QuizRepository;
import com.example.aistudyassistant.repositories.ScheduleRepository;
import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.aistudyassistant.api.SupabaseClient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {

    private TextView tvGreeting, tvUserName, tvSearchHint;
    private TextView tvSeeAllDocs, tvSeeAllSchedule, tvSeeAllQuizResults, tvSeeAllFlashcardHistory;
    private TextView tvNoDocs, tvNoSchedule;
    private TextView tvLatestQuizDocument, tvLatestQuizFolder;
    private TextView tvLatestQuizScore, tvLatestQuizCorrect, tvLatestQuizWrong, tvNoQuizResult;
    private TextView tvLatestFlashcardDocument, tvLatestFlashcardFolder, tvLatestFlashcardCount;
    private TextView tvLatestFlashcardCreatedAt, tvNoFlashcardHistory;
    private CardView cardLatestQuizResult, cardLatestFlashcardSet;
    private RecyclerView rvRecentDocs, rvUpcomingSchedule;

    // Quick Action buttons
    private View qaUpload, qaAskAi, qaQuiz, qaFlashcards, qaSchedule, qaProjects, qaNotes, qaStudyPlan;

    private BottomNavigationView bottomNavigation;

    private DocumentAdapter documentAdapter;
    private ScheduleAdapter scheduleAdapter;
    private static final int MAX_HOME_DOCUMENTS = 5;

    private final List<Document> recentDocs = new ArrayList<>();
    private final List<Schedule> upcomingSchedules = new ArrayList<>();
    private QuizResult latestQuizResult;
    private FlashcardSet latestFlashcardSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initViews();
        setupGreeting();
        setupQuickActions();
        setupRecyclerViews();
        setupBottomNavigation();
        String homeAccessToken = SharedPrefManager.getInstance(this).getAccessToken();
        SupabaseClient.getInstance().setAccessToken(homeAccessToken);
        loadData();
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvUserName = findViewById(R.id.tv_user_name);
        tvSearchHint = findViewById(R.id.tv_search_hint);
        tvSeeAllDocs = findViewById(R.id.tv_see_all_docs);
        tvSeeAllSchedule = findViewById(R.id.tv_see_all_schedule);
        tvSeeAllQuizResults = findViewById(R.id.tv_see_all_quiz_results);
        tvSeeAllFlashcardHistory = findViewById(R.id.tv_see_all_flashcard_history);
        tvNoDocs = findViewById(R.id.tv_no_docs);
        tvNoSchedule = findViewById(R.id.tv_no_schedule);
        tvLatestQuizDocument = findViewById(R.id.tv_latest_quiz_document);
        tvLatestQuizFolder = findViewById(R.id.tv_latest_quiz_folder);
        tvLatestQuizScore = findViewById(R.id.tv_latest_quiz_score);
        tvLatestQuizCorrect = findViewById(R.id.tv_latest_quiz_correct);
        tvLatestQuizWrong = findViewById(R.id.tv_latest_quiz_wrong);
        tvNoQuizResult = findViewById(R.id.tv_no_quiz_result);
        cardLatestQuizResult = findViewById(R.id.card_latest_quiz_result);
        tvLatestFlashcardDocument = findViewById(R.id.tv_latest_flashcard_document);
        tvLatestFlashcardFolder = findViewById(R.id.tv_latest_flashcard_folder);
        tvLatestFlashcardCount = findViewById(R.id.tv_latest_flashcard_count);
        tvLatestFlashcardCreatedAt = findViewById(R.id.tv_latest_flashcard_created_at);
        tvNoFlashcardHistory = findViewById(R.id.tv_no_flashcard_history);
        cardLatestFlashcardSet = findViewById(R.id.card_latest_flashcard_set);
        rvRecentDocs = findViewById(R.id.rv_recent_docs);
        rvUpcomingSchedule = findViewById(R.id.rv_upcoming_schedule);
        qaUpload = findViewById(R.id.qa_upload);
        qaAskAi = findViewById(R.id.qa_ask_ai);
        qaQuiz = findViewById(R.id.qa_quiz);
        qaFlashcards = findViewById(R.id.qa_flashcards);
        qaSchedule = findViewById(R.id.qa_schedule);
        qaProjects = findViewById(R.id.qa_projects);
        qaNotes = findViewById(R.id.qa_notes);
        qaStudyPlan = findViewById(R.id.qa_study_plan);
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupGreeting() {
        String name = SharedPrefManager.getInstance(this).getUserName();
        tvUserName.setText(getString(R.string.user_name_format, name));

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) tvGreeting.setText(R.string.good_morning);

        else if (hour < 18) tvGreeting.setText(R.string.good_afternoon);
        else tvGreeting.setText(R.string.good_evening);
    }

    private void setupQuickActions() {
        qaUpload.setOnClickListener(v ->
                startActivity(new Intent(this, UploadDocumentActivity.class)));

        qaAskAi.setOnClickListener(v ->
                startActivity(new Intent(this, AIChatActivity.class)));

        qaQuiz.setOnClickListener(v ->
                startActivity(new Intent(this, DocumentsActivity.class)));

        qaFlashcards.setOnClickListener(v ->
                startActivity(new Intent(this, DocumentsActivity.class)));

        qaSchedule.setOnClickListener(v ->
                startActivity(new Intent(this, ScheduleActivity.class)));

        qaProjects.setOnClickListener(v ->
                startActivity(new Intent(this, ProjectsActivity.class)));

        qaNotes.setOnClickListener(v ->
                startActivity(new Intent(this, NotesActivity.class)));

        qaStudyPlan.setOnClickListener(v ->
                startActivity(new Intent(this, StudyPlanActivity.class)));

        // Search hint click
        tvSearchHint.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));

        // See all links
        tvSeeAllDocs.setOnClickListener(v ->
                startActivity(new Intent(this, DocumentsActivity.class)));
        tvSeeAllSchedule.setOnClickListener(v ->
                startActivity(new Intent(this, ScheduleActivity.class)));
        tvSeeAllQuizResults.setOnClickListener(v ->
                startActivity(new Intent(this, QuizHistoryActivity.class)));
        tvSeeAllFlashcardHistory.setOnClickListener(v ->
                startActivity(new Intent(this, FlashcardHistoryActivity.class)));
    }

    private void setupRecyclerViews() {
        documentAdapter = new DocumentAdapter(this, recentDocs);
        documentAdapter.setListener(new DocumentAdapter.OnDocumentClickListener() {
            @Override
            public void onDocumentClick(Document document) {
                Intent intent = new Intent(HomeActivity.this, DocumentDetailActivity.class);
                intent.putExtra(Constants.EXTRA_DOCUMENT_ID, document.getId());
                intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, document.getName());
                intent.putExtra(Constants.EXTRA_DOCUMENT_PATH, document.getFilePath());
                intent.putExtra(Constants.EXTRA_DOCUMENT_TYPE, document.getFileType());
                startActivity(intent);
            }
            @Override
            public void onDocumentMoreClick(Document document, View anchorView) { }

            @Override
            public void onFavoriteClick(Document document) {
                // Handle favorite toggle from home if needed
            }
        });
        rvRecentDocs.setLayoutManager(new LinearLayoutManager(this));
        rvRecentDocs.setAdapter(documentAdapter);
        rvRecentDocs.setNestedScrollingEnabled(false);

        scheduleAdapter = new ScheduleAdapter(this, upcomingSchedules, false);
        rvUpcomingSchedule.setLayoutManager(new LinearLayoutManager(this));
        rvUpcomingSchedule.setAdapter(scheduleAdapter);
        rvUpcomingSchedule.setNestedScrollingEnabled(false);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            else if (id == R.id.nav_documents) {
                startActivity(new Intent(this, DocumentsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_schedule) {
                startActivity(new Intent(this, ScheduleActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, AIChatActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void loadData() {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        if (userId.isEmpty()) return;

        // 1. Load recent documents. Prefer recently opened IDs, then fill with newest documents.
        List<String> recentIds = SharedPrefManager.getInstance(this).getRecentDocumentIds();
        DocumentRepository.getInstance().getAllDocuments(userId, new ApiCallback<List<Document>>() {
            @Override
            public void onSuccess(List<Document> result) {
                List<Document> homeDocuments = buildHomeDocuments(result, recentIds);
                runOnUiThread(() -> {
                    recentDocs.clear();
                    recentDocs.addAll(homeDocuments);
                    documentAdapter.updateDocuments(recentDocs);
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> updateEmptyState());
            }
        });

        loadTodaySchedules(userId);
        loadLatestQuizResult(userId);
        loadLatestFlashcardSet(userId);
    }

    private void loadTodaySchedules(String userId) {
        ScheduleRepository.getInstance().getTodaySchedules(userId, new ApiCallback<List<Schedule>>() {
            @Override
            public void onSuccess(List<Schedule> result) {
                runOnUiThread(() -> {
                    upcomingSchedules.clear();
                    int count = Math.min(result.size(), 3);
                    upcomingSchedules.addAll(result.subList(0, count));
                    scheduleAdapter.updateSchedules(upcomingSchedules);
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    upcomingSchedules.clear();
                    scheduleAdapter.updateSchedules(upcomingSchedules);
                    updateEmptyState();
                    Toast.makeText(
                            HomeActivity.this,
                            "Could not load today's schedule",
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }
        });
    }

    private void loadLatestQuizResult(String userId) {
        QuizRepository.getInstance().getLatestQuizResult(userId, new ApiCallback<QuizResult>() {
            @Override
            public void onSuccess(QuizResult result) {
                if (result == null) {
                    runOnUiThread(() -> {
                        latestQuizResult = null;
                        updateLatestQuizResult();
                    });
                    return;
                }
                resolveLatestQuizContext(userId, result);
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    latestQuizResult = null;
                    updateLatestQuizResult();
                });
            }
        });
    }

    private void resolveLatestQuizContext(String userId, QuizResult result) {
        DocumentRepository.getInstance().getAllDocuments(userId, new ApiCallback<List<Document>>() {
            @Override
            public void onSuccess(List<Document> documents) {
                Document document = findDocumentById(documents, result.getDocumentId());
                if (document != null) {
                    result.setDocumentName(document.getName());
                    if (isBlank(result.getProjectId())) {
                        result.setProjectId(document.getProjectId());
                    }
                }
                resolveLatestQuizProject(userId, result);
            }

            @Override
            public void onError(String errorMessage) {
                resolveLatestQuizProject(userId, result);
            }
        });
    }

    private void resolveLatestQuizProject(String userId, QuizResult result) {
        if (isBlank(result.getProjectId())) {
            runOnUiThread(() -> {
                latestQuizResult = result;
                updateLatestQuizResult();
            });
            return;
        }

        ProjectRepository.getInstance().getAllProjects(userId, new ApiCallback<List<Project>>() {
            @Override
            public void onSuccess(List<Project> projects) {
                Project project = findProjectById(projects, result.getProjectId());
                if (project != null) {
                    result.setProjectName(project.getName());
                }
                runOnUiThread(() -> {
                    latestQuizResult = result;
                    updateLatestQuizResult();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    latestQuizResult = result;
                    updateLatestQuizResult();
                });
            }
        });
    }

    private Document findDocumentById(List<Document> documents, String documentId) {
        if (documents == null || isBlank(documentId)) return null;
        for (Document document : documents) {
            if (documentId.equals(document.getId())) return document;
        }
        return null;
    }

    private Project findProjectById(List<Project> projects, String projectId) {
        if (projects == null || isBlank(projectId)) return null;
        for (Project project : projects) {
            if (projectId.equals(project.getId())) return project;
        }
        return null;
    }

    private void loadLatestFlashcardSet(String userId) {
        AIContentRepository.getInstance().getFlashcardsByUser(userId, new ApiCallback<List<Flashcard>>() {
            @Override
            public void onSuccess(List<Flashcard> result) {
                List<FlashcardSet> sets = buildFlashcardSets(result);
                if (sets.isEmpty()) {
                    runOnUiThread(() -> {
                        latestFlashcardSet = null;
                        updateLatestFlashcardSet();
                    });
                    return;
                }
                FlashcardSet latestSet = sets.get(0);
                resolveLatestFlashcardContext(userId, latestSet);
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    latestFlashcardSet = null;
                    updateLatestFlashcardSet();
                });
            }
        });
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

    private void resolveLatestFlashcardContext(String userId, FlashcardSet set) {
        DocumentRepository.getInstance().getAllDocuments(userId, new ApiCallback<List<Document>>() {
            @Override
            public void onSuccess(List<Document> documents) {
                Document document = findDocumentById(documents, set.getDocumentId());
                if (document != null) {
                    set.setDocumentName(document.getName());
                    set.setTopicId(document.getTopicId());
                    set.setProjectId(document.getProjectId());
                }
                resolveLatestFlashcardProject(userId, set);
            }

            @Override
            public void onError(String errorMessage) {
                resolveLatestFlashcardProject(userId, set);
            }
        });
    }

    private void resolveLatestFlashcardProject(String userId, FlashcardSet set) {
        if (isBlank(set.getProjectId())) {
            runOnUiThread(() -> {
                latestFlashcardSet = set;
                updateLatestFlashcardSet();
            });
            return;
        }

        ProjectRepository.getInstance().getAllProjects(userId, new ApiCallback<List<Project>>() {
            @Override
            public void onSuccess(List<Project> projects) {
                Project project = findProjectById(projects, set.getProjectId());
                if (project != null) {
                    set.setProjectName(project.getName());
                }
                runOnUiThread(() -> {
                    latestFlashcardSet = set;
                    updateLatestFlashcardSet();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    latestFlashcardSet = set;
                    updateLatestFlashcardSet();
                });
            }
        });
    }

    private List<Document> buildHomeDocuments(List<Document> allDocuments, List<String> recentIds) {
        List<Document> homeDocuments = new ArrayList<>();
        Set<String> addedIds = new HashSet<>();

        for (String id : recentIds) {
            for (Document doc : allDocuments) {
                if (id != null && id.equals(doc.getId()) && addedIds.add(doc.getId())) {
                    homeDocuments.add(doc);
                    break;
                }
            }
            if (homeDocuments.size() >= MAX_HOME_DOCUMENTS) return homeDocuments;
        }

        for (Document doc : allDocuments) {
            if (doc.getId() != null && addedIds.add(doc.getId())) {
                homeDocuments.add(doc);
            }
            if (homeDocuments.size() >= MAX_HOME_DOCUMENTS) break;
        }
        return homeDocuments;
    }

    private void updateEmptyState() {
        tvNoDocs.setVisibility(recentDocs.isEmpty() ? View.VISIBLE : View.GONE);
        tvNoSchedule.setVisibility(upcomingSchedules.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateLatestQuizResult() {
        if (latestQuizResult == null) {
            cardLatestQuizResult.setVisibility(View.GONE);
            tvNoQuizResult.setVisibility(View.VISIBLE);
            return;
        }

        cardLatestQuizResult.setVisibility(View.VISIBLE);
        tvNoQuizResult.setVisibility(View.GONE);
        String documentName = isBlank(latestQuizResult.getDocumentName())
                ? getString(R.string.quiz_unknown_document)
                : latestQuizResult.getDocumentName();
        tvLatestQuizDocument.setText(getString(R.string.quiz_document_format, documentName));

        if (isBlank(latestQuizResult.getProjectName())) {
            tvLatestQuizFolder.setVisibility(View.GONE);
        } else {
            tvLatestQuizFolder.setVisibility(View.VISIBLE);
            tvLatestQuizFolder.setText(getString(
                    R.string.quiz_folder_format,
                    latestQuizResult.getProjectName()
            ));
        }
        tvLatestQuizScore.setText(getString(
                R.string.quiz_score_format,
                latestQuizResult.getScore(),
                latestQuizResult.getTotalQuestions()
        ));
        tvLatestQuizCorrect.setText(getString(
                R.string.quiz_correct_format,
                latestQuizResult.getCorrectCount()
        ));
        tvLatestQuizWrong.setText(getString(
                R.string.quiz_wrong_format,
                latestQuizResult.getWrongCount()
        ));
    }

    private void updateLatestFlashcardSet() {
        if (latestFlashcardSet == null) {
            cardLatestFlashcardSet.setVisibility(View.GONE);
            tvNoFlashcardHistory.setVisibility(View.VISIBLE);
            return;
        }

        cardLatestFlashcardSet.setVisibility(View.VISIBLE);
        tvNoFlashcardHistory.setVisibility(View.GONE);
        String documentName = isBlank(latestFlashcardSet.getDocumentName())
                ? getString(R.string.quiz_unknown_document)
                : latestFlashcardSet.getDocumentName();
        tvLatestFlashcardDocument.setText(getString(R.string.quiz_document_format, documentName));

        if (isBlank(latestFlashcardSet.getProjectName())) {
            tvLatestFlashcardFolder.setVisibility(View.GONE);
        } else {
            tvLatestFlashcardFolder.setVisibility(View.VISIBLE);
            tvLatestFlashcardFolder.setText(getString(
                    R.string.quiz_folder_format,
                    latestFlashcardSet.getProjectName()
            ));
        }

        tvLatestFlashcardCount.setText(getString(
                R.string.flashcard_count_format,
                latestFlashcardSet.getCardCount()
        ));
        tvLatestFlashcardCreatedAt.setText(formatTimestamp(latestFlashcardSet.getCreatedAt()));
    }

    private String formatTimestamp(long value) {
        if (value <= 0) return getString(R.string.quiz_unknown_time);
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(
                "MMM dd, yyyy HH:mm",
                java.util.Locale.getDefault()
        );
        return format.format(new java.util.Date(value));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        loadData();
    }
}
