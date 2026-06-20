package com.example.aistudyassistant.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private final Context context;
    private final List<Document> documents;
    private OnDocumentClickListener listener;

    public interface OnDocumentClickListener {
        void onDocumentClick(Document document);
        void onDocumentMoreClick(Document document, View anchorView);
    }

    public DocumentAdapter(Context context, List<Document> documents) {
        this.context = context;
        this.documents = documents;
    }

    public void setListener(OnDocumentClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        Document doc = documents.get(position);
        holder.bind(doc);
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    public void updateDocuments(List<Document> newDocs) {
        documents.clear();
        documents.addAll(newDocs);
        notifyDataSetChanged();
    }

    public void removeDocument(int position) {
        documents.remove(position);
        notifyItemRemoved(position);
    }

    class DocumentViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileTypeIcon, tvDocName, tvDocSize, tvDocDate, tvStatus;
        ImageButton btnMore;

        DocumentViewHolder(View itemView) {
            super(itemView);
            tvFileTypeIcon = itemView.findViewById(R.id.tv_file_type_icon);
            tvDocName = itemView.findViewById(R.id.tv_doc_name);
            tvDocSize = itemView.findViewById(R.id.tv_doc_size);
            tvDocDate = itemView.findViewById(R.id.tv_doc_date);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnMore = itemView.findViewById(R.id.btn_more);
        }

        void bind(Document doc) {
            tvFileTypeIcon.setText(doc.getFileTypeIcon());
            tvDocName.setText(doc.getName());
            tvDocSize.setText(doc.getFileSizeFormatted());

            // Format date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvDocDate.setText(sdf.format(new Date(doc.getCreatedAt())));

            // Status badge
            setStatusBadge(doc.getStatus());

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onDocumentClick(doc);
            });
            btnMore.setOnClickListener(v -> {
                if (listener != null) listener.onDocumentMoreClick(doc, v);
            });
        }

        private void setStatusBadge(String status) {
            if (status == null) return;
            switch (status) {
                case Constants.STATUS_UPLOADED:
                    tvStatus.setText("Uploaded");
                    tvStatus.setTextColor(context.getResources().getColor(R.color.status_uploaded));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_uploaded);
                    break;
                case Constants.STATUS_PROCESSING:
                    tvStatus.setText("Processing");
                    tvStatus.setTextColor(context.getResources().getColor(R.color.status_processing));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_processing);
                    break;
                case Constants.STATUS_COMPLETED:
                    tvStatus.setText("Completed");
                    tvStatus.setTextColor(context.getResources().getColor(R.color.status_completed));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
                    break;
                case Constants.STATUS_FAILED:
                    tvStatus.setText("Failed");
                    tvStatus.setTextColor(context.getResources().getColor(R.color.status_failed));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_failed);
                    break;
            }
        }
    }
}
