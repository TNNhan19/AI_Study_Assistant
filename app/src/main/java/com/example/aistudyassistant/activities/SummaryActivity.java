package com.example.aistudyassistant.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.GeminiClient;
import com.example.aistudyassistant.models.Summary;
import com.example.aistudyassistant.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SummaryActivity extends AppCompatActivity {

    private TextView tvDocName, tvSummaryText, tvConclusion;
    private LinearLayout layoutKeyPoints, layoutLoading;
    private ChipGroup chipGroupKeywords;
    private MaterialButton btnGenerate;
    private ImageButton btnBack;

    private String documentId;
    private String documentName;
    private String documentUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        documentId = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_ID);
        documentName = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_NAME);
        documentUrl = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_URL);

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

        new Thread(() -> {
            // TODO: First download/read the document text from Supabase Storage
            // String documentText = downloadDocumentText(documentUrl);

            // MOCK: Use placeholder for now
            String documentText = "Sample document content. Replace this with actual document text.";

            String responseJson = GeminiClient.getInstance().generateSummary(documentText);

            runOnUiThread(() -> {
                setGenerating(false);
                if (responseJson != null) {
                    displaySummary(responseJson);
                } else {
                    Toast.makeText(this, "Failed to generate summary. Check your API key.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void displaySummary(String responseJson) {
        try {
            // Parse JSON response from Gemini
            // Clean markdown code blocks if present
            String cleaned = responseJson.trim();
            if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
            if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
            cleaned = cleaned.trim();

            JsonObject json = JsonParser.parseString(cleaned).getAsJsonObject();

            // Summary
            if (json.has("summary")) {
                tvSummaryText.setText(json.get("summary").getAsString());
            }

            // Key Points
            if (json.has("keyPoints")) {
                layoutKeyPoints.removeAllViews();
                JsonArray keyPoints = json.getAsJsonArray("keyPoints");
                for (int i = 0; i < keyPoints.size(); i++) {
                    TextView tv = new TextView(this);
                    tv.setText("• " + keyPoints.get(i).getAsString());
                    tv.setTextColor(getResources().getColor(R.color.text_primary));
                    tv.setTextSize(14);
                    tv.setPadding(0, 4, 0, 4);
                    layoutKeyPoints.addView(tv);
                }
            }

            // Keywords chips
            if (json.has("keywords")) {
                chipGroupKeywords.removeAllViews();
                JsonArray keywords = json.getAsJsonArray("keywords");
                for (int i = 0; i < keywords.size(); i++) {
                    Chip chip = new Chip(this);
                    chip.setText(keywords.get(i).getAsString());
                    chip.setChipBackgroundColorResource(R.color.surface_variant);
                    chip.setTextColor(getResources().getColor(R.color.primary));
                    chipGroupKeywords.addView(chip);
                }
            }

            // Conclusion
            if (json.has("conclusion")) {
                tvConclusion.setText(json.get("conclusion").getAsString());
            }

            // TODO: Save to Supabase

        } catch (Exception e) {
            // If JSON parsing fails, show raw text
            tvSummaryText.setText(responseJson);
        }
    }

    private void setGenerating(boolean generating) {
        layoutLoading.setVisibility(generating ? View.VISIBLE : View.GONE);
        btnGenerate.setEnabled(!generating);
        btnGenerate.setText(generating ? "Generating..." : "Generate");
    }
}
