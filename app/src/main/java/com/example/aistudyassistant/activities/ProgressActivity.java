package com.example.aistudyassistant.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.Project;
import com.example.aistudyassistant.models.ProjectProgress;
import com.example.aistudyassistant.models.QuizResult;
import com.example.aistudyassistant.repositories.ProgressRepository;
import com.example.aistudyassistant.repositories.ProjectRepository;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProgressActivity extends AppCompatActivity {

    private static final int TARGET_SCORE = 70;

    private Spinner spinnerProject;
    private TextView tvDocs, tvSummaries, tvFlashcards, tvLatestScore;
    private TextView tvNoQuizPerformance, tvNoWeak, tvProgressEmpty, tvProgressLoading;
    private LinearLayout layoutWeakTopics;
    private CombinedChart chartQuizPerformance;

    private final List<Project> projects = new ArrayList<>();
    private ProgressRepository progressRepository;
    private String userId;
    private String selectedProjectId;
    private int loadVersion;
    private boolean suppressInitialSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        SupabaseClient.getInstance().setAccessToken(
                SharedPrefManager.getInstance(this).getAccessToken()
        );
        progressRepository = ProgressRepository.getInstance();
        userId = SharedPrefManager.getInstance(this).getUserId();

        initViews();
        setupChart();
        loadProjects();
    }

    private void initViews() {
        spinnerProject = findViewById(R.id.spinner_project);
        tvDocs = findViewById(R.id.tv_count_docs);
        tvSummaries = findViewById(R.id.tv_count_summaries);
        tvFlashcards = findViewById(R.id.tv_count_flashcards);
        tvLatestScore = findViewById(R.id.tv_avg_quiz);
        tvNoQuizPerformance = findViewById(R.id.tv_no_quiz_performance);
        tvNoWeak = findViewById(R.id.tv_no_weak_topics);
        tvProgressEmpty = findViewById(R.id.tv_progress_empty);
        tvProgressLoading = findViewById(R.id.tv_progress_loading);
        layoutWeakTopics = findViewById(R.id.layout_weak_topics);
        chartQuizPerformance = findViewById(R.id.chart_quiz_performance);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        spinnerProject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (suppressInitialSelection) {
                    suppressInitialSelection = false;
                    return;
                }
                if (position < 0 || position >= projects.size()) return;
                String projectId = projects.get(position).getId();
                if (projectId.equals(selectedProjectId)) return;
                selectedProjectId = projectId;
                loadProgressForProject(projectId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupChart() {
        chartQuizPerformance.getDescription().setEnabled(false);
        chartQuizPerformance.setDrawGridBackground(false);
        chartQuizPerformance.setTouchEnabled(true);
        chartQuizPerformance.setDragEnabled(false);
        chartQuizPerformance.setScaleEnabled(false);

        Legend legend = chartQuizPerformance.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(getResources().getColor(R.color.text_secondary));
        legend.setTextSize(12f);

        YAxis leftAxis = chartQuizPerformance.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setTextColor(getResources().getColor(R.color.text_secondary));
        leftAxis.setDrawGridLines(true);

        chartQuizPerformance.getAxisRight().setEnabled(false);

        XAxis xAxis = chartQuizPerformance.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(getResources().getColor(R.color.text_secondary));
    }

    private void loadProjects() {
        if (isBlank(userId)) {
            showEmptyProjectState("Please sign in again to view progress.");
            return;
        }

        setLoading(true);
        ProjectRepository.getInstance().getAllProjects(userId, new ApiCallback<List<Project>>() {
            @Override
            public void onSuccess(List<Project> result) {
                runOnUiThread(() -> {
                    projects.clear();
                    if (result != null) projects.addAll(result);
                    if (projects.isEmpty()) {
                        setLoading(false);
                        showEmptyProjectState("Create a project to view progress.");
                        clearProgressUi();
                        return;
                    }
                    tvProgressEmpty.setVisibility(View.GONE);
                    bindProjectSelector();
                    selectedProjectId = projects.get(0).getId();
                    loadProgressForProject(selectedProjectId);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showEmptyProjectState("Create a project to view progress.");
                    Toast.makeText(
                            ProgressActivity.this,
                            isBlank(errorMessage) ? "Could not load projects" : errorMessage,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void bindProjectSelector() {
        List<String> labels = new ArrayList<>();
        for (Project project : projects) {
            labels.add(isBlank(project.getName()) ? "Untitled project" : project.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        suppressInitialSelection = true;
        spinnerProject.setAdapter(adapter);
        spinnerProject.setSelection(0);
    }

    private void loadProgressForProject(String projectId) {
        if (isBlank(projectId)) {
            showEmptyProjectState("No project selected.");
            return;
        }

        int requestVersion = ++loadVersion;
        setLoading(true);
        clearProgressUi();
        progressRepository.getProjectProgress(
                userId,
                projectId,
                new ApiCallback<ProjectProgress>() {
                    @Override
                    public void onSuccess(ProjectProgress result) {
                        runOnUiThread(() -> {
                            if (requestVersion != loadVersion) return;
                            setLoading(false);
                            displayProgress(result);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            if (requestVersion != loadVersion) return;
                            setLoading(false);
                            clearProgressUi();
                            Toast.makeText(
                                    ProgressActivity.this,
                                    isBlank(errorMessage)
                                            ? "Could not load progress"
                                            : errorMessage,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void displayProgress(ProjectProgress progress) {
        if (progress == null) {
            clearProgressUi();
            return;
        }

        tvDocs.setText(String.valueOf(progress.getDocumentCount()));
        tvSummaries.setText(String.valueOf(progress.getSummaryCount()));
        tvFlashcards.setText(String.valueOf(progress.getFlashcardCount()));

        QuizResult latest = progress.getLatestQuizResult();
        if (latest == null || latest.getTotalQuestions() <= 0) {
            tvLatestScore.setText("--");
        } else {
            tvLatestScore.setText(String.format(
                    Locale.getDefault(),
                    "%d%%",
                    scorePercent(latest)));
        }

        renderQuizChart(progress.getRecentQuizResults());
        renderSuggestions(latest, progress.getLatestQuizDocumentName());
    }

    private void renderQuizChart(List<QuizResult> results) {
        if (results == null || results.isEmpty()) {
            chartQuizPerformance.clear();
            chartQuizPerformance.setVisibility(View.GONE);
            tvNoQuizPerformance.setVisibility(View.VISIBLE);
            return;
        }

        List<BarEntry> scoreEntries = new ArrayList<>();
        List<Entry> targetEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            QuizResult result = results.get(i);
            scoreEntries.add(new BarEntry(i, scorePercent(result)));
            targetEntries.add(new Entry(i, TARGET_SCORE));
            labels.add("Quiz " + (i + 1));
        }

        BarDataSet barSet = new BarDataSet(scoreEntries, "Quiz Score");
        barSet.setColor(getResources().getColor(R.color.primary));
        barSet.setValueTextColor(getResources().getColor(R.color.text_secondary));
        barSet.setValueTextSize(10f);

        LineDataSet targetSet = new LineDataSet(targetEntries, "Target 70%");
        targetSet.setColor(getResources().getColor(R.color.quiz_wrong));
        targetSet.setLineWidth(2f);
        targetSet.setDrawCircles(false);
        targetSet.setDrawValues(false);

        BarData barData = new BarData(barSet);
        barData.setBarWidth(0.45f);

        CombinedData data = new CombinedData();
        data.setData(barData);
        data.setData(new LineData(targetSet));

        XAxis xAxis = chartQuizPerformance.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(results.size() - 0.5f);
        xAxis.setLabelCount(labels.size(), false);

        chartQuizPerformance.setData(data);
        chartQuizPerformance.invalidate();
        chartQuizPerformance.setVisibility(View.VISIBLE);
        tvNoQuizPerformance.setVisibility(View.GONE);
    }

    private void renderSuggestions(QuizResult latest, String documentName) {
        layoutWeakTopics.removeAllViews();

        if (latest == null || latest.getTotalQuestions() <= 0) {
            tvNoWeak.setText("Complete a quiz in this project to get suggestions.");
            tvNoWeak.setVisibility(View.VISIBLE);
            return;
        }

        tvNoWeak.setVisibility(View.GONE);
        int percent = scorePercent(latest);
        if (percent < TARGET_SCORE) {
            if (!isBlank(documentName)) {
                addSuggestion("Review document: " + documentName);
            } else {
                addSuggestion("Review the source document again.");
            }
            addSuggestion("Review flashcards from this project.");
            addSuggestion("Retake the quiz after reviewing.");
        } else {
            addSuggestion("Good progress. Keep practicing with more quizzes and flashcards.");
        }
    }

    private void addSuggestion(String text) {
        TextView suggestion = new TextView(this);
        suggestion.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        suggestion.setText(text);
        suggestion.setTextColor(getResources().getColor(R.color.text_secondary));
        suggestion.setTextSize(14f);
        suggestion.setPadding(0, 8, 0, 8);
        layoutWeakTopics.addView(suggestion);
    }

    private void clearProgressUi() {
        tvDocs.setText("0");
        tvSummaries.setText("0");
        tvFlashcards.setText("0");
        tvLatestScore.setText("--");
        chartQuizPerformance.clear();
        chartQuizPerformance.setVisibility(View.GONE);
        tvNoQuizPerformance.setVisibility(View.VISIBLE);
        layoutWeakTopics.removeAllViews();
        tvNoWeak.setText("Complete a quiz in this project to get suggestions.");
        tvNoWeak.setVisibility(View.VISIBLE);
    }

    private void showEmptyProjectState(String message) {
        tvProgressEmpty.setText(message);
        tvProgressEmpty.setVisibility(View.VISIBLE);
        spinnerProject.setEnabled(false);
    }

    private void setLoading(boolean loading) {
        tvProgressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private int scorePercent(QuizResult result) {
        if (result == null || result.getTotalQuestions() <= 0) return 0;
        return Math.round(result.getScore() * 100f / result.getTotalQuestions());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
