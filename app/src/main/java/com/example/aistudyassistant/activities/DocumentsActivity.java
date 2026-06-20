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
import java.util.List;

public class DocumentsActivity extends AppCompatActivity {

    private RecyclerView rvDocuments;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private FloatingActionButton fabUpload;
    private TextInputEditText etSearch;
    private BottomNavigationView bottomNavigation;

    private DocumentAdapter adapter;
    private final List<Document> allDocuments = new ArrayList<>();
    private final List<Document> filteredDocuments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);
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

        fabUpload.setOnClickListener(v ->
                startActivity(new Intent(this, UploadDocumentActivity.class)));
    }

    private void setupRecyclerView() {
        adapter = new DocumentAdapter(this, filteredDocuments);
        adapter.setListener(new DocumentAdapter.OnDocumentClickListener() {
            @Override
            public void onDocumentClick(Document document) {
                Intent intent = new Intent(DocumentsActivity.this, DocumentDetailActivity.class);
                intent.putExtra(Constants.EXTRA_DOCUMENT_ID, document.getId());
                intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, document.getName());
                intent.putExtra(Constants.EXTRA_DOCUMENT_URL, document.getFileUrl());
                startActivity(intent);
            }

            @Override
            public void onDocumentMoreClick(Document document, View anchorView) {
                showDocumentPopupMenu(document, anchorView);
            }
        });
        rvDocuments.setLayoutManager(new LinearLayoutManager(this));
        rvDocuments.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDocuments(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterDocuments(String query) {
        filteredDocuments.clear();
        if (query.isEmpty()) {
            filteredDocuments.addAll(allDocuments);
        } else {
            String lower = query.toLowerCase();
            for (Document doc : allDocuments) {
                if (doc.getName().toLowerCase().contains(lower)) {
                    filteredDocuments.add(doc);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
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
        // TODO: Call SupabaseClient to delete document
        allDocuments.remove(document);
        filteredDocuments.remove(document);
        adapter.notifyDataSetChanged();
        updateEmptyState();
        Toast.makeText(this, "Document deleted", Toast.LENGTH_SHORT).show();
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
                startActivity(new Intent(this, ChatActivity.class));
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
        setLoading(true);
        // TODO: Load documents from Supabase
        // String userId = SharedPrefManager.getInstance(this).getUserId();
        // new Thread(() -> {
        //     String response = SupabaseClient.getInstance().getFromTable(
        //         Constants.TABLE_DOCUMENTS, "user_id=eq." + userId + "&order=created_at.desc");
        //     runOnUiThread(() -> {
        //         setLoading(false);
        //         parseAndDisplayDocuments(response);
        //     });
        // }).start();

        // For now, show empty state
        setLoading(false);
        updateEmptyState();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        rvDocuments.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState() {
        layoutEmpty.setVisibility(filteredDocuments.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDocuments();
    }
}
