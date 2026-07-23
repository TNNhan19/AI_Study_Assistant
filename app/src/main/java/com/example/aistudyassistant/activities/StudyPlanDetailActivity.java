package com.example.aistudyassistant.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class StudyPlanDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PLAN_TITLE = "extra_plan_title";
    public static final String EXTRA_EXAM_DATE = "extra_exam_date";
    public static final String EXTRA_PLAN_DATA = "extra_plan_data";
    public static final String EXTRA_CREATED_AT = "extra_created_at";

    private TextView tvPlanTitle, tvPlanInfo, tvRawPlan;
    private LinearLayout layoutPlanDays;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_plan_detail);

        initViews();
        displayPlan();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        tvPlanTitle = findViewById(R.id.tv_plan_title);
        tvPlanInfo = findViewById(R.id.tv_plan_info);
        tvRawPlan = findViewById(R.id.tv_raw_plan);
        layoutPlanDays = findViewById(R.id.layout_plan_days);

        btnBack.setOnClickListener(v -> finish());
    }

    private void displayPlan() {
        String title = getIntent().getStringExtra(EXTRA_PLAN_TITLE);
        String examDate = getIntent().getStringExtra(EXTRA_EXAM_DATE);
        String planData = getIntent().getStringExtra(EXTRA_PLAN_DATA);
        String createdAt = getIntent().getStringExtra(EXTRA_CREATED_AT);

        tvPlanTitle.setText(isBlank(title) ? getString(R.string.ai_study_plan) : title);
        tvPlanInfo.setText(buildPlanInfo(planData, examDate, createdAt));
        layoutPlanDays.removeAllViews();
        tvRawPlan.setVisibility(View.GONE);

        try {
            JsonObject root = JsonParser.parseString(planData).getAsJsonObject();
            JsonArray days = root.has("days") && root.get("days").isJsonArray()
                    ? root.getAsJsonArray("days")
                    : new JsonArray();
            if (days.size() == 0) {
                showRawPlan(planData);
                return;
            }

            for (JsonElement element : days) {
                if (!element.isJsonObject()) continue;
                layoutPlanDays.addView(createDayView(element.getAsJsonObject()));
            }
        } catch (Exception error) {
            showRawPlan(planData);
        }
    }

    private String buildPlanInfo(String planData, String examDate, String createdAt) {
        StringBuilder builder = new StringBuilder();
        String subject = readPlanJsonString(planData, "subject");
        String studyTime = readPlanJsonString(planData, "study_time_per_day");
        int dayCount = countPlanDays(planData);

        appendLine(builder, getString(R.string.study_plan_subject_format, subject), !isBlank(subject));
        appendLine(builder, getString(R.string.study_plan_exam_date_format, examDate), !isBlank(examDate));
        appendLine(builder, getString(R.string.study_plan_study_time_format, studyTime), !isBlank(studyTime));
        appendLine(builder, getString(R.string.study_plan_day_count_format, dayCount), dayCount > 0);
        appendLine(builder, getString(R.string.study_plan_created_at_format, createdAt), !isBlank(createdAt));

        return builder.toString().trim();
    }

    private void appendLine(StringBuilder builder, String line, boolean shouldAppend) {
        if (!shouldAppend) return;
        if (builder.length() > 0) builder.append('\n');
        builder.append(line);
    }

    private View createDayView(JsonObject day) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        container.setLayoutParams(params);
        container.setBackgroundColor(getResources().getColor(R.color.surface));

        TextView title = createText(readString(day, "date") + " - " + readString(day, "topic"),
                R.color.text_primary,
                true);
        container.addView(title);

        String tasks = formatTasks(day.get("tasks"));
        if (!isBlank(tasks)) {
            container.addView(createText(tasks, R.color.text_secondary, false));
        }

        String estimatedTime = readString(day, "estimated_time");
        if (!isBlank(estimatedTime)) {
            container.addView(createText(
                    getString(R.string.estimated_time_format, estimatedTime),
                    R.color.primary,
                    false
            ));
        }

        String method = readString(day, "study_method");
        if (!isBlank(method)) {
            container.addView(createText(
                    getString(R.string.study_method_format, method),
                    R.color.text_secondary,
                    false
            ));
        }
        return container;
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

    private void showRawPlan(String planData) {
        tvRawPlan.setVisibility(View.VISIBLE);
        tvRawPlan.setText(isBlank(planData) ? getString(R.string.plan_parse_error) : planData);
    }

    private String formatTasks(JsonElement tasksElement) {
        if (tasksElement == null || tasksElement.isJsonNull()) return "";
        if (!tasksElement.isJsonArray()) return tasksElement.getAsString();
        StringBuilder builder = new StringBuilder();
        for (JsonElement task : tasksElement.getAsJsonArray()) {
            if (task == null || task.isJsonNull()) continue;
            builder.append("- ").append(task.getAsString()).append('\n');
        }
        return builder.toString().trim();
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

    private String readString(JsonObject json, String key) {
        return !json.has(key) || json.get(key).isJsonNull()
                ? ""
                : json.get(key).getAsString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
