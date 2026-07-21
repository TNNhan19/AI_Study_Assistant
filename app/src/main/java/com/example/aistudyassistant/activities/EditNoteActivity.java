package com.example.aistudyassistant.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.models.Note;
import com.example.aistudyassistant.repositories.NoteRepository;
import com.example.aistudyassistant.utils.SharedPrefManager;

public class EditNoteActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private TextView tvToolbarTitle;
    private NoteRepository repository;
    
    private String noteId, documentId;
    private boolean isPinned;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        repository = NoteRepository.getInstance();
        
        noteId = getIntent().getStringExtra("note_id");
        documentId = getIntent().getStringExtra("document_id");
        isPinned = getIntent().getBooleanExtra("note_pinned", false);

        initViews();
        
        if (noteId != null) {
            etTitle.setText(getIntent().getStringExtra("note_title"));
            etContent.setText(getIntent().getStringExtra("note_content"));
            tvToolbarTitle.setText("Edit Note");
        } else {
            tvToolbarTitle.setText("New Note");
        }
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_note_title);
        etContent = findViewById(R.id.et_note_content);
        tvToolbarTitle = findViewById(R.id.tv_title);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save).setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = SharedPrefManager.getInstance(this).getUserId();
        
        if (noteId == null) {
            // Create
            Note note = new Note();
            note.setUserId(userId);
            note.setDocumentId(documentId);
            note.setTitle(title);
            note.setContent(content);
            note.setPinned(false);

            repository.createNote(note, new ApiCallback<Note>() {
                @Override
                public void onSuccess(Note result) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditNoteActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> Toast.makeText(EditNoteActivity.this, errorMessage, Toast.LENGTH_SHORT).show());
                }
            });
        } else {
            // Update
            Note note = new Note();
            note.setId(noteId);
            note.setTitle(title);
            note.setContent(content);
            note.setPinned(isPinned);

            repository.updateNote(note, new ApiCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditNoteActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> Toast.makeText(EditNoteActivity.this, errorMessage, Toast.LENGTH_SHORT).show());
                }
            });
        }
    }
}
