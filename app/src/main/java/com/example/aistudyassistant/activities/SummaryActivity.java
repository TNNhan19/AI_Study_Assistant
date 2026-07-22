package com.example.aistudyassistant.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.models.Summary;
import com.example.aistudyassistant.services.AIProcessingService;
import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class SummaryActivity extends AppCompatActivity {

    private TextView tvDocName, tvSummaryText, tvConclusion;
    private LinearLayout layoutKeyPoints, layoutLoading;
    private ChipGroup chipGroupKeywords;
    private MaterialButton btnGenerate;
    private ImageButton btnBack;

    private String documentId;
    private String documentName;
    private String documentUrl;
    private String documentType;
    private String topicId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        documentId = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_ID);
        documentName = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_NAME);
        documentUrl = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_URL);
        documentType = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_TYPE);
        topicId = getIntent().getStringExtra(Constants.EXTRA_TOPIC_ID);

        initViews();
        setupClickListeners();
        loadExistingSummary();
    }

    private void initViews() {
        tvDocName = findViewById(R.id.tv_doc_name);
        tvSummaryText = findViewById(R.id.tv_summary_text);
        tvConclusion = findViewById(R.id.tv_conclusion);
        layoutKeyPoints = findViewById(R.id.layout_key_points);
        layoutLoading = findViewById(R.id.layout_loading);
        chipGroupKeywords = findViewById(R.id.chip_group_keywords);
        btnGenerate = findViewById(R.id.btn_generate);
        btnBack = findViewById(R.id.btn_back);

        tvDocName.setText(documentName != null ? documentName : "Document");
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnGenerate.setOnClickListener(v -> generateSummary());
    }

    private void loadExistingSummary() {
        // TODO: Load existing summary from Supabase
        // If exists, display it
        // Otherwise, show generate button
    }

    private void generateSummary() {
        setGenerating(true);
        AIProcessingService.getInstance(this).createSummary(
                buildDocument(),
                new ApiCallback<Summary>() {
            @Override
            public void onSuccess(Summary result) {
                setGenerating(false);
                displaySummary(result);
            }

            @Override
            public void onError(String errorMessage) {
                setGenerating(false);
                Toast.makeText(SummaryActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displaySummary(Summary summary) {
        tvSummaryText.setText(summary.getSummaryText());

        layoutKeyPoints.removeAllViews();
        for (String keyPoint : summary.getKeyPoints()) {
            TextView tv = new TextView(this);
            tv.setText("• " + keyPoint);
            tv.setTextColor(getResources().getColor(R.color.text_primary));
            tv.setTextSize(14);
            tv.setPadding(0, 4, 0, 4);
            layoutKeyPoints.addView(tv);
        }

        chipGroupKeywords.removeAllViews();
        for (String keyword : summary.getKeywords()) {
            Chip chip = new Chip(this);
            chip.setText(keyword);
            chip.setChipBackgroundColorResource(R.color.surface_variant);
            chip.setTextColor(getResources().getColor(R.color.primary));
            chipGroupKeywords.addView(chip);
        }

        tvConclusion.setText(summary.getConclusion());
    }

    private Document buildDocument() {
        Document document = new Document();
        document.setId(documentId);
        document.setUserId(SharedPrefManager.getInstance(this).getUserId());
        document.setName(documentName);
        document.setFilePath(documentUrl);
        document.setFileType(documentType);
        document.setTopicId(topicId);
        return document;
    }

    private void setGenerating(boolean generating) {
        layoutLoading.setVisibility(generating ? View.VISIBLE : View.GONE);
        btnGenerate.setEnabled(!generating);
        btnGenerate.setText(generating ? "Generating..." : "Generate");
    }
}
