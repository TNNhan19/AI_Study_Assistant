package com.example.aistudyassistant.activities;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.models.Schedule;
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

    private TextInputEditText etTitle, etDescription;
    private TextView tvDate, tvTime;
    private LinearLayout layoutDatePicker, layoutTimePicker;
    private SwitchMaterial switchReminder;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private ImageButton btnBack;

    private Calendar selectedDateTime = Calendar.getInstance();
    private boolean dateSelected = false;
    private boolean timeSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_schedule);
        initViews();
        setupClickListeners();
    }

    private void initViews() {
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
        if (selectedDateTime.getTimeInMillis() <= System.currentTimeMillis()) {
            Toast.makeText(this, "Please select a future date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        Schedule schedule = new Schedule(title, description,
                selectedDateTime.getTimeInMillis(), switchReminder.isChecked());
        schedule.setUserId(SharedPrefManager.getInstance(this).getUserId());

        // TODO: Save to Supabase
        // new Thread(() -> {
        //     String json = buildScheduleJson(schedule);
        //     String result = SupabaseClient.getInstance().insertIntoTable(Constants.TABLE_SCHEDULES, json);
        //     int scheduleId = parseIdFromResult(result);
        //     schedule.setId(scheduleId);
        //     runOnUiThread(() -> {
        //         setLoading(false);
        //         if (scheduleId > 0) {
        //             if (schedule.isReminderEnabled()) scheduleAlarm(schedule);
        //             finish();
        //         }
        //     });
        // }).start();

        // For now (no API key), just schedule alarm and finish
        if (switchReminder.isChecked()) {
            scheduleAlarm(schedule);
        }
        Toast.makeText(this, "Schedule saved!", Toast.LENGTH_SHORT).show();
        setLoading(false);
        finish();
    }

    private void scheduleAlarm(Schedule schedule) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra(Constants.ALARM_SCHEDULE_ID, schedule.getId());
        intent.putExtra(Constants.ALARM_SCHEDULE_TITLE, schedule.getTitle());
        intent.putExtra("schedule_description", schedule.getDescription());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, schedule.getId(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    schedule.getDateTimeMillis(),
                    pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP,
                    schedule.getDateTimeMillis(), pendingIntent);
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }
}
