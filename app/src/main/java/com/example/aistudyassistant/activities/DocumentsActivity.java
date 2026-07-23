package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.adapters.DocumentAdapter;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.utils.Constants;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DocumentsActivity extends AppCompatActivity {

    private RecyclerView rvDocuments;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private FloatingActionButton fabUpload;
    private TextInputEditText etSearch;
    private BottomNavigationView bottomNavigation;
    private com.google.android.material.chip.ChipGroup chipGroupFilters;

    private DocumentAdapter adapter;
    private final List<Document> allDocuments = new ArrayList<>();
    private final List<Document> filteredDocuments = new ArrayList<>();
    private int selectedSort = 1;
    
    private String projectId, topicId, topicName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);

        projectId = getIntent().getStringExtra(Constants.EXTRA_PROJECT_ID);
        topicId = getIntent().getStringExtra(Constants.EXTRA_TOPIC_ID);
        topicName = getIntent().getStringExtra("topic_name");

        initViews();
        setupRecyclerView();
        setupSearch();
        setupBottomNavigation();
        loadDocuments();
    }

    private void initViews() {
        rvDocuments = findViewById(R.id.rv_documents);
        layoutEmpty = findViewById(R.id.layout_empty);
        progressBar = findViewById(R.id.progress_bar);
        fabUpload = findViewById(R.id.fab_upload);
        etSearch = findViewById(R.id.et_search);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        chipGroupFilters = findViewById(R.id.chip_group_filters);

        TextView toolbarTitle = findViewById(R.id.tv_toolbar_title);
        toolbarTitle.setText(hasValue(topicName) ? topicName : "My Documents");

        fabUpload.setOnClickListener(v -> {
            Intent intent = new Intent(this, UploadDocumentActivity.class);
            if (hasValue(projectId)) intent.putExtra(Constants.EXTRA_PROJECT_ID, projectId);
            if (hasValue(topicId)) intent.putExtra(Constants.EXTRA_TOPIC_ID, topicId);
            startActivity(intent);
        });
        
        findViewById(R.id.btn_filter).setOnClickListener(v -> showFilterOptions());
        
        chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> applyFilters());
    }

    private void showFilterOptions() {
        String[] options = {"Sort by Name", "Sort by Date (Newest)", "Sort by Date (Oldest)", "Sort by Size"};
        new AlertDialog.Builder(this)
                .setTitle("Sort Documents")
                .setItems(options, (dialog, which) -> {
                    selectedSort = which;
                    applyFilters();
                })
                .show();
    }

    private void setupRecyclerView() {
        adapter = new DocumentAdapter(this, filteredDocuments);
        adapter.setListener(new DocumentAdapter.OnDocumentClickListener() {
            @Override
            public void onDocumentClick(Document document) {
                // Track as recently viewed
                com.example.aistudyassistant.utils.SharedPrefManager.getInstance(DocumentsActivity.this).addRecentDocument(document.getId());
                
                Intent intent = new Intent(DocumentsActivity.this, DocumentDetailActivity.class);
                intent.putExtra(Constants.EXTRA_DOCUMENT_ID, document.getId());
                intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, document.getName());
                intent.putExtra(Constants.EXTRA_DOCUMENT_PATH, document.getFilePath());
                intent.putExtra(Constants.EXTRA_DOCUMENT_TYPE, document.getFileType());
                startActivity(intent);
            }

            @Override
            public void onDocumentMoreClick(Document document, View anchorView) {
                showDocumentPopupMenu(document, anchorView);
            }

            @Override
            public void onFavoriteClick(Document document) {
                toggleFavorite(document);
            }
        });
        rvDocuments.setLayoutManager(new LinearLayoutManager(this));
        rvDocuments.setAdapter(adapter);
    }

    private void toggleFavorite(Document document) {
        boolean newState = !document.isFavorite();
        com.example.aistudyassistant.repositories.DocumentRepository.getInstance().toggleFavorite(document.getId(), newState, new com.example.aistudyassistant.api.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    document.setFavorite(newState);
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(DocumentsActivity.this, errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void applyFilters() {
        String query = etSearch.getText() != null ? etSearch.getText().toString().toLowerCase() : "";
        int checkedChipId = chipGroupFilters.getCheckedChipId();
        
        filteredDocuments.clear();
        for (Document doc : allDocuments) {
            boolean matchesSearch = doc.getName().toLowerCase().contains(query);
            boolean matchesChip = true;
            
            if (checkedChipId == R.id.chip_favorites) matchesChip = doc.isFavorite();
            else if (checkedChipId == R.id.chip_pdf) matchesChip = "pdf".equalsIgnoreCase(doc.getFileType());
            else if (checkedChipId == R.id.chip_docx) matchesChip = "docx".equalsIgnoreCase(doc.getFileType());
            else if (checkedChipId == R.id.chip_txt) matchesChip = "txt".equalsIgnoreCase(doc.getFileType());
            
            if (matchesSearch && matchesChip) {
                filteredDocuments.add(doc);
            }
        }

        // Ưu tiên đẩy Đánh dấu sao lên đầu
        java.util.Collections.sort(filteredDocuments, (d1, d2) -> {
            if (d1.isFavorite() && !d2.isFavorite()) return -1;
            if (!d1.isFavorite() && d2.isFavorite()) return 1;
            return getSelectedComparator().compare(d1, d2);
        });

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private Comparator<Document> getSelectedComparator() {
        switch (selectedSort) {
            case 0:
                return (d1, d2) -> d1.getName().compareToIgnoreCase(d2.getName());
            case 2:
                return (d1, d2) -> Long.compare(d1.getCreatedAt(), d2.getCreatedAt());
            case 3:
                return (d1, d2) -> Long.compare(d2.getFileSize(), d1.getFileSize());
            case 1:
            default:
                return (d1, d2) -> Long.compare(d2.getCreatedAt(), d1.getCreatedAt());
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterDocuments(String query) {
        applyFilters();
    }

    private void showDocumentPopupMenu(Document document, View anchorView) {
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenu().add("Open");
        popup.getMenu().add("Delete");
        popup.setOnMenuItemClickListener(item -> {
            if ("Delete".equals(item.getTitle().toString())) {
                confirmDelete(document);
                return true;
            } else if ("Open".equals(item.getTitle().toString())) {
                Intent intent = new Intent(this, DocumentDetailActivity.class);
                intent.putExtra(Constants.EXTRA_DOCUMENT_ID, document.getId());
                intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, document.getName());
                intent.putExtra(Constants.EXTRA_DOCUMENT_PATH, document.getFilePath());
                intent.putExtra(Constants.EXTRA_DOCUMENT_TYPE, document.getFileType());
                startActivity(intent);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmDelete(Document document) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Document")
                .setMessage("Are you sure you want to delete \"" + document.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteDocument(document))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteDocument(Document document) {
        setLoading(true);
        com.example.aistudyassistant.repositories.DocumentRepository.getInstance().deleteDocument(document, new com.example.aistudyassistant.api.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    setLoading(false);
                    allDocuments.remove(document);
                    filteredDocuments.remove(document);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    Toast.makeText(DocumentsActivity.this, "Document deleted", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(DocumentsActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_documents);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_documents) return true;
            else if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_schedule) {
                startActivity(new Intent(this, ScheduleActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, AIChatActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void loadDocuments() {
        String userId = com.example.aistudyassistant.utils.SharedPrefManager.getInstance(this).getUserId();
        if (userId == null) return;

        setLoading(true);
        com.example.aistudyassistant.repositories.DocumentRepository.getInstance().getAllDocuments(userId, new com.example.aistudyassistant.api.ApiCallback<List<Document>>() {
            @Override
            public void onSuccess(List<Document> result) {
                runOnUiThread(() -> {
                    setLoading(false);
                    allDocuments.clear();
                    
                    // Topic is the most specific scope. Only use the project
                    // scope when this screen was not opened from a topic.
                    for (Document doc : result) {
                        boolean match;
                        if (hasValue(topicId)) {
                            match = topicId.equals(doc.getTopicId());
                        } else if (hasValue(projectId)) {
                            match = projectId.equals(doc.getProjectId());
                        } else {
                            match = true;
                        }

                        if (match) allDocuments.add(doc);
                    }
                    
                    applyFilters();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(DocumentsActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        rvDocuments.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState() {
        layoutEmpty.setVisibility(filteredDocuments.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDocuments();
    }
}
