package com.example.aistudyassistant.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.models.FlashcardSet;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FlashcardSetHistoryAdapter
        extends RecyclerView.Adapter<FlashcardSetHistoryAdapter.FlashcardSetViewHolder> {

    private final Context context;
    private final List<FlashcardSet> sets;
    private OnFlashcardSetClickListener listener;

    public interface OnFlashcardSetClickListener {
        void onFlashcardSetClick(FlashcardSet set);
    }

    public FlashcardSetHistoryAdapter(Context context, List<FlashcardSet> sets) {
        this.context = context;
        this.sets = sets;
    }

    public void setListener(OnFlashcardSetClickListener listener) {
        this.listener = listener;
    }

    public void updateSets(List<FlashcardSet> newSets) {
        if (sets == newSets) {
            notifyDataSetChanged();
            return;
        }
        sets.clear();
        sets.addAll(newSets);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FlashcardSetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_flashcard_set_history, parent, false);
        return new FlashcardSetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FlashcardSetViewHolder holder, int position) {
        holder.bind(sets.get(position));
    }

    @Override
    public int getItemCount() {
        return sets.size();
    }

    class FlashcardSetViewHolder extends RecyclerView.ViewHolder {
        TextView tvDocument, tvFolder, tvCreatedAt, tvCount, tvReview;

        FlashcardSetViewHolder(View itemView) {
            super(itemView);
            tvDocument = itemView.findViewById(R.id.tv_flashcard_history_document);
            tvFolder = itemView.findViewById(R.id.tv_flashcard_history_folder);
            tvCreatedAt = itemView.findViewById(R.id.tv_flashcard_history_created_at);
            tvCount = itemView.findViewById(R.id.tv_flashcard_history_count);
            tvReview = itemView.findViewById(R.id.tv_flashcard_history_review);
        }

        void bind(FlashcardSet set) {
            String documentName = isBlank(set.getDocumentName())
                    ? context.getString(R.string.quiz_unknown_document)
                    : set.getDocumentName();
            tvDocument.setText(documentName);

            if (isBlank(set.getProjectName())) {
                tvFolder.setVisibility(View.GONE);
            } else {
                tvFolder.setVisibility(View.VISIBLE);
                tvFolder.setText(context.getString(
                        R.string.quiz_folder_format,
                        set.getProjectName()
                ));
            }

            tvCreatedAt.setText(formatCreatedAt(set.getCreatedAt()));
            tvCount.setText(context.getString(
                    R.string.flashcard_count_format,
                    set.getCardCount()
            ));
            tvReview.setText(R.string.review_flashcards);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onFlashcardSetClick(set);
            });
        }

        private String formatCreatedAt(long createdAt) {
            if (createdAt <= 0) return context.getString(R.string.quiz_unknown_time);
            SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            return format.format(new Date(createdAt));
        }

        private boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
