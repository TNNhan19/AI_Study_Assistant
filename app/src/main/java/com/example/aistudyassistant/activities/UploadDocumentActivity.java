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

import com.example.aistudyassistant.models.Project;
import com.example.aistudyassistant.models.Topic;
import com.example.aistudyassistant.utils.SharedPrefManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class UploadDocumentActivity extends AppCompatActivity {

    private CardView cardDropZone, cardFileInfo;
    private TextView tvFileName, tvFileSize, tvUploadStatus, tvUploadPercent;
    private LinearLayout layoutProgress;
    private ProgressBar progressUpload;
    private MaterialButton btnUpload;
    private ImageButton btnBack, btnRemoveFile;
    private TextInputEditText etDocTitle;
    private android.widget.AutoCompleteTextView actvProject, actvTopic;

    private Uri selectedFileUri;
    private String selectedFileName;
    private long selectedFileSize;
    
    private List<Project> projectList = new ArrayList<>();
    private List<Topic> topicList = new ArrayList<>();
    private String selectedProjectId = null;
    private String selectedTopicId = null;

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
        btnRemoveFile = findViewById(R.id.btn_remove_file);
        btnBack = findViewById(R.id.btn_back);
        etDocTitle = findViewById(R.id.et_doc_title);
        actvProject = findViewById(R.id.actv_project);
        actvTopic = findViewById(R.id.actv_topic);
        
        loadProjectsForDropdown();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        cardDropZone.setOnClickListener(v ->
                filePickerLauncher.launch(new String[]{"application/pdf", "text/plain",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"}));

        btnUpload.setOnClickListener(v -> uploadDocument());

        btnRemoveFile.setOnClickListener(v -> {
            selectedFileUri = null;
            selectedFileName = null;
            selectedFileSize = 0;
            cardFileInfo.setVisibility(View.GONE);
            cardDropZone.setVisibility(View.VISIBLE);
            etDocTitle.setText("");
        });
    }

    private void handleFileSelected(Uri uri) {
        selectedFileUri = uri;
        String fileName = "document";
        long fileSize = 0;

        // Use ContentResolver to get file name and size
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (nameIndex != -1) fileName = cursor.getString(nameIndex);
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        selectedFileName = fileName;
        selectedFileSize = fileSize;

        // Show file info card
        tvFileName.setText(selectedFileName);
        tvFileSize.setText(formatFileSize(selectedFileSize));
        etDocTitle.setText(selectedFileName);
        cardDropZone.setVisibility(View.GONE);
        cardFileInfo.setVisibility(View.VISIBLE);
    }

    private void loadProjectsForDropdown() {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        if (userId.isEmpty()) return;
        
        com.example.aistudyassistant.repositories.ProjectRepository.getInstance().getAllProjects(userId, new com.example.aistudyassistant.api.ApiCallback<List<Project>>() {
            @Override
            public void onSuccess(List<Project> result) {
                projectList = result;
                List<String> names = new ArrayList<>();
                names.add("None");
                for (Project p : result) names.add(p.getName());
                
                runOnUiThread(() -> {
                    android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(UploadDocumentActivity.this, android.R.layout.simple_dropdown_item_1line, names);
                    actvProject.setAdapter(adapter);
                    actvProject.setOnItemClickListener((parent, view, position, id) -> {
                        if (position == 0) {
                            selectedProjectId = null;
                            selectedTopicId = null;
                            topicList.clear();
                            actvTopic.setAdapter(null);
                            actvTopic.setText("");
                        } else {
                            selectedProjectId = projectList.get(position - 1).getId();
                            selectedTopicId = null;
                            actvTopic.setText("");
                            loadTopicsForDropdown(selectedProjectId);
                        }
                    });
                });
            }
            @Override public void onError(String errorMessage) {}
        });
    }

    private void loadTopicsForDropdown(String projectId) {
        com.example.aistudyassistant.repositories.TopicRepository.getInstance().getTopicsByProject(projectId, new com.example.aistudyassistant.api.ApiCallback<List<Topic>>() {
            @Override
            public void onSuccess(List<Topic> result) {
                topicList = result;
                List<String> names = new ArrayList<>();
                names.add("None");
                for (Topic t : result) names.add(t.getName());
                
                runOnUiThread(() -> {
                    android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(UploadDocumentActivity.this, android.R.layout.simple_dropdown_item_1line, names);
                    actvTopic.setAdapter(adapter);
                    actvTopic.setOnItemClickListener((parent, view, position, id) -> {
                        selectedTopicId = (position == 0) ? null : topicList.get(position - 1).getId();
                    });
                });
            }
            @Override public void onError(String errorMessage) {}
        });
    }

    private void uploadDocument() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etDocTitle.getText() != null ? etDocTitle.getText().toString().trim() : "";
        if (title.isEmpty()) title = selectedFileName;

        final String finalTitle = title;
        final String userId = com.example.aistudyassistant.utils.SharedPrefManager.getInstance(this).getUserId();

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

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

                // Prepare Document model
                com.example.aistudyassistant.models.Document doc = new com.example.aistudyassistant.models.Document();
                doc.setUserId(userId);
                doc.setName(finalTitle);
                doc.setFileSize(selectedFileSize);
                doc.setProjectId(selectedProjectId);
                doc.setTopicId(selectedTopicId);
                
                // Determine file type from extension
                String extension = "";
                int i = selectedFileName.lastIndexOf('.');
                if (i > 0) extension = selectedFileName.substring(i + 1).toLowerCase();
                doc.setFileType(extension);

                // Define storage path: userId/timestamp_filename
                String storagePath = userId + "/" + System.currentTimeMillis() + "_" + selectedFileName;
                doc.setFilePath(storagePath);

                // Call Repository to upload
                com.example.aistudyassistant.repositories.DocumentRepository.getInstance().uploadDocument(doc, fileBytes, new com.example.aistudyassistant.api.ApiCallback<com.example.aistudyassistant.models.Document>() {
                    @Override
                    public void onSuccess(com.example.aistudyassistant.models.Document result) {
                        runOnUiThread(() -> {
                            setUploading(false);
                            Toast.makeText(UploadDocumentActivity.this, "Document uploaded successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            setUploading(false);
                            Toast.makeText(UploadDocumentActivity.this, "Upload failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        });
                    }
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    setUploading(false);
                    Toast.makeText(this, "Error reading file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
