package com.example.aistudyassistant.activities;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.Schedule;
import com.example.aistudyassistant.repositories.ScheduleRepository;
import com.example.aistudyassistant.receivers.AlarmReceiver;
import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CreateScheduleActivity extends AppCompatActivity {

    public static final String EXTRA_SCHEDULE_ID = "schedule_id";
    public static final String EXTRA_SCHEDULE_TITLE = "schedule_title";
    public static final String EXTRA_SCHEDULE_DESCRIPTION = "schedule_description";
    public static final String EXTRA_SCHEDULE_TIME = "schedule_time";
    public static final String EXTRA_SCHEDULE_COMPLETED = "schedule_completed";

    private TextInputEditText etTitle, etDescription;
    private TextView tvHeaderTitle, tvDate, tvTime;
    private LinearLayout layoutDatePicker, layoutTimePicker;
    private SwitchMaterial switchReminder;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private ImageButton btnBack;

    private Calendar selectedDateTime = Calendar.getInstance();
    private boolean dateSelected = false;
    private boolean timeSelected = false;
    private boolean pendingSaveAfterNotificationPermission = false;
    private String editingScheduleId;
    private boolean editingScheduleCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_schedule);
        SupabaseClient.getInstance().setAccessToken(
                SharedPrefManager.getInstance(this).getAccessToken()
        );
        initViews();
        setupClickListeners();
        loadScheduleForEditIfNeeded();
    }

    private void initViews() {
        tvHeaderTitle = findViewById(R.id.tv_title);
        etTitle = findViewById(R.id.et_title);
        etDescription = findViewById(R.id.et_description);
        tvDate = findViewById(R.id.tv_date);
        tvTime = findViewById(R.id.tv_time);
        layoutDatePicker = findViewById(R.id.layout_date_picker);
        layoutTimePicker = findViewById(R.id.layout_time_picker);
        switchReminder = findViewById(R.id.switch_reminder);
        btnSave = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.progress_bar);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        layoutDatePicker.setOnClickListener(v -> showDatePicker());
        layoutTimePicker.setOnClickListener(v -> showTimePicker());

        btnSave.setOnClickListener(v -> saveSchedule());
    }

    private void showDatePicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, day);
                    dateSelected = true;
                    tvDate.setText(new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                            .format(selectedDateTime.getTime()));
                },
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, hour);
            selectedDateTime.set(Calendar.MINUTE, minute);
            selectedDateTime.set(Calendar.SECOND, 0);
            timeSelected = true;
            tvTime.setText(new SimpleDateFormat("h:mm a", Locale.getDefault())
                    .format(selectedDateTime.getTime()));
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show();
    }

    private void saveSchedule() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Title is required");
            return;
        }
        if (!dateSelected) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!timeSelected) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
            return;
        }
        if (switchReminder.isChecked() && !hasNotificationPermission()) {
            pendingSaveAfterNotificationPermission = true;
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    Constants.REQUEST_CODE_NOTIFICATION_PERMISSION
            );
            return;
        }
        if (switchReminder.isChecked() && needsExactAlarmPermission()) {
            Intent intent = new Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
            Toast.makeText(this, "Allow exact alarms, then save the schedule again", Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);

        Schedule schedule = new Schedule(title, description,
                selectedDateTime.getTimeInMillis(), switchReminder.isChecked());
        String userId = SharedPrefManager.getInstance(this).getUserId();
        if (userId == null || userId.isEmpty()) {
            setLoading(false);
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }
        schedule.setUserId(userId);
        schedule.setId(editingScheduleId);
        schedule.setCompleted(editingScheduleCompleted);

        ApiCallback<Schedule> callback = new ApiCallback<Schedule>() {
            @Override
            public void onSuccess(Schedule savedSchedule) {
                runOnUiThread(() -> {
                    setLoading(false);
                    if (isEditing()) {
                        cancelAlarm(savedSchedule);
                    }
                    if (savedSchedule.isReminderEnabled()
                            && !savedSchedule.isCompleted()
                            && savedSchedule.getDateTimeMillis() > System.currentTimeMillis()) {
                        scheduleAlarm(savedSchedule);
                    }
                    Toast.makeText(
                            CreateScheduleActivity.this,
                            isEditing() ? "Schedule updated!" : "Schedule saved!",
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(
                            CreateScheduleActivity.this,
                            "Could not save schedule: " + errorMessage,
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        };

        if (isEditing()) {
            ScheduleRepository.getInstance().updateSchedule(schedule, callback);
        } else {
            ScheduleRepository.getInstance().createSchedule(schedule, callback);
        }
    }

    private void loadScheduleForEditIfNeeded() {
        Intent intent = getIntent();
        editingScheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID);
        if (TextUtils.isEmpty(editingScheduleId)) return;

        tvHeaderTitle.setText("Edit Schedule");
        btnSave.setText("Update Schedule");

        long scheduleTime = intent.getLongExtra(EXTRA_SCHEDULE_TIME, 0);
        if (scheduleTime > 0) {
            Schedule schedule = new Schedule();
            schedule.setId(editingScheduleId);
            schedule.setTitle(intent.getStringExtra(EXTRA_SCHEDULE_TITLE));
            schedule.setDescription(intent.getStringExtra(EXTRA_SCHEDULE_DESCRIPTION));
            schedule.setDateTimeMillis(scheduleTime);
            schedule.setCompleted(intent.getBooleanExtra(EXTRA_SCHEDULE_COMPLETED, false));
            populateSchedule(schedule);
            return;
        }

        ScheduleRepository.getInstance().getScheduleById(editingScheduleId, new ApiCallback<Schedule>() {
            @Override
            public void onSuccess(Schedule result) {
                if (result == null) return;
                runOnUiThread(() -> populateSchedule(result));
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(
                        CreateScheduleActivity.this,
                        "Could not load schedule: " + errorMessage,
                        Toast.LENGTH_LONG
                ).show());
            }
        });
    }

    private void populateSchedule(Schedule schedule) {
        etTitle.setText(schedule.getTitle());
        etDescription.setText(schedule.getDescription());
        selectedDateTime.setTimeInMillis(schedule.getDateTimeMillis());
        editingScheduleCompleted = schedule.isCompleted();
        dateSelected = true;
        timeSelected = true;
        tvDate.setText(new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                .format(selectedDateTime.getTime()));
        tvTime.setText(new SimpleDateFormat("h:mm a", Locale.getDefault())
                .format(selectedDateTime.getTime()));
    }

    private boolean isEditing() {
        return !TextUtils.isEmpty(editingScheduleId);
    }

    private void scheduleAlarm(Schedule schedule) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra(Constants.ALARM_SCHEDULE_ID, schedule.getAlarmRequestCode());
        intent.putExtra(Constants.ALARM_SCHEDULE_TITLE, schedule.getTitle());
        intent.putExtra("schedule_description", schedule.getDescription());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, schedule.getAlarmRequestCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    schedule.getDateTimeMillis(),
                    pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    schedule.getDateTimeMillis(),
                    pendingIntent);
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    schedule.getDateTimeMillis(),
                    pendingIntent);
        }
    }

    private void cancelAlarm(Schedule schedule) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                schedule.getAlarmRequestCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean needsExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false;
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        return alarmManager != null && !alarmManager.canScheduleExactAlarms();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.REQUEST_CODE_NOTIFICATION_PERMISSION
                && pendingSaveAfterNotificationPermission) {
            pendingSaveAfterNotificationPermission = false;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveSchedule();
            } else {
                Toast.makeText(this, "Notification permission is required for reminders", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }
}
