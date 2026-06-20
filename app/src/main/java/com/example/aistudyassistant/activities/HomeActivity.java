package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.adapters.DocumentAdapter;
import com.example.aistudyassistant.adapters.ScheduleAdapter;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.models.Schedule;
import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private TextView tvGreeting, tvUserName, tvSearchHint;
    private TextView tvSeeAllDocs, tvSeeAllSchedule;
    private TextView tvNoDocs, tvNoSchedule;
    private RecyclerView rvRecentDocs, rvUpcomingSchedule;

    // Quick Action buttons
    private View qaUpload, qaAskAi, qaQuiz, qaFlashcards, qaSchedule;

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
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupGreeting() {
        String name = SharedPrefManager.getInstance(this).getUserName();
        tvUserName.setText(name + "!");

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) tvGreeting.setText("Good Morning,");
        else if (hour < 18) tvGreeting.setText("Good Afternoon,");
        else tvGreeting.setText("Good Evening,");
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
                intent.putExtra(Constants.EXTRA_DOCUMENT_URL, document.getFileUrl());
                startActivity(intent);
            }
            @Override
            public void onDocumentMoreClick(Document document, View anchorView) { }
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
        // TODO: Load real data from Supabase
        // For now, show empty state
        updateEmptyState();
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
