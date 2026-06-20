package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.SupabaseClient;
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
        loadProfile();
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

        // TODO: Load stats from Supabase
        tvTotalDocs.setText("0");
        tvTotalQuizzes.setText("0");
        tvTotalFlashcards.setText("0");
        tvUpcomingSessions.setText("0");
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        btnLogout.setOnClickListener(v -> confirmLogout());
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
                SharedPrefManager.getInstance(this).clearSession();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
                startActivity(new Intent(this, ChatActivity.class));
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
