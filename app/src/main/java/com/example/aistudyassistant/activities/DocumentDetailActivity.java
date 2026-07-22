package com.example.aistudyassistant.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.repositories.DocumentRepository;
import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.utils.SharedPrefManager;

import java.io.File;
import java.util.List;

public class DocumentDetailActivity extends AppCompatActivity {

    private TextView tvDocName, tvFileSize, tvUploadDate, tvStatus;
    private ImageButton btnBack, btnDelete;
    private CardView cardSummary, cardQuiz, cardFlashcards, cardChat, cardNotes;
    private LinearLayout layoutProcessing;

    private String documentId;
    private String documentName;
    private String documentPath;
    private String documentType;
    private String projectId, topicId;
    private Document currentDocument;
    private boolean isOpeningDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_detail);

        documentId = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_ID);
        documentName = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_NAME);
        documentPath = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_PATH);
        documentType = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_TYPE);
        if (documentPath == null) {
            // Backward compatibility with callers that used the old extra name.
            documentPath = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_URL);
        }

        initViews();
        displayDocumentInfo();
        setupClickListeners();

        if (documentId != null) {
            SharedPrefManager.getInstance(this).addRecentDocument(documentId);
            loadDocumentDetails();
        }
    }

    private void loadDocumentDetails() {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        DocumentRepository.getInstance().getAllDocuments(userId, new ApiCallback<List<Document>>() {
            @Override
            public void onSuccess(List<Document> result) {
                for (Document doc : result) {
                    if (doc.getId().equals(documentId)) {
                        currentDocument = doc;
                        documentName = doc.getName();
                        documentType = doc.getFileType();
                        projectId = doc.getProjectId();
                        topicId = doc.getTopicId();
                        documentPath = doc.getFilePath();
                        runOnUiThread(() -> {
                            tvDocName.setText(doc.getName());
                            tvFileSize.setText(doc.getFileSizeFormatted());
                            tvStatus.setText(doc.getStatus());
                        });
                        return;
                    }
                }
                runOnUiThread(() -> Toast.makeText(
                        DocumentDetailActivity.this,
                        "Không tìm thấy tài liệu",
                        Toast.LENGTH_SHORT
                ).show());
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(
                        DocumentDetailActivity.this,
                        "Không thể tải thông tin tài liệu: " + errorMessage,
                        Toast.LENGTH_SHORT
                ).show());
            }
        });
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
        cardNotes = findViewById(R.id.card_notes);
        layoutProcessing = findViewById(R.id.layout_processing);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnDelete.setOnClickListener(v -> confirmDelete());
        tvDocName.setOnClickListener(v -> openDocument());

        cardSummary.setOnClickListener(v -> startDocumentFeature(SummaryActivity.class));
        cardQuiz.setOnClickListener(v -> startDocumentFeature(QuizActivity.class));
        cardFlashcards.setOnClickListener(v -> startDocumentFeature(FlashcardsActivity.class));
        cardChat.setOnClickListener(v -> startDocumentFeature(DocumentChatActivity.class));

        cardNotes.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotesActivity.class);
            intent.putExtra("document_id", documentId);
            intent.putExtra("document_name", documentName);
            intent.putExtra("project_id", projectId);
            intent.putExtra("topic_id", topicId);
            startActivity(intent);
        });
    }

    private void startDocumentFeature(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.putExtra(Constants.EXTRA_DOCUMENT_ID, documentId);
        intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, documentName);
        // Truyền private Storage path cho các màn hình AI.
        intent.putExtra(Constants.EXTRA_DOCUMENT_URL, documentPath);
        intent.putExtra(Constants.EXTRA_DOCUMENT_TYPE, documentType);
        intent.putExtra(Constants.EXTRA_TOPIC_ID, topicId);
        startActivity(intent);
    }

    private void displayDocumentInfo() {
        tvDocName.setText(documentName != null ? documentName : "Document");
        tvFileSize.setText("—");
        tvUploadDate.setText("—");
        tvStatus.setText("Đang tải");
    }

    private void openDocument() {
        if (isOpeningDocument) return;
        if (currentDocument == null || currentDocument.getFilePath() == null
                || currentDocument.getFilePath().trim().isEmpty()) {
            Toast.makeText(this, "Thông tin tệp đang được tải, vui lòng thử lại", Toast.LENGTH_SHORT).show();
            return;
        }

        isOpeningDocument = true;
        tvDocName.setEnabled(false);
        Toast.makeText(this, "Đang chuẩn bị tài liệu...", Toast.LENGTH_SHORT).show();

        DocumentRepository.getInstance().downloadDocument(
                currentDocument,
                getCacheDir(),
                new ApiCallback<File>() {
                    @Override
                    public void onSuccess(File cachedFile) {
                        runOnUiThread(() -> {
                            isOpeningDocument = false;
                            tvDocName.setEnabled(true);
                            openCachedFile(cachedFile, currentDocument.getFileType());
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            isOpeningDocument = false;
                            tvDocName.setEnabled(true);
                            Toast.makeText(
                                    DocumentDetailActivity.this,
                                    "Không thể mở tài liệu: " + errorMessage,
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    }
                }
        );
    }

    private void openCachedFile(File file, String fileType) {
        Uri contentUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                file
        );
        Intent viewIntent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(contentUri, getMimeType(fileType))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(Intent.createChooser(viewIntent, "Mở tài liệu bằng"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Thiết bị chưa có ứng dụng hỗ trợ loại tệp này", Toast.LENGTH_LONG).show();
        }
    }

    private String getMimeType(String fileType) {
        if (fileType == null) return "application/octet-stream";
        switch (fileType.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "txt":
                return "text/plain";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default:
                return "application/octet-stream";
        }
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
        // Existing delete flow is outside the document-viewing change.
        Toast.makeText(this, "Document deleted", Toast.LENGTH_SHORT).show();
        finish();
    }
}
