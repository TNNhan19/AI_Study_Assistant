package com.example.aistudyassistant.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.models.Project;
import com.example.aistudyassistant.models.StudyPlan;
import com.example.aistudyassistant.repositories.DocumentRepository;
import com.example.aistudyassistant.repositories.ProjectRepository;
import com.example.aistudyassistant.repositories.StudyPlanRepository;
import com.example.aistudyassistant.services.AIProcessingService;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StudyPlanActivity extends AppCompatActivity {

    private static final String CUSTOM_SUBJECT_ID = "";

    private Spinner spProject;
    private EditText etSubject, etExamDate, etStudyTime;
    private MaterialButton btnGenerate;
    private ProgressBar progressBar;
    private TextView tvNoSavedPlans;
    private LinearLayout layoutSavedPlans;

    private final List<Project> projects = new ArrayList<>();
    private final List<Document> documents = new ArrayList<>();
    private final List<String> projectLabels = new ArrayList<>();
    private final List<StudyPlan> savedPlans = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_plan);

        SupabaseClient.getInstance().setAccessToken(
                SharedPrefManager.getInstance(this).getAccessToken()
        );
        initViews();
        setupListeners();
        loadProjectsAndDocuments();
        loadSavedStudyPlans();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        spProject = findViewById(R.id.sp_project);
        etSubject = findViewById(R.id.et_subject);
        etExamDate = findViewById(R.id.et_exam_date);
        etStudyTime = findViewById(R.id.et_study_time);
        btnGenerate = findViewById(R.id.btn_generate_study_plan);
        progressBar = findViewById(R.id.progress_bar);
        tvNoSavedPlans = findViewById(R.id.tv_no_saved_plans);
        layoutSavedPlans = findViewById(R.id.layout_saved_plans);

        btnBack.setOnClickListener(v -> finish());
        setupProjectSpinner();
    }

    private void setupListeners() {
        etExamDate.setOnClickListener(v -> showDatePicker());
        btnGenerate.setOnClickListener(v -> generateStudyPlan());
    }

    private void setupProjectSpinner() {
        projectLabels.clear();
        projectLabels.add(getString(R.string.custom_subject));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                projectLabels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProject.setAdapter(adapter);
    }

    private void loadProjectsAndDocuments() {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        if (isBlank(userId)) return;

        ProjectRepository.getInstance().getAllProjects(userId, new ApiCallback<List<Project>>() {
            @Override
            public void onSuccess(List<Project> result) {
                runOnUiThread(() -> {
                    projects.clear();
                    projects.addAll(result);
                    projectLabels.clear();
                    projectLabels.add(getString(R.string.custom_subject));
                    for (Project project : projects) {
                        projectLabels.add(project.getName());
                    }
                    ((ArrayAdapter<?>) spProject.getAdapter()).notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(
                        StudyPlanActivity.this,
                        "Could not load projects",
                        Toast.LENGTH_SHORT
                ).show());
            }
        });

        DocumentRepository.getInstance().getAllDocuments(userId, new ApiCallback<List<Document>>() {
            @Override
            public void onSuccess(List<Document> result) {
                documents.clear();
                documents.addAll(result);
            }

            @Override
            public void onError(String errorMessage) {
                documents.clear();
            }
        });
    }

    private void loadSavedStudyPlans() {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        if (isBlank(userId)) return;

        StudyPlanRepository.getInstance().getStudyPlansByUser(userId, new ApiCallback<List<StudyPlan>>() {
            @Override
            public void onSuccess(List<StudyPlan> result) {
                runOnUiThread(() -> {
                    savedPlans.clear();
                    savedPlans.addAll(result);
                    renderSavedPlans();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    savedPlans.clear();
                    renderSavedPlans();
                });
            }
        });
    }

    private void renderSavedPlans() {
        layoutSavedPlans.removeAllViews();
        boolean hasPlans = !savedPlans.isEmpty();
        tvNoSavedPlans.setVisibility(hasPlans ? View.GONE : View.VISIBLE);
        if (!hasPlans) return;

        for (StudyPlan plan : savedPlans) {
            layoutSavedPlans.addView(createSavedPlanView(plan));
        }
    }

    private View createSavedPlanView(StudyPlan plan) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(16, 16, 16, 16);
        container.setBackgroundColor(getResources().getColor(R.color.surface));
        container.setClickable(true);
        container.setFocusable(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        container.setLayoutParams(params);

        String title = isBlank(plan.getTitle()) ? getString(R.string.ai_study_plan) : plan.getTitle();
        container.addView(createText(title, R.color.text_primary, true));

        String subject = readPlanJsonString(plan.getPlanData(), "subject");
        if (!isBlank(subject)) {
            container.addView(createText(
                    getString(R.string.study_plan_subject_format, subject),
                    R.color.text_secondary,
                    false
            ));
        }
        if (!isBlank(plan.getExamDate())) {
            container.addView(createText(
                    getString(R.string.study_plan_exam_date_format, plan.getExamDate()),
                    R.color.text_secondary,
                    false
            ));
        }
        String studyTime = readPlanJsonString(plan.getPlanData(), "study_time_per_day");
        if (!isBlank(studyTime)) {
            container.addView(createText(
                    getString(R.string.study_plan_study_time_format, studyTime),
                    R.color.text_secondary,
                    false
            ));
        }
        int dayCount = countPlanDays(plan.getPlanData());
        if (dayCount > 0) {
            container.addView(createText(
                    getString(R.string.study_plan_day_count_format, dayCount),
                    R.color.primary,
                    false
            ));
        }
        if (!isBlank(plan.getCreatedAt())) {
            container.addView(createText(
                    getString(R.string.study_plan_created_at_format, plan.getCreatedAt()),
                    R.color.text_secondary,
                    false
            ));
        }
        container.setOnClickListener(v -> openStudyPlanDetail(plan));
        return container;
    }

    private void openStudyPlanDetail(StudyPlan plan) {
        Intent intent = new Intent(this, StudyPlanDetailActivity.class);
        intent.putExtra(StudyPlanDetailActivity.EXTRA_PLAN_TITLE, plan.getTitle());
        intent.putExtra(StudyPlanDetailActivity.EXTRA_EXAM_DATE, plan.getExamDate());
        intent.putExtra(StudyPlanDetailActivity.EXTRA_PLAN_DATA, plan.getPlanData());
        intent.putExtra(StudyPlanDetailActivity.EXTRA_CREATED_AT, plan.getCreatedAt());
        startActivity(intent);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> etExamDate.setText(String.format(
                        Locale.US,
                        "%04d-%02d-%02d",
                        year,
                        month + 1,
                        dayOfMonth
                )),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void generateStudyPlan() {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        if (isBlank(userId)) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }

        String projectId = getSelectedProjectId();
        String subject = getSelectedSubject();
        String examDate = etExamDate.getText().toString().trim();
        String studyTime = etStudyTime.getText().toString().trim();

        if (isBlank(subject)) {
            etSubject.setError(getString(R.string.subject_required));
            return;
        }
        if (isBlank(examDate)) {
            etExamDate.setError(getString(R.string.exam_date_required));
            return;
        }
        if (isBlank(studyTime)) {
            etStudyTime.setError(getString(R.string.study_time_required));
            return;
        }

        setLoading(true);
        AIProcessingService.getInstance(this).generateStudyPlan(
                subject,
                examDate,
                studyTime,
                getDocumentTitles(projectId),
                new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String planData) {
                        saveGeneratedPlan(userId, projectId, subject, examDate, planData);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(
                                    StudyPlanActivity.this,
                                    errorMessage,
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    }

                    @Override
                    public void onWaitingForNetwork() {
                        Toast.makeText(
                                StudyPlanActivity.this,
                                "No connection. Study plan will generate when network returns.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void saveGeneratedPlan(String userId, String projectId, String subject,
                                   String examDate, String planData) {
        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setProjectId(projectId);
        plan.setTitle(readPlanTitle(planData, subject));
        plan.setExamDate(examDate);
        plan.setPlanData(planData);

        StudyPlanRepository.getInstance().saveStudyPlan(plan, new ApiCallback<StudyPlan>() {
            @Override
            public void onSuccess(StudyPlan savedPlan) {
                runOnUiThread(() -> {
                    setLoading(false);
                    loadSavedStudyPlans();
                    Toast.makeText(
                            StudyPlanActivity.this,
                            "Study plan saved",
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(
                            StudyPlanActivity.this,
                            errorMessage,
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private TextView createText(String text, int colorRes, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(getResources().getColor(colorRes));
        textView.setTextSize(bold ? 16 : 14);
        if (bold) textView.setTypeface(null, android.graphics.Typeface.BOLD);
        textView.setPadding(0, 4, 0, 4);
        return textView;
    }

    private String getSelectedProjectId() {
        int position = spProject.getSelectedItemPosition();
        if (position <= 0 || position - 1 >= projects.size()) return CUSTOM_SUBJECT_ID;
        return projects.get(position - 1).getId();
    }

    private String getSelectedSubject() {
        int position = spProject.getSelectedItemPosition();
        if (position > 0 && position - 1 < projects.size()) {
            return projects.get(position - 1).getName();
        }
        return etSubject.getText().toString().trim();
    }

    private List<String> getDocumentTitles(String projectId) {
        List<String> titles = new ArrayList<>();
        if (isBlank(projectId)) return titles;
        for (Document document : documents) {
            if (projectId.equals(document.getProjectId()) && !isBlank(document.getName())) {
                titles.add(document.getName());
            }
        }
        return titles;
    }

    private String readPlanTitle(String planData, String fallbackSubject) {
        try {
            JsonObject root = JsonParser.parseString(planData).getAsJsonObject();
            String title = readString(root, "title");
            if (!isBlank(title)) return title;
        } catch (Exception ignored) {
            // Fallback below.
        }
        return "Study Plan - " + fallbackSubject;
    }

    private String readString(JsonObject json, String key) {
        return !json.has(key) || json.get(key).isJsonNull()
                ? ""
                : json.get(key).getAsString();
    }

    private String readPlanJsonString(String planData, String key) {
        try {
            JsonObject root = JsonParser.parseString(planData).getAsJsonObject();
            return readString(root, key);
        } catch (Exception ignored) {
            return "";
        }
    }

    private int countPlanDays(String planData) {
        try {
            JsonObject root = JsonParser.parseString(planData).getAsJsonObject();
            JsonElement days = root.get("days");
            return days != null && days.isJsonArray() ? days.getAsJsonArray().size() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnGenerate.setEnabled(!loading);
        btnGenerate.setText(loading ? R.string.generating : R.string.generate_study_plan);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
