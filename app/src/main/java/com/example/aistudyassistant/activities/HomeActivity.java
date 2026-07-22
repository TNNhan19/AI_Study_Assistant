package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.adapters.DocumentAdapter;
import com.example.aistudyassistant.adapters.ScheduleAdapter;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.models.Schedule;
import com.example.aistudyassistant.repositories.ProjectRepository;
import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.aistudyassistant.api.SupabaseClient;

import com.example.aistudyassistant.models.Project;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private TextView tvGreeting, tvUserName, tvSearchHint;
    private TextView tvSeeAllDocs, tvSeeAllSchedule;
    private TextView tvNoDocs, tvNoSchedule;
    private RecyclerView rvRecentDocs, rvUpcomingSchedule;

    // Quick Action buttons
    private View qaUpload, qaAskAi, qaQuiz, qaFlashcards, qaSchedule, qaProjects, qaNotes;

    private BottomNavigationView bottomNavigation;

    private DocumentAdapter documentAdapter;
    private ScheduleAdapter scheduleAdapter;

    private final List<Document> recentDocs = new ArrayList<>();
    private final List<Schedule> upcomingSchedules = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initViews();
        setupGreeting();
        setupQuickActions();
        setupRecyclerViews();
        setupBottomNavigation();
        loadData();

        String userId = SharedPrefManager.getInstance(this).getUserId();

        // Đảm bảo request test DB dùng access token của user đang login.
        String accessToken = SharedPrefManager.getInstance(this).getAccessToken();
        SupabaseClient.getInstance().setAccessToken(accessToken);
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvUserName = findViewById(R.id.tv_user_name);
        tvSearchHint = findViewById(R.id.tv_search_hint);
        tvSeeAllDocs = findViewById(R.id.tv_see_all_docs);
        tvSeeAllSchedule = findViewById(R.id.tv_see_all_schedule);
        tvNoDocs = findViewById(R.id.tv_no_docs);
        tvNoSchedule = findViewById(R.id.tv_no_schedule);
        rvRecentDocs = findViewById(R.id.rv_recent_docs);
        rvUpcomingSchedule = findViewById(R.id.rv_upcoming_schedule);
        qaUpload = findViewById(R.id.qa_upload);
        qaAskAi = findViewById(R.id.qa_ask_ai);
        qaQuiz = findViewById(R.id.qa_quiz);
        qaFlashcards = findViewById(R.id.qa_flashcards);
        qaSchedule = findViewById(R.id.qa_schedule);
        qaProjects = findViewById(R.id.qa_projects);
        qaNotes = findViewById(R.id.qa_notes);
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
                startActivity(new Intent(this, ChatActivity.class)));

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

        // Search hint click
        tvSearchHint.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));

        // See all links
        tvSeeAllDocs.setOnClickListener(v ->
                startActivity(new Intent(this, DocumentsActivity.class)));
        tvSeeAllSchedule.setOnClickListener(v ->
                startActivity(new Intent(this, ScheduleActivity.class)));
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

        scheduleAdapter = new ScheduleAdapter(this, upcomingSchedules);
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
                startActivity(new Intent(this, ChatActivity.class));
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

        // 1. Load Recent Documents from SharedPreferences
        List<String> recentIds = SharedPrefManager.getInstance(this).getRecentDocumentIds();
        if (recentIds.isEmpty()) {
            updateEmptyState();
        } else {
            com.example.aistudyassistant.repositories.DocumentRepository.getInstance().getAllDocuments(userId, new ApiCallback<List<Document>>() {
                @Override
                public void onSuccess(List<Document> result) {
                    List<Document> sortedRecents = new ArrayList<>();
                    for (String id : recentIds) {
                        for (Document doc : result) {
                            if (doc.getId().equals(id)) {
                                sortedRecents.add(doc);
                                break;
                            }
                        }
                    }
                    runOnUiThread(() -> {
                        recentDocs.clear();
                        recentDocs.addAll(sortedRecents);
                        documentAdapter.updateDocuments(recentDocs);
                        updateEmptyState();
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> updateEmptyState());
                }
            });
        }
        
        // 2. TODO: Load Upcoming Schedules from Supabase
    }

    private void updateEmptyState() {
        tvNoDocs.setVisibility(recentDocs.isEmpty() ? View.VISIBLE : View.GONE);
        tvNoSchedule.setVisibility(upcomingSchedules.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        loadData();
    }
}
