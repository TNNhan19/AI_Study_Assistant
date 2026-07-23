package com.example.aistudyassistant.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.adapters.ScheduleAdapter;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.models.Schedule;
import com.example.aistudyassistant.repositories.ScheduleRepository;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ScheduleActivity extends AppCompatActivity {

    private RecyclerView rvSchedules;
    private LinearLayout layoutEmpty;
    private FloatingActionButton fabCreate;
    private BottomNavigationView bottomNavigation;

    private ScheduleAdapter adapter;
    private final List<Schedule> schedules = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        initViews();
        setupRecyclerView();
        setupBottomNavigation();
        loadSchedules();
    }

    private void initViews() {
        rvSchedules = findViewById(R.id.rv_schedules);
        layoutEmpty = findViewById(R.id.layout_empty);
        fabCreate = findViewById(R.id.fab_create);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        fabCreate.setOnClickListener(v ->
                startActivity(new Intent(this, CreateScheduleActivity.class)));
    }

    private void setupRecyclerView() {
        adapter = new ScheduleAdapter(this, schedules);
        adapter.setListener(new ScheduleAdapter.OnScheduleClickListener() {
            @Override
            public void onScheduleClick(Schedule schedule) {
                // TODO: Open edit schedule screen
                Intent intent = new Intent(ScheduleActivity.this, CreateScheduleActivity.class);
                intent.putExtra("schedule_id", schedule.getId());
                startActivity(intent);
            }

            @Override
            public void onScheduleDelete(Schedule schedule, int position) {
                confirmDelete(schedule, position);
            }
        });
        rvSchedules.setLayoutManager(new LinearLayoutManager(this));
        rvSchedules.setAdapter(adapter);
    }

    private void confirmDelete(Schedule schedule, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Schedule")
                .setMessage("Delete \"" + schedule.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    cancelAlarm(schedule);
                    ScheduleRepository.getInstance().deleteSchedule(schedule.getId(), new ApiCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            runOnUiThread(() -> {
                                adapter.removeAt(position);
                                updateEmptyState();
                                Toast.makeText(ScheduleActivity.this, "Schedule deleted", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(String errorMessage) {
                            runOnUiThread(() -> Toast.makeText(
                                    ScheduleActivity.this,
                                    "Could not delete schedule: " + errorMessage,
                                    Toast.LENGTH_LONG
                            ).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void cancelAlarm(Schedule schedule) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, com.example.aistudyassistant.receivers.AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, schedule.getAlarmRequestCode(),
                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private void loadSchedules() {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        if (userId == null || userId.isEmpty()) {
            updateEmptyState();
            return;
        }

        ScheduleRepository.getInstance().getUpcomingSchedules(userId, new ApiCallback<List<Schedule>>() {
            @Override
            public void onSuccess(List<Schedule> result) {
                runOnUiThread(() -> {
                    adapter.updateSchedules(result);
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(
                            ScheduleActivity.this,
                            "Could not load schedules: " + errorMessage,
                            Toast.LENGTH_LONG
                    ).show();
                    updateEmptyState();
                });
            }
        });
    }

    private void updateEmptyState() {
        layoutEmpty.setVisibility(schedules.isEmpty() ? View.VISIBLE : View.GONE);
        rvSchedules.setVisibility(schedules.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_schedule);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_schedule) return true;
            else if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_documents) {
                startActivity(new Intent(this, DocumentsActivity.class));
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

    @Override
    protected void onResume() {
        super.onResume();
        loadSchedules();
    }
}
