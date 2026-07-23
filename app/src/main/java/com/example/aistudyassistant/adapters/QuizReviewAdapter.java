package com.example.aistudyassistant.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.models.QuizQuestion;

import java.util.ArrayList;
import java.util.List;

public class QuizReviewAdapter
        extends RecyclerView.Adapter<QuizReviewAdapter.QuizReviewViewHolder> {

    private final Context context;
    private final List<ReviewItem> items = new ArrayList<>();

    public QuizReviewAdapter(Context context) {
        this.context = context;
    }

    public void updateItems(List<ReviewItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QuizReviewViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_quiz_review_question, parent, false);
        return new QuizReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuizReviewViewHolder holder, int position) {
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ReviewItem {
        private final QuizQuestion question;
        private final String selectedAnswer;
        private final boolean answerRecorded;

        public ReviewItem(QuizQuestion question, String selectedAnswer,
                          boolean answerRecorded) {
            this.question = question;
            this.selectedAnswer = selectedAnswer;
            this.answerRecorded = answerRecorded;
        }
    }

    class QuizReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatus, tvQuestion, tvSelected, tvCorrect, tvExplanation;

        QuizReviewViewHolder(View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tv_review_status);
            tvQuestion = itemView.findViewById(R.id.tv_review_question);
            tvSelected = itemView.findViewById(R.id.tv_review_selected);
            tvCorrect = itemView.findViewById(R.id.tv_review_correct);
            tvExplanation = itemView.findViewById(R.id.tv_review_explanation);
        }

        void bind(ReviewItem item, int position) {
            QuizQuestion question = item.question;
            String correctAnswer = normalizeAnswer(question.getCorrectAnswer());
            String selectedAnswer = normalizeAnswer(item.selectedAnswer);
            boolean correct = item.answerRecorded && selectedAnswer.equals(correctAnswer);

            tvQuestion.setText((position + 1) + ". " + question.getQuestion());

            if (!item.answerRecorded) {
                tvStatus.setText(R.string.quiz_review_not_recorded);
                tvStatus.setTextColor(context.getResources().getColor(R.color.text_secondary));
            } else {
                tvStatus.setText(correct
                        ? R.string.quiz_review_correct
                        : R.string.quiz_review_incorrect);
                tvStatus.setTextColor(context.getResources().getColor(
                        correct ? R.color.quiz_correct : R.color.quiz_wrong));
            }

            String selectedText = item.answerRecorded
                    ? formatAnswer(selectedAnswer, question.getOptionByLetter(selectedAnswer))
                    : context.getString(R.string.quiz_review_missing_answer);
            tvSelected.setText(context.getString(
                    R.string.quiz_review_selected_format, selectedText));
            tvSelected.setTextColor(context.getResources().getColor(
                    correct ? R.color.quiz_correct : R.color.quiz_wrong));

            tvCorrect.setText(context.getString(
                    R.string.quiz_review_correct_format,
                    formatAnswer(correctAnswer, question.getOptionByLetter(correctAnswer))));

            if (TextUtils.isEmpty(question.getExplanation())) {
                tvExplanation.setVisibility(View.GONE);
            } else {
                tvExplanation.setVisibility(View.VISIBLE);
                tvExplanation.setText(context.getString(
                        R.string.quiz_review_explanation_format,
                        question.getExplanation()));
            }
        }

        private String formatAnswer(String letter, String answerText) {
            if (TextUtils.isEmpty(letter)) return "";
            if (TextUtils.isEmpty(answerText)) return letter;
            return letter + ". " + answerText;
        }

        private String normalizeAnswer(String value) {
            return value == null ? "" : value.trim().toUpperCase();
        }
    }
}
