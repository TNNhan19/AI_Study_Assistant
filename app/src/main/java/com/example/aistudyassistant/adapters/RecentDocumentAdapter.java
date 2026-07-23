package com.example.aistudyassistant.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.models.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecentDocumentAdapter
        extends RecyclerView.Adapter<RecentDocumentAdapter.RecentDocumentViewHolder> {

    public interface OnDocumentClickListener {
        void onDocumentClick(Document document);
    }

    private final Context context;
    private final List<Document> documents = new ArrayList<>();
    private final Map<String, Long> openedAtByDocumentId = new HashMap<>();
    private OnDocumentClickListener listener;

    public RecentDocumentAdapter(Context context) {
        this.context = context;
    }

    public void setListener(OnDocumentClickListener listener) {
        this.listener = listener;
    }

    public void updateDocuments(List<Document> newDocuments, Map<String, Long> openedAt) {
        documents.clear();
        documents.addAll(newDocuments);
        openedAtByDocumentId.clear();
        openedAtByDocumentId.putAll(openedAt);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecentDocumentViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_recent_document, parent, false);
        return new RecentDocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecentDocumentViewHolder holder, int position) {
        holder.bind(documents.get(position));
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    class RecentDocumentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvFileTypeIcon;
        private final TextView tvDocumentName;
        private final TextView tvDocumentMeta;
        private final TextView tvOpenedAt;

        RecentDocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileTypeIcon = itemView.findViewById(R.id.tv_recent_file_type_icon);
            tvDocumentName = itemView.findViewById(R.id.tv_recent_doc_name);
            tvDocumentMeta = itemView.findViewById(R.id.tv_recent_doc_meta);
            tvOpenedAt = itemView.findViewById(R.id.tv_recent_opened_at);
        }

        void bind(Document document) {
            tvFileTypeIcon.setText(document.getFileTypeIcon());
            tvDocumentName.setText(document.getName());
            String fileType = document.getFileType() == null
                    ? "" : document.getFileType().toUpperCase(Locale.getDefault());
            tvDocumentMeta.setText(context.getString(
                    R.string.recent_document_meta,
                    fileType,
                    document.getFileSizeFormatted()));

            long openedAt = openedAtByDocumentId.containsKey(document.getId())
                    ? openedAtByDocumentId.get(document.getId()) : 0L;
            if (openedAt > 0L) {
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                        openedAt,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE);
                tvOpenedAt.setText(context.getString(
                        R.string.recent_document_opened, relativeTime));
            } else {
                tvOpenedAt.setText(R.string.recent_document_opened_recently);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDocumentClick(document);
                }
            });
        }
    }
}
