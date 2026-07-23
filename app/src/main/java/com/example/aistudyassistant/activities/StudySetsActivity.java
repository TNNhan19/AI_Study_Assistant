package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.adapters.StudySetAdapter;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.models.StudySet;
import com.example.aistudyassistant.repositories.AIContentRepository;
import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudySetsActivity extends AppCompatActivity {

    public static final String EXTRA_CONTENT_TYPE = "content_type";
    public static final String TYPE_QUIZ = "QUIZ";
    public static final String TYPE_FLASHCARD = "FLASHCARD";

    private boolean quizMode;
    private StudySetAdapter adapter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyState;
    private TextView emptyTitle;
    private TextView emptyMessage;
    private TextInputEditText searchInput;
    private final List<StudySet> allStudySets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_sets);

        quizMode = TYPE_QUIZ.equals(
                getIntent().getStringExtra(EXTRA_CONTENT_TYPE));
        setupViews();
        loadStudySets();
    }

    private void setupViews() {
        ImageButton backButton = findViewById(R.id.btn_back);
        TextView title = findViewById(R.id.tv_study_sets_title);
        TextView subtitle = findViewById(R.id.tv_study_sets_subtitle);
        MaterialButton createButton = findViewById(R.id.btn_create_study_set);
        recyclerView = findViewById(R.id.rv_study_sets);
        progressBar = findViewById(R.id.progress_bar);
        emptyState = findViewById(R.id.layout_empty);
        emptyTitle = findViewById(R.id.tv_empty_title);
        emptyMessage = findViewById(R.id.tv_empty_message);
        TextInputLayout searchLayout = findViewById(R.id.til_search_quizzes);
        searchInput = findViewById(R.id.et_search_quizzes);

        title.setText(quizMode ? R.string.my_quizzes : R.string.my_flashcards);
        subtitle.setText(quizMode
                ? R.string.quiz_library_subtitle
                : R.string.flashcard_library_subtitle);
        emptyTitle.setText(quizMode
                ? R.string.no_saved_quizzes
                : R.string.no_saved_flashcards);
        emptyMessage.setText(quizMode
                ? R.string.no_saved_quizzes_message
                : R.string.no_saved_flashcards_message);

        searchLayout.setVisibility(quizMode ? View.VISIBLE : View.GONE);
        adapter = new StudySetAdapter(quizMode, new StudySetAdapter.Listener() {
            @Override
            public void onStudySetClick(StudySet studySet) {
                openStudySet(studySet);
            }

            @Override
            public void onStudySetPinClick(StudySet studySet) {
                updatePinned(studySet, !studySet.isPinned());
            }

            @Override
            public void onStudySetMoreClick(View anchor, StudySet studySet) {
                showQuizMenu(anchor, studySet);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence value, int start, int before, int count) {
                filterStudySets(value == null ? "" : value.toString());
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });

        backButton.setOnClickListener(v -> finish());
        createButton.setOnClickListener(v ->
                startActivity(new Intent(this, DocumentsActivity.class)));
    }

    private void loadStudySets() {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            showError(getString(R.string.session_required));
            return;
        }

        setLoading(true);
        AIContentRepository.getInstance().getStudySets(
                userId,
                quizMode,
                new ApiCallback<List<StudySet>>() {
                    @Override
                    public void onSuccess(List<StudySet> result) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            allStudySets.clear();
                            allStudySets.addAll(result);
                            filterStudySets(searchInput.getText() == null
                                    ? ""
                                    : searchInput.getText().toString());
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            showError(errorMessage);
                        });
                    }
                });
    }

    private void filterStudySets(String query) {
        String normalizedQuery = normalize(query);
        List<StudySet> filtered = new ArrayList<>();
        for (StudySet studySet : allStudySets) {
            if (normalizedQuery.isEmpty()
                    || normalize(studySet.getTitle()).contains(normalizedQuery)
                    || normalize(studySet.getDocumentName()).contains(normalizedQuery)
                    || normalize(studySet.getProjectName()).contains(normalizedQuery)
                    || normalize(studySet.getTopicName()).contains(normalizedQuery)) {
                filtered.add(studySet);
            }
        }

        adapter.submitList(filtered);
        boolean empty = filtered.isEmpty();
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty && quizMode && !normalizedQuery.isEmpty()) {
            emptyTitle.setText(R.string.no_matching_quizzes);
            emptyMessage.setVisibility(View.GONE);
        } else {
            emptyTitle.setText(quizMode
                    ? R.string.no_saved_quizzes
                    : R.string.no_saved_flashcards);
            emptyMessage.setVisibility(View.VISIBLE);
        }
    }

    private String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(
                value.trim().toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "");
    }

    private void showQuizMenu(View anchor, StudySet studySet) {
        if (!quizMode) return;
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenu().add(0, 1, 0,
                studySet.isPinned() ? R.string.unpin_quiz : R.string.pin_quiz);
        popupMenu.getMenu().add(0, 2, 1, R.string.rename_quiz);
        popupMenu.getMenu().add(0, 3, 2, R.string.delete_quiz);
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                updatePinned(studySet, !studySet.isPinned());
                return true;
            }
            if (item.getItemId() == 2) {
                showRenameDialog(studySet);
                return true;
            }
            if (item.getItemId() == 3) {
                showDeleteDialog(studySet);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void showRenameDialog(StudySet studySet) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(R.string.rename_quiz_hint);
        input.setText(studySet.getTitle());
        input.setSelection(input.length());
        int horizontalPadding = (int) (24 * getResources().getDisplayMetrics().density);
        input.setPadding(horizontalPadding, 0, horizontalPadding, 0);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.rename_quiz)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) {
                        input.setError(getString(R.string.quiz_name_required));
                        return;
                    }
                    updateQuizTitle(dialog, studySet, title);
                }));
        dialog.show();
    }

    private void updateQuizTitle(
            AlertDialog dialog, StudySet studySet, String title) {
        AIContentRepository.getInstance().updateQuizSet(
                studySet.getQuizSetId(), title, null,
                new ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        runOnUiThread(() -> {
                            studySet.setTitle(title);
                            dialog.dismiss();
                            filterStudySets(searchInput.getText() == null
                                    ? ""
                                    : searchInput.getText().toString());
                            Toast.makeText(
                                    StudySetsActivity.this,
                                    R.string.rename_quiz_success,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> Toast.makeText(
                                StudySetsActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG).show());
                    }
                });
    }

    private void updatePinned(StudySet studySet, boolean pinned) {
        AIContentRepository.getInstance().updateQuizSet(
                studySet.getQuizSetId(), null, pinned,
                new ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        runOnUiThread(() -> {
                            studySet.setPinned(pinned);
                            allStudySets.sort((first, second) ->
                                    Boolean.compare(
                                            second.isPinned(), first.isPinned()));
                            filterStudySets(searchInput.getText() == null
                                    ? ""
                                    : searchInput.getText().toString());
                            Toast.makeText(
                                    StudySetsActivity.this,
                                    pinned
                                            ? R.string.pin_quiz_success
                                            : R.string.unpin_quiz_success,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> Toast.makeText(
                                StudySetsActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG).show());
                    }
                });
    }

    private void showDeleteDialog(StudySet studySet) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_quiz)
                .setMessage(getString(
                        R.string.delete_quiz_confirm, studySet.getTitle()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete,
                        (dialog, which) -> deleteQuizSet(studySet))
                .show();
    }

    private void deleteQuizSet(StudySet studySet) {
        AIContentRepository.getInstance().deleteQuizSet(
                studySet.getQuizSetId(),
                new ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        runOnUiThread(() -> {
                            allStudySets.remove(studySet);
                            filterStudySets(searchInput.getText() == null
                                    ? ""
                                    : searchInput.getText().toString());
                            Toast.makeText(
                                    StudySetsActivity.this,
                                    R.string.delete_quiz_success,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> Toast.makeText(
                                StudySetsActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG).show());
                    }
                });
    }

    private void openStudySet(StudySet studySet) {
        Intent intent = new Intent(
                this,
                quizMode ? QuizActivity.class : FlashcardsActivity.class);
        intent.putExtra(Constants.EXTRA_DOCUMENT_ID, studySet.getDocumentId());
        intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, studySet.getDocumentName());
        intent.putExtra(Constants.EXTRA_DOCUMENT_URL, studySet.getDocumentPath());
        intent.putExtra(Constants.EXTRA_DOCUMENT_PATH, studySet.getDocumentPath());
        intent.putExtra(Constants.EXTRA_DOCUMENT_TYPE, studySet.getDocumentType());
        intent.putExtra(Constants.EXTRA_TOPIC_ID, studySet.getTopicId());
        intent.putExtra(Constants.EXTRA_QUIZ_SET_ID, studySet.getQuizSetId());
        intent.putExtra(Constants.EXTRA_QUIZ_DIFFICULTY, studySet.getDifficulty());
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        allStudySets.clear();
        adapter.submitList(null);
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }
}
