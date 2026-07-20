package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.adapters.SearchResultAdapter;
import com.example.aistudyassistant.models.SearchResult;
import com.example.aistudyassistant.repositories.SearchRepository;
import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private TextInputEditText etSearch;
    private ImageButton btnBack;
    private LinearLayout layoutRecent, layoutResults, layoutEmpty;
    private ChipGroup chipGroupRecent;
    private RecyclerView rvResults;
    private TextView tvResultsCount, tvClearAll;
    private ProgressBar progressBar;

    private SearchResultAdapter adapter;
    private final List<SearchResult> searchResults = new ArrayList<>();
    private final List<String> recentSearches = new ArrayList<>();
    private SearchRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        repository = SearchRepository.getInstance();
        initViews();
        setupSearch();
        loadRecentSearches();
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        btnBack = findViewById(R.id.btn_back);
        layoutRecent = findViewById(R.id.layout_recent);
        layoutResults = findViewById(R.id.layout_results);
        layoutEmpty = findViewById(R.id.layout_empty);
        chipGroupRecent = findViewById(R.id.chip_group_recent);
        rvResults = findViewById(R.id.rv_results);
        tvResultsCount = findViewById(R.id.tv_results_count);
        tvClearAll = findViewById(R.id.tv_clear_all);
        progressBar = findViewById(R.id.progress_bar);

        btnBack.setOnClickListener(v -> finish());
        tvClearAll.setOnClickListener(v -> clearRecentSearches());

        // Setup RecyclerView
        adapter = new SearchResultAdapter(this, searchResults);
        adapter.setListener(result -> handleResultClick(result));
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);
    }

    private void handleResultClick(SearchResult result) {
        Intent intent;
        switch (result.getType()) {
            case DOCUMENT:
                intent = new Intent(this, DocumentDetailActivity.class);
                intent.putExtra(Constants.EXTRA_DOCUMENT_ID, result.getId());
                intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, result.getTitle());
                startActivity(intent);
                break;
            case PROJECT:
                intent = new Intent(this, TopicsActivity.class);
                intent.putExtra("project_id", result.getId());
                intent.putExtra("project_name", result.getTitle());
                startActivity(intent);
                break;
            case TOPIC:
                // For simplicity, we just toast for now or you could open a specific view
                Toast.makeText(this, "Topic: " + result.getTitle(), Toast.LENGTH_SHORT).show();
                break;
            case NOTE:
                intent = new Intent(this, EditNoteActivity.class);
                intent.putExtra("note_id", result.getId());
                intent.putExtra("note_title", result.getTitle());
                intent.putExtra("note_content", result.getSubtitle());
                startActivity(intent);
                break;
            case FLASHCARD:
                intent = new Intent(this, FlashcardsActivity.class);
                intent.putExtra(Constants.EXTRA_DOCUMENT_ID, result.getId()); // In real app, might need more context
                startActivity(intent);
                break;
            case QUIZ:
                intent = new Intent(this, QuizActivity.class);
                intent.putExtra(Constants.EXTRA_DOCUMENT_ID, result.getId());
                startActivity(intent);
                break;
        }
    }

    private void setupSearch() {
        etSearch.requestFocus();
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    showState("recent");
                } else if (query.length() >= 2) {
                    performSearch(query);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String query) {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        if (userId.isEmpty()) return;

        showState("loading");

        repository.globalSearch(userId, query, new ApiCallback<List<SearchResult>>() {
            @Override
            public void onSuccess(List<SearchResult> result) {
                runOnUiThread(() -> {
                    searchResults.clear();
                    searchResults.addAll(result);
                    adapter.notifyDataSetChanged();
                    showState(searchResults.isEmpty() ? "empty" : "results");
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(SearchActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    showState("empty");
                });
            }
        });

        // Save to recent
        if (!recentSearches.contains(query)) {
            recentSearches.add(0, query);
            if (recentSearches.size() > 8) recentSearches.remove(recentSearches.size() - 1);
            saveRecentSearches();
        }
    }

    private void loadRecentSearches() {
        String saved = SharedPrefManager.getInstance(this).getString("recent_queries", "");
        if (!saved.isEmpty()) {
            recentSearches.clear();
            for (String q : saved.split("\\|")) {
                if (!q.isEmpty()) recentSearches.add(q);
            }
        }
        updateRecentChips();
    }

    private void saveRecentSearches() {
        StringBuilder sb = new StringBuilder();
        for (String q : recentSearches) sb.append(q).append("|");
        SharedPrefManager.getInstance(this).putString("recent_queries", sb.toString());
        updateRecentChips();
    }

    private void updateRecentChips() {
        chipGroupRecent.removeAllViews();
        for (String search : recentSearches) {
            addRecentChip(search);
        }
    }

    private void addRecentChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setOnClickListener(v -> {
            etSearch.setText(text);
            etSearch.setSelection(text.length());
        });
        chip.setOnCloseIconClickListener(v -> {
            recentSearches.remove(text);
            saveRecentSearches();
        });
        chipGroupRecent.addView(chip);
    }

    private void clearRecentSearches() {
        recentSearches.clear();
        saveRecentSearches();
    }

    private void showState(String state) {
        layoutRecent.setVisibility(View.GONE);
        layoutResults.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        switch (state) {
            case "recent":
                layoutRecent.setVisibility(View.VISIBLE);
                break;
            case "loading":
                progressBar.setVisibility(View.VISIBLE);
                break;
            case "results":
                tvResultsCount.setText(searchResults.size() + " results found");
                layoutResults.setVisibility(View.VISIBLE);
                break;
            case "empty":
                layoutEmpty.setVisibility(View.VISIBLE);
                break;
        }
    }
}
