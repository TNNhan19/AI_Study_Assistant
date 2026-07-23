package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.StudyStats;
import com.example.aistudyassistant.repositories.StudyRepository;
import com.example.aistudyassistant.utils.SessionManager;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail;
    private TextView tvTotalDocs, tvTotalQuizzes, tvTotalFlashcards, tvUpcomingSessions;
    private MaterialButton btnEditProfile, btnLogout;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        initViews();
        setupClickListeners();
        setupBottomNavigation();
    }

    private void initViews() {
        tvName = findViewById(R.id.tv_name);
        tvEmail = findViewById(R.id.tv_email);
        tvTotalDocs = findViewById(R.id.tv_total_docs);
        tvTotalQuizzes = findViewById(R.id.tv_total_quizzes);
        tvTotalFlashcards = findViewById(R.id.tv_total_flashcards);
        tvUpcomingSessions = findViewById(R.id.tv_upcoming_sessions);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnLogout = findViewById(R.id.btn_logout);
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void loadProfile() {
        SharedPrefManager prefs = SharedPrefManager.getInstance(this);
        tvName.setText(prefs.getUserName());
        tvEmail.setText(prefs.getUserEmail());

        String userId = prefs.getUserId();
        if (userId.isEmpty()) {
            displayLibraryStats(new StudyStats());
            return;
        }

        StudyRepository.getInstance().getLibraryStats(
                userId,
                new ApiCallback<StudyStats>() {
                    @Override
                    public void onSuccess(StudyStats stats) {
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            displayLibraryStats(stats);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            Toast.makeText(ProfileActivity.this,
                                    "Could not load statistics: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                }
        );
    }

    private void displayLibraryStats(StudyStats stats) {
        tvTotalDocs.setText(String.valueOf(stats.getTotalDocuments()));
        tvTotalQuizzes.setText(String.valueOf(stats.getTotalQuizzes()));
        tvTotalFlashcards.setText(String.valueOf(stats.getTotalFlashcards()));
        // Schedule creation is not persisted yet, so this counter remains zero.
        tvUpcomingSessions.setText("0");
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        btnLogout.setOnClickListener(v -> confirmLogout());
        
        findViewById(R.id.layout_progress).setOnClickListener(v -> 
                startActivity(new Intent(this, ProgressActivity.class)));
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        new Thread(() -> {
            SupabaseClient.getInstance().signOut();
            runOnUiThread(() -> {
                // Xóa token đã lưu và token đang giữ trong SupabaseClient.
                SessionManager.getInstance(this).clearSession();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }).start();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_profile);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) return true;
            else if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_documents) {
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
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }
}
