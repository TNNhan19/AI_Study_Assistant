package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.utils.SharedPrefManager;

public class DocumentDetailActivity extends AppCompatActivity {

    private TextView tvDocName, tvFileSize, tvUploadDate, tvStatus;
    private ImageButton btnBack, btnDelete;
    private CardView cardSummary, cardQuiz, cardFlashcards, cardChat;
    private LinearLayout layoutProcessing;

    private String documentId;
    private String documentName;
    private String documentUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_detail);

        documentId = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_ID);
        documentName = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_NAME);
        documentUrl = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_URL);

        initViews();
        setupClickListeners();
        displayDocumentInfo();
    }

    private void initViews() {
        tvDocName = findViewById(R.id.tv_doc_name);
        tvFileSize = findViewById(R.id.tv_file_size);
        tvUploadDate = findViewById(R.id.tv_upload_date);
        tvStatus = findViewById(R.id.tv_status);
        btnBack = findViewById(R.id.btn_back);
        btnDelete = findViewById(R.id.btn_delete);
        cardSummary = findViewById(R.id.card_summary);
        cardQuiz = findViewById(R.id.card_quiz);
        cardFlashcards = findViewById(R.id.card_flashcards);
        cardChat = findViewById(R.id.card_chat);
        layoutProcessing = findViewById(R.id.layout_processing);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnDelete.setOnClickListener(v -> confirmDelete());

        cardSummary.setOnClickListener(v -> {
            Intent intent = new Intent(this, SummaryActivity.class);
            intent.putExtra(Constants.EXTRA_DOCUMENT_ID, documentId);
            intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, documentName);
            intent.putExtra(Constants.EXTRA_DOCUMENT_URL, documentUrl);
            startActivity(intent);
        });

        cardQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra(Constants.EXTRA_DOCUMENT_ID, documentId);
            intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, documentName);
            intent.putExtra(Constants.EXTRA_DOCUMENT_URL, documentUrl);
            startActivity(intent);
        });

        cardFlashcards.setOnClickListener(v -> {
            Intent intent = new Intent(this, FlashcardsActivity.class);
            intent.putExtra(Constants.EXTRA_DOCUMENT_ID, documentId);
            intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, documentName);
            intent.putExtra(Constants.EXTRA_DOCUMENT_URL, documentUrl);
            startActivity(intent);
        });

        cardChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, DocumentChatActivity.class);
            intent.putExtra(Constants.EXTRA_DOCUMENT_ID, documentId);
            intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, documentName);
            intent.putExtra(Constants.EXTRA_DOCUMENT_URL, documentUrl);
            startActivity(intent);
        });
    }

    private void displayDocumentInfo() {
        tvDocName.setText(documentName != null ? documentName : "Document");
        // TODO: Load actual file size, date and status from Supabase
        tvFileSize.setText("—");
        tvUploadDate.setText("—");
        tvStatus.setText("Completed");
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Document")
                .setMessage("Are you sure you want to delete this document? This will also delete all AI-generated content (summary, quiz, flashcards).")
                .setPositiveButton("Delete", (dialog, which) -> deleteDocument())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteDocument() {
        // TODO: Call SupabaseClient to delete document and related data
        Toast.makeText(this, "Document deleted", Toast.LENGTH_SHORT).show();
        finish();
    }
}
