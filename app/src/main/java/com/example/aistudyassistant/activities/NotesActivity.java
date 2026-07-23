package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
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
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.models.Note;
import com.example.aistudyassistant.models.Project;
import com.example.aistudyassistant.models.Topic;
import com.example.aistudyassistant.repositories.DocumentRepository;
import com.example.aistudyassistant.repositories.NoteRepository;
import com.example.aistudyassistant.repositories.ProjectRepository;
import com.example.aistudyassistant.repositories.TopicRepository;
import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.utils.NoteSearchUtils;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class NotesActivity extends AppCompatActivity {

    private static final int MENU_TOGGLE_PIN = 1;
    private static final int MENU_DELETE = 2;

    private RecyclerView rvNotes;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;
    private TextView tvTitle;
    private TextView tvFilterSummary;
    private TextInputEditText etSearchNotes;
    private ImageButton btnFilterNotes;

    private NoteAdapter adapter;
    private final List<Note> notes = new ArrayList<>();
    private final List<Note> allNotes = new ArrayList<>();
    private final List<Document> documents = new ArrayList<>();
    private final List<Project> projects = new ArrayList<>();
    private final List<Topic> topics = new ArrayList<>();
    private final Map<String, Document> documentsById = new HashMap<>();
    private final Map<String, Project> projectsById = new HashMap<>();
    private final Map<String, Topic> topicsById = new HashMap<>();
    private NoteRepository repository;

    private String documentId;
    private String documentName;
    private String selectedProjectId;
    private String selectedTopicId;
    private int loadVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        documentId = getIntent().getStringExtra("document_id");
        documentName = getIntent().getStringExtra("document_name");
        selectedProjectId = getIntent().getStringExtra(Constants.EXTRA_PROJECT_ID);
        selectedTopicId = getIntent().getStringExtra(Constants.EXTRA_TOPIC_ID);

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
        tvFilterSummary = findViewById(R.id.tv_note_filter_summary);
        etSearchNotes = findViewById(R.id.et_search_notes);
        btnFilterNotes = findViewById(R.id.btn_filter_notes);

        if (documentName != null) {
            tvTitle.setText("Notes: " + documentName);
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        etSearchNotes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence value, int start, int before, int count) {
                applyNoteFilters();
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });
        btnFilterNotes.setOnClickListener(v -> showProjectFilterDialog());
        
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditNoteActivity.class);
            if (documentId != null) intent.putExtra("document_id", documentId);
            if (hasValue(selectedTopicId)) {
                intent.putExtra(Constants.EXTRA_TOPIC_ID, selectedTopicId);
            }
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
        if (!hasValue(userId)) {
            allNotes.clear();
            applyNoteFilters();
            return;
        }

        setLoading(true);
        int requestVersion = ++loadVersion;
        AtomicInteger pendingLoads = new AtomicInteger(4);
        AtomicReference<String> firstError = new AtomicReference<>();
        AtomicReference<List<Note>> loadedNotes =
                new AtomicReference<>(new ArrayList<>());
        AtomicReference<List<Document>> loadedDocuments =
                new AtomicReference<>(new ArrayList<>());
        AtomicReference<List<Project>> loadedProjects =
                new AtomicReference<>(new ArrayList<>());
        AtomicReference<List<Topic>> loadedTopics =
                new AtomicReference<>(new ArrayList<>());

        Runnable completePart = () -> {
            if (pendingLoads.decrementAndGet() != 0) return;
            runOnUiThread(() -> {
                if (requestVersion != loadVersion) return;

                setLoading(false);
                allNotes.clear();
                allNotes.addAll(loadedNotes.get());
                documents.clear();
                documents.addAll(loadedDocuments.get());
                projects.clear();
                projects.addAll(loadedProjects.get());
                topics.clear();
                topics.addAll(loadedTopics.get());

                rebuildReferenceMaps();
                enrichNoteContext();
                applyNoteFilters();

                if (firstError.get() != null) {
                    Toast.makeText(
                            NotesActivity.this,
                            firstError.get(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        };

        repository.getAllNotes(userId, loadInto(
                loadedNotes, firstError, completePart));
        DocumentRepository.getInstance().getAllDocuments(userId, loadInto(
                loadedDocuments, firstError, completePart));
        ProjectRepository.getInstance().getAllProjects(userId, loadInto(
                loadedProjects, firstError, completePart));
        TopicRepository.getInstance().getAllTopics(userId, loadInto(
                loadedTopics, firstError, completePart));
    }

    private <T> ApiCallback<List<T>> loadInto(
            AtomicReference<List<T>> target,
            AtomicReference<String> firstError,
            Runnable onComplete) {
        return new ApiCallback<List<T>>() {
            @Override
            public void onSuccess(List<T> result) {
                target.set(result == null ? new ArrayList<>() : result);
                onComplete.run();
            }

            @Override
            public void onError(String errorMessage) {
                target.set(new ArrayList<>());
                firstError.compareAndSet(
                        null,
                        hasValue(errorMessage)
                                ? errorMessage
                                : "Some note filters could not be loaded");
                onComplete.run();
            }
        };
    }

    private void rebuildReferenceMaps() {
        documentsById.clear();
        projectsById.clear();
        topicsById.clear();

        for (Document document : documents) {
            documentsById.put(document.getId(), document);
        }
        for (Project project : projects) {
            projectsById.put(project.getId(), project);
        }
        for (Topic topic : topics) {
            topicsById.put(topic.getId(), topic);
        }
    }

    private void enrichNoteContext() {
        for (Note note : allNotes) {
            Document document = documentsById.get(note.getDocumentId());
            String effectiveTopicId = getEffectiveTopicId(note);
            String effectiveProjectId = getEffectiveProjectId(note);
            Topic topic = topicsById.get(effectiveTopicId);
            Project project = projectsById.get(effectiveProjectId);

            note.setDocumentName(document == null ? null : document.getName());
            note.setTopicName(topic == null ? null : topic.getName());
            note.setProjectName(project == null ? null : project.getName());
        }
    }

    private String getEffectiveTopicId(Note note) {
        if (hasValue(note.getTopicId())) return note.getTopicId();
        Document document = documentsById.get(note.getDocumentId());
        return document == null ? null : document.getTopicId();
    }

    private String getEffectiveProjectId(Note note) {
        Document document = documentsById.get(note.getDocumentId());
        if (document != null && hasValue(document.getProjectId())) {
            return document.getProjectId();
        }

        Topic topic = topicsById.get(getEffectiveTopicId(note));
        return topic == null ? null : topic.getProjectId();
    }

    private void applyNoteFilters() {
        if (adapter == null) return;
        String query = etSearchNotes == null || etSearchNotes.getText() == null
                ? "" : etSearchNotes.getText().toString();

        notes.clear();
        for (Note note : allNotes) {
            if (hasValue(documentId)
                    && !documentId.equals(note.getDocumentId())) {
                continue;
            }
            if (hasValue(selectedProjectId)
                    && !selectedProjectId.equals(getEffectiveProjectId(note))) {
                continue;
            }
            if (hasValue(selectedTopicId)
                    && !selectedTopicId.equals(getEffectiveTopicId(note))) {
                continue;
            }
            if (!NoteSearchUtils.matches(
                    query,
                    note.getTitle(),
                    note.getContent(),
                    note.getDocumentName())) {
                continue;
            }
            notes.add(note);
        }

        notes.sort((first, second) -> {
            if (first.isPinned() != second.isPinned()) {
                return first.isPinned() ? -1 : 1;
            }
            String firstUpdated = hasValue(first.getUpdatedAt())
                    ? first.getUpdatedAt() : first.getCreatedAt();
            String secondUpdated = hasValue(second.getUpdatedAt())
                    ? second.getUpdatedAt() : second.getCreatedAt();
            return safeString(secondUpdated).compareTo(safeString(firstUpdated));
        });

        adapter.notifyDataSetChanged();
        updateFilterSummary();
        updateEmptyState();
    }

    private void updateFilterSummary() {
        String projectLabel = getString(R.string.all_projects);
        Project selectedProject = projectsById.get(selectedProjectId);
        if (selectedProject != null) {
            projectLabel = selectedProject.getName();
        }

        String topicLabel = getString(R.string.all_topics);
        Topic selectedTopic = topicsById.get(selectedTopicId);
        if (selectedTopic != null) {
            topicLabel = selectedTopic.getName();
        }
        tvFilterSummary.setText(getString(
                R.string.note_filter_summary, projectLabel, topicLabel));
    }

    private void showProjectFilterDialog() {
        List<Project> availableProjects = new ArrayList<>(projects);
        availableProjects.sort(Comparator.comparing(
                project -> safeString(project.getName()),
                String.CASE_INSENSITIVE_ORDER));

        String[] labels = new String[availableProjects.size() + 1];
        labels[0] = getString(R.string.all_projects);
        int checkedIndex = 0;
        for (int i = 0; i < availableProjects.size(); i++) {
            Project project = availableProjects.get(i);
            labels[i + 1] = project.getName();
            if (project.getId().equals(selectedProjectId)) {
                checkedIndex = i + 1;
            }
        }

        int[] pendingSelection = {checkedIndex};
        new AlertDialog.Builder(this)
                .setTitle(R.string.select_project)
                .setSingleChoiceItems(
                        labels,
                        checkedIndex,
                        (dialog, which) -> pendingSelection[0] = which)
                .setPositiveButton(R.string.next, (dialog, which) -> {
                    selectedProjectId = pendingSelection[0] == 0
                            ? null
                            : availableProjects.get(
                                    pendingSelection[0] - 1).getId();
                    if (hasValue(selectedTopicId)) {
                        Topic selectedTopic = topicsById.get(selectedTopicId);
                        if (selectedTopic == null
                                || (hasValue(selectedProjectId)
                                && !selectedProjectId.equals(
                                selectedTopic.getProjectId()))) {
                            selectedTopicId = null;
                        }
                    }
                    showTopicFilterDialog();
                })
                .setNeutralButton(R.string.clear_filter, (dialog, which) -> {
                    selectedProjectId = null;
                    selectedTopicId = null;
                    applyNoteFilters();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTopicFilterDialog() {
        List<Topic> availableTopics = new ArrayList<>();
        for (Topic topic : topics) {
            if (!hasValue(selectedProjectId)
                    || selectedProjectId.equals(topic.getProjectId())) {
                availableTopics.add(topic);
            }
        }
        availableTopics.sort(Comparator.comparing(
                topic -> safeString(topic.getName()),
                String.CASE_INSENSITIVE_ORDER));

        String[] labels = new String[availableTopics.size() + 1];
        labels[0] = getString(R.string.all_topics);
        int checkedIndex = 0;
        for (int i = 0; i < availableTopics.size(); i++) {
            Topic topic = availableTopics.get(i);
            labels[i + 1] = topic.getName();
            if (topic.getId().equals(selectedTopicId)) {
                checkedIndex = i + 1;
            }
        }

        int[] pendingSelection = {checkedIndex};
        new AlertDialog.Builder(this)
                .setTitle(R.string.select_topic)
                .setSingleChoiceItems(
                        labels,
                        checkedIndex,
                        (dialog, which) -> pendingSelection[0] = which)
                .setPositiveButton(R.string.apply, (dialog, which) -> {
                    selectedTopicId = pendingSelection[0] == 0
                            ? null
                            : availableTopics.get(
                                    pendingSelection[0] - 1).getId();
                    applyNoteFilters();
                })
                .setNeutralButton(R.string.clear_filter, (dialog, which) -> {
                    selectedProjectId = null;
                    selectedTopicId = null;
                    applyNoteFilters();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private void showNotePopupMenu(Note note, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(
                Menu.NONE,
                MENU_TOGGLE_PIN,
                Menu.NONE,
                note.isPinned() ? "Unpin" : "Pin");
        popup.getMenu().add(
                Menu.NONE,
                MENU_DELETE,
                Menu.NONE,
                "Delete");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_TOGGLE_PIN) {
                togglePin(note);
                return true;
            }
            if (item.getItemId() == MENU_DELETE) {
                confirmDelete(note);
                return true;
            }
            return false;
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
                    allNotes.remove(note);
                    notes.remove(note);
                    applyNoteFilters();
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
