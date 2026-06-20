package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.io.InputStream;

public class UploadDocumentActivity extends AppCompatActivity {

    private CardView cardDropZone, cardFileInfo;
    private TextView tvFileName, tvFileSize, tvUploadStatus, tvUploadPercent;
    private LinearLayout layoutProgress;
    private ProgressBar progressUpload;
    private MaterialButton btnUpload, btnRemoveFile;
    private ImageButton btnBack;
    private TextInputEditText etDocTitle;

    private Uri selectedFileUri;
    private String selectedFileName;
    private long selectedFileSize;

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    handleFileSelected(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_document);
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        cardDropZone = findViewById(R.id.card_drop_zone);
        cardFileInfo = findViewById(R.id.card_file_info);
        tvFileName = findViewById(R.id.tv_file_name);
        tvFileSize = findViewById(R.id.tv_file_size);
        tvUploadStatus = findViewById(R.id.tv_upload_status);
        tvUploadPercent = findViewById(R.id.tv_upload_percent);
        layoutProgress = findViewById(R.id.layout_progress);
        progressUpload = findViewById(R.id.progress_upload);
        btnUpload = findViewById(R.id.btn_upload);
        btnBack = findViewById(R.id.btn_back);
        etDocTitle = findViewById(R.id.et_doc_title);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        cardDropZone.setOnClickListener(v ->
                filePickerLauncher.launch(new String[]{"application/pdf", "text/plain",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"}));

        btnUpload.setOnClickListener(v -> uploadDocument());
    }

    private void handleFileSelected(Uri uri) {
        try {
            selectedFileUri = uri;

            // Get file name from URI
            String path = uri.getPath();
            selectedFileName = path != null ? path.substring(path.lastIndexOf('/') + 1) : "document.pdf";

            // Get file size
            InputStream inputStream = getContentResolver().openInputStream(uri);
            selectedFileSize = inputStream != null ? inputStream.available() : 0;
            if (inputStream != null) inputStream.close();

            // Show file info card
            tvFileName.setText(selectedFileName);
            tvFileSize.setText(formatFileSize(selectedFileSize));
            cardDropZone.setVisibility(View.GONE);
            cardFileInfo.setVisibility(View.VISIBLE);

        } catch (IOException e) {
            Toast.makeText(this, "Could not read file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadDocument() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etDocTitle.getText() != null ? etDocTitle.getText().toString().trim() : "";
        if (title.isEmpty()) title = selectedFileName;

        final String docTitle = title;

        setUploading(true);

        new Thread(() -> {
            try {
                // Read file bytes
                InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        setUploading(false);
                        Toast.makeText(this, "Could not read file", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                byte[] fileBytes = inputStream.readAllBytes();
                inputStream.close();

                // TODO: Upload to Supabase Storage
                // String userId = SharedPrefManager.getInstance(this).getUserId();
                // String storagePath = userId + "/" + System.currentTimeMillis() + "_" + selectedFileName;
                // String uploadResult = SupabaseClient.getInstance().uploadFile(
                //     Constants.STORAGE_BUCKET, storagePath, fileBytes, "application/pdf");
                //
                // String fileUrl = SupabaseClient.getInstance().getFilePublicUrl(
                //     Constants.STORAGE_BUCKET, storagePath);
                //
                // Then insert document record into database
                // Then call GeminiClient to process the document

                runOnUiThread(() -> {
                    setUploading(false);
                    Toast.makeText(this,
                            "Document uploaded! AI processing started.",
                            Toast.LENGTH_SHORT).show();
                    finish();
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    setUploading(false);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void setUploading(boolean uploading) {
        layoutProgress.setVisibility(uploading ? View.VISIBLE : View.GONE);
        btnUpload.setEnabled(!uploading);
        btnUpload.setText(uploading ? "Uploading..." : "Upload & Process");
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        else return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
