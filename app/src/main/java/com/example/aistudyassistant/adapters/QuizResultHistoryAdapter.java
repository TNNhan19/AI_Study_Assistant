package com.example.aistudyassistant.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.models.QuizResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuizResultHistoryAdapter
        extends RecyclerView.Adapter<QuizResultHistoryAdapter.QuizResultViewHolder> {

    private final Context context;
    private final List<QuizResult> results;
    private OnQuizResultClickListener listener;

    public interface OnQuizResultClickListener {
        void onQuizResultClick(QuizResult result);
    }

    public QuizResultHistoryAdapter(Context context, List<QuizResult> results) {
        this.context = context;
        this.results = results;
    }

    public void setListener(OnQuizResultClickListener listener) {
        this.listener = listener;
    }

    public void updateResults(List<QuizResult> newResults) {
        if (results == newResults) {
            notifyDataSetChanged();
            return;
        }
        results.clear();
        results.addAll(newResults);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QuizResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_quiz_result_history, parent, false);
        return new QuizResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuizResultViewHolder holder, int position) {
        holder.bind(results.get(position));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    class QuizResultViewHolder extends RecyclerView.ViewHolder {
        TextView tvDocument, tvFolder, tvCompletedAt, tvScore, tvCorrectWrong, tvReview;

        QuizResultViewHolder(View itemView) {
            super(itemView);
            tvDocument = itemView.findViewById(R.id.tv_quiz_history_document);
            tvFolder = itemView.findViewById(R.id.tv_quiz_history_folder);
            tvCompletedAt = itemView.findViewById(R.id.tv_quiz_history_completed_at);
            tvScore = itemView.findViewById(R.id.tv_quiz_history_score);
            tvCorrectWrong = itemView.findViewById(R.id.tv_quiz_history_correct_wrong);
            tvReview = itemView.findViewById(R.id.tv_quiz_history_review);
        }

        void bind(QuizResult result) {
            String documentName = isBlank(result.getDocumentName())
                    ? context.getString(R.string.quiz_unknown_document)
                    : result.getDocumentName();
            tvDocument.setText(documentName);

            if (isBlank(result.getProjectName())) {
                tvFolder.setVisibility(View.GONE);
            } else {
                tvFolder.setVisibility(View.VISIBLE);
                tvFolder.setText(context.getString(
                        R.string.quiz_folder_format,
                        result.getProjectName()
                ));
            }

            tvCompletedAt.setText(formatCompletedAt(result.getCompletedAt()));
            tvScore.setText(context.getString(
                    R.string.quiz_score_format,
                    result.getScore(),
                    result.getTotalQuestions()
            ));
            tvCorrectWrong.setText(context.getString(
                    R.string.quiz_history_correct_wrong_format,
                    result.getCorrectCount(),
                    result.getWrongCount()
            ));
            tvReview.setText(R.string.review_quiz);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onQuizResultClick(result);
            });
        }

        private String formatCompletedAt(long completedAt) {
            if (completedAt <= 0) return context.getString(R.string.quiz_unknown_time);
            SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            return format.format(new Date(completedAt));
        }

        private boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
