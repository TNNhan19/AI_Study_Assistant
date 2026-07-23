package com.example.aistudyassistant.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.models.StudySet;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class StudySetAdapter extends RecyclerView.Adapter<StudySetAdapter.ViewHolder> {

    public interface Listener {
        void onStudySetClick(StudySet studySet);
        void onStudySetPinClick(StudySet studySet);
        void onStudySetMoreClick(View anchor, StudySet studySet);
    }

    private final boolean quizMode;
    private final Listener listener;
    private final List<StudySet> studySets = new ArrayList<>();

    public StudySetAdapter(boolean quizMode, Listener listener) {
        this.quizMode = quizMode;
        this.listener = listener;
    }

    public void submitList(List<StudySet> values) {
        studySets.clear();
        if (values != null) studySets.addAll(values);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_study_set, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudySet studySet = studySets.get(position);
        holder.icon.setText(quizMode ? "Q" : "F");
        holder.title.setText(quizMode
                ? studySet.getTitle()
                : studySet.getDocumentName());
        holder.source.setVisibility(quizMode ? View.VISIBLE : View.GONE);
        holder.source.setText(holder.itemView.getContext().getString(
                R.string.quiz_source_document,
                studySet.getDocumentName()));
        String countText = holder.itemView.getContext().getResources().getQuantityString(
                quizMode ? R.plurals.quiz_question_count : R.plurals.flashcard_count,
                studySet.getItemCount(),
                studySet.getItemCount());
        holder.meta.setText(quizMode
                ? countText + " · " + difficultyLabel(studySet.getDifficulty())
                : countText);
        String projectName = isBlank(studySet.getProjectName())
                ? holder.itemView.getContext().getString(R.string.no_project)
                : studySet.getProjectName();
        String topicName = isBlank(studySet.getTopicName())
                ? holder.itemView.getContext().getString(R.string.no_topic)
                : studySet.getTopicName();
        holder.context.setText(holder.itemView.getContext().getString(
                R.string.study_set_context,
                projectName,
                topicName));
        holder.pin.setVisibility(quizMode ? View.VISIBLE : View.GONE);
        holder.pin.setImageResource(studySet.isPinned()
                ? android.R.drawable.btn_star_big_on
                : android.R.drawable.btn_star_big_off);
        holder.pin.setColorFilter(holder.itemView.getContext().getColor(
                studySet.isPinned() ? R.color.primary : R.color.text_secondary));
        holder.pin.setContentDescription(holder.itemView.getContext().getString(
                studySet.isPinned() ? R.string.unpin_quiz : R.string.pin_quiz));
        holder.more.setVisibility(quizMode ? View.VISIBLE : View.GONE);
        holder.action.setText(quizMode
                ? R.string.retake_quiz_action
                : R.string.review_flashcards_action);
        holder.itemView.setOnClickListener(v -> listener.onStudySetClick(studySet));
        holder.action.setOnClickListener(v -> listener.onStudySetClick(studySet));
        holder.pin.setOnClickListener(v -> listener.onStudySetPinClick(studySet));
        holder.more.setOnClickListener(v ->
                listener.onStudySetMoreClick(holder.more, studySet));
    }

    @Override
    public int getItemCount() {
        return studySets.size();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String difficultyLabel(String difficulty) {
        if ("EASY".equalsIgnoreCase(difficulty)) return "Easy";
        if ("HARD".equalsIgnoreCase(difficulty)) return "Hard";
        return "Medium";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView icon;
        final TextView title;
        final TextView source;
        final TextView meta;
        final TextView context;
        final ImageButton pin;
        final MaterialButton action;
        final ImageButton more;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.tv_study_set_icon);
            title = itemView.findViewById(R.id.tv_study_set_title);
            source = itemView.findViewById(R.id.tv_study_set_source);
            meta = itemView.findViewById(R.id.tv_study_set_meta);
            context = itemView.findViewById(R.id.tv_study_set_context);
            pin = itemView.findViewById(R.id.btn_study_set_pin);
            action = itemView.findViewById(R.id.btn_open_study_set);
            more = itemView.findViewById(R.id.btn_study_set_more);
        }
    }
}
