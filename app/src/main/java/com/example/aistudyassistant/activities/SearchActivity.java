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

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.adapters.DocumentAdapter;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.utils.Constants;
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

    private DocumentAdapter adapter;
    private final List<Document> searchResults = new ArrayList<>();
    private final List<String> recentSearches = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
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
        adapter = new DocumentAdapter(this, searchResults);
        adapter.setListener(new DocumentAdapter.OnDocumentClickListener() {
            @Override
            public void onDocumentClick(Document document) {
                Intent intent = new Intent(SearchActivity.this, DocumentDetailActivity.class);
                intent.putExtra(Constants.EXTRA_DOCUMENT_ID, document.getId());
                intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, document.getName());
                startActivity(intent);
            }
            @Override
            public void onDocumentMoreClick(Document document, View anchorView) {}
        });
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.requestFocus();
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    showState("recent");
                } else {
                    performSearch(query);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String query) {
        showState("loading");

        // TODO: Search documents in Supabase
        // new Thread(() -> {
        //     String userId = SharedPrefManager.getInstance(this).getUserId();
        //     String response = SupabaseClient.getInstance().getFromTable(
        //         Constants.TABLE_DOCUMENTS,
        //         "user_id=eq." + userId + "&name=ilike.*" + query + "*");
        //     runOnUiThread(() -> {
        //         parseAndDisplayResults(response);
        //     });
        // }).start();

        // Save recent search
        if (!recentSearches.contains(query)) {
            recentSearches.add(0, query);
            if (recentSearches.size() > 10) recentSearches.remove(recentSearches.size() - 1);
        }

        // Show empty for now (no API)
        searchResults.clear();
        adapter.notifyDataSetChanged();
        showState(searchResults.isEmpty() ? "empty" : "results");
    }

    private void loadRecentSearches() {
        // TODO: Load from SharedPreferences
        chipGroupRecent.removeAllViews();
        for (String search : recentSearches) {
            addRecentChip(search);
        }
    }

    private void addRecentChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.surface_variant);
        chip.setOnClickListener(v -> {
            etSearch.setText(text);
            etSearch.setSelection(text.length());
        });
        chip.setOnCloseIconClickListener(v -> {
            recentSearches.remove(text);
            chipGroupRecent.removeView(chip);
        });
        chipGroupRecent.addView(chip);
    }

    private void clearRecentSearches() {
        recentSearches.clear();
        chipGroupRecent.removeAllViews();
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
