package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.aistudyassistant.adapters.NoteAdapter;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.models.Note;
import com.example.aistudyassistant.repositories.NoteRepository;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class NotesActivity extends AppCompatActivity {

    private RecyclerView rvNotes;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;
    private TextView tvTitle;

    private NoteAdapter adapter;
    private final List<Note> notes = new ArrayList<>();
    private NoteRepository repository;

    private String documentId;
    private String documentName;
    private String projectId, topicId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        documentId = getIntent().getStringExtra("document_id");
        documentName = getIntent().getStringExtra("document_name");
        projectId = getIntent().getStringExtra("project_id");
        topicId = getIntent().getStringExtra("topic_id");

        repository = NoteRepository.getInstance();
        initViews();
        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    private void initViews() {
        rvNotes = findViewById(R.id.rv_notes);
        layoutEmpty = findViewById(R.id.layout_empty);
        progressBar = findViewById(R.id.progress_bar);
        fabAdd = findViewById(R.id.fab_add_note);
        tvTitle = findViewById(R.id.tv_toolbar_title);

        if (documentName != null) {
            tvTitle.setText("Notes: " + documentName);
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditNoteActivity.class);
            if (documentId != null) intent.putExtra("document_id", documentId);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        adapter = new NoteAdapter(this, notes);
        adapter.setListener(new NoteAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note) {
                Intent intent = new Intent(NotesActivity.this, EditNoteActivity.class);
                intent.putExtra("note_id", note.getId());
                intent.putExtra("note_title", note.getTitle());
                intent.putExtra("note_content", note.getContent());
                intent.putExtra("note_pinned", note.isPinned());
                startActivity(intent);
            }

            @Override
            public void onNoteMoreClick(Note note, View anchor) {
                showNotePopupMenu(note, anchor);
            }
        });
        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        rvNotes.setAdapter(adapter);
    }

    private void loadNotes() {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        setLoading(true);

        ApiCallback<List<Note>> callback = new ApiCallback<List<Note>>() {
            @Override
            public void onSuccess(List<Note> result) {
                runOnUiThread(() -> {
                    setLoading(false);
                    notes.clear();
                    notes.addAll(result);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(NotesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
            }
        };

        if (documentId != null) {
            repository.getNotesByDocument(documentId, callback);
        } else {
            // Lọc theo Topic nếu có, nếu không thì lấy tất cả của User
            repository.getAllNotes(userId, new ApiCallback<List<Note>>() {
                @Override
                public void onSuccess(List<Note> result) {
                    List<Note> filtered = new ArrayList<>();
                    for (Note n : result) {
                        boolean isMatch = true;
                        if (topicId != null && !topicId.equals(n.getTopicId())) isMatch = false;
                        
                        if (isMatch) filtered.add(n);
                    }
                    callback.onSuccess(filtered);
                }

                @Override
                public void onError(String errorMessage) {
                    callback.onError(errorMessage);
                }
            });
        }
    }

    private void showNotePopupMenu(Note note, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(note.isPinned() ? "Unpin" : "Pin");
        popup.getMenu().add("Delete");
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.contains("Pin")) {
                togglePin(note);
            } else {
                confirmDelete(note);
            }
            return true;
        });
        popup.show();
    }

    private void togglePin(Note note) {
        boolean newState = !note.isPinned();
        note.setPinned(newState);
        repository.updateNote(note, new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    // Cập nhật lại giao diện và thông báo
                    String msg = newState ? "Note pinned" : "Note unpinned";
                    Toast.makeText(NotesActivity.this, msg, Toast.LENGTH_SHORT).show();
                    loadNotes(); // Tải lại để sắp xếp lại danh sách
                });
            }
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    // Hoàn tác trạng thái nếu lỗi
                    note.setPinned(!newState);
                    Toast.makeText(NotesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void confirmDelete(Note note) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete", (dialog, which) -> deleteNote(note))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNote(Note note) {
        repository.deleteNote(note.getId(), new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    notes.remove(note);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
            }
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(NotesActivity.this, errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void updateEmptyState() {
        layoutEmpty.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
