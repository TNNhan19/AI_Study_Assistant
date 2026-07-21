package com.example.aistudyassistant.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.models.StudyStats;
import com.example.aistudyassistant.repositories.StudyRepository;
import com.example.aistudyassistant.utils.SharedPrefManager;

import java.util.List;
import java.util.Locale;

public class ProgressActivity extends AppCompatActivity {

    private TextView tvDocs, tvSummaries, tvFlashcards, tvAvgQuiz, tvPerfLabel, tvNoWeak;
    private ProgressBar progressPerf;
    private LinearLayout layoutWeakTopics;
    private StudyRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        repository = StudyRepository.getInstance();
        initViews();
        loadStats();
    }

    private void initViews() {
        tvDocs = findViewById(R.id.tv_count_docs);
        tvSummaries = findViewById(R.id.tv_count_summaries);
        tvFlashcards = findViewById(R.id.tv_count_flashcards);
        tvAvgQuiz = findViewById(R.id.tv_avg_quiz);
        tvPerfLabel = findViewById(R.id.tv_performance_label);
        tvNoWeak = findViewById(R.id.tv_no_weak_topics);
        progressPerf = findViewById(R.id.progress_quiz_performance);
        layoutWeakTopics = findViewById(R.id.layout_weak_topics);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void loadStats() {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        if (userId.isEmpty()) return;

        repository.getStudyStats(userId, new ApiCallback<StudyStats>() {
            @Override
            public void onSuccess(StudyStats result) {
                runOnUiThread(() -> displayStats(result));
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(ProgressActivity.this, errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void displayStats(StudyStats stats) {
        tvDocs.setText(String.valueOf(stats.getTotalDocuments()));
        tvSummaries.setText(String.valueOf(stats.getTotalSummaries()));
        tvFlashcards.setText(String.valueOf(stats.getTotalFlashcards()));
        
        int avgScore = Math.round(stats.getAverageQuizScore());
        tvAvgQuiz.setText(String.format(Locale.getDefault(), "%d%%", avgScore));
        
        progressPerf.setProgress(avgScore);
        tvPerfLabel.setText(getPerformanceMessage(avgScore));

        // Display weak topics
        layoutWeakTopics.removeAllViews();
        List<StudyStats.TopicPerformance> weakTopics = stats.getWeakTopics();
        
        if (weakTopics == null || weakTopics.isEmpty()) {
            tvNoWeak.setVisibility(View.VISIBLE);
        } else {
            tvNoWeak.setVisibility(View.GONE);
            for (StudyStats.TopicPerformance tp : weakTopics) {
                addWeakTopicView(tp);
            }
        }
    }

    private void addWeakTopicView(StudyStats.TopicPerformance tp) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_weak_topic, layoutWeakTopics, false);
        TextView tvName = view.findViewById(R.id.tv_topic_name);
        TextView tvScore = view.findViewById(R.id.tv_topic_score);
        ProgressBar pb = view.findViewById(R.id.pb_topic_score);

        tvName.setText(tp.getTopicName());
        int score = Math.round(tp.getAverageScore());
        tvScore.setText(score + "%");
        pb.setProgress(score);

        layoutWeakTopics.addView(view);
    }

    private String getPerformanceMessage(int score) {
        if (score >= 90) return "Excellent! You've mastered the content.";
        if (score >= 70) return "Great job! Keep up the good work.";
        if (score >= 50) return "Good progress, but there's room to improve.";
        return "Keep studying! Focus on your weak topics.";
    }
}
