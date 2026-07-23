package com.example.aistudyassistant.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.models.Note;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private final Context context;
    private final List<Note> notes;
    private OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
        void onNoteMoreClick(Note note, View anchor);
    }

    public NoteAdapter(Context context, List<Note> notes) {
        this.context = context;
        this.notes = notes;
    }

    public void setListener(OnNoteClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.tvTitle.setText(note.getTitle());
        holder.tvContent.setText(note.getContent());
        holder.ivPinned.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);

        String contextLabel = buildReadableContextLabel(note);
        holder.tvContext.setText(contextLabel);
        holder.tvContext.setVisibility(
                contextLabel.isEmpty() ? View.GONE : View.VISIBLE);
        
        // Date formatting (simplified)
        if (note.getCreatedAt() != null) {
            String date = note.getCreatedAt().split("T")[0];
            holder.tvDate.setText(date);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNoteClick(note);
        });

        holder.btnMore.setOnClickListener(v -> {
            if (listener != null) listener.onNoteMoreClick(note, v);
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    private String buildReadableContextLabel(Note note) {
        java.util.List<String> contextParts = new java.util.ArrayList<>();
        if (!TextUtils.isEmpty(note.getProjectName())) {
            contextParts.add(context.getString(
                    R.string.note_project_format,
                    note.getProjectName()));
        }
        String topicName = TextUtils.isEmpty(note.getTopicName())
                ? context.getString(R.string.no_topic)
                : note.getTopicName();
        contextParts.add(context.getString(R.string.note_topic_format, topicName));
        if (!TextUtils.isEmpty(note.getDocumentName())) {
            contextParts.add(context.getString(
                    R.string.note_document_format,
                    note.getDocumentName()));
        }
        return TextUtils.join(" | ", contextParts);
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvContext, tvDate;
        ImageButton btnMore;
        ImageView ivPinned;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_note_title);
            tvContent = itemView.findViewById(R.id.tv_note_content);
            tvContext = itemView.findViewById(R.id.tv_note_context);
            tvDate = itemView.findViewById(R.id.tv_note_date);
            btnMore = itemView.findViewById(R.id.btn_more);
            ivPinned = itemView.findViewById(R.id.iv_pinned);
        }
    }
}
