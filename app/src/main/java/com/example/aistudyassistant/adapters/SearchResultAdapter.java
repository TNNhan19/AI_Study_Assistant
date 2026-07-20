package com.example.aistudyassistant.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.models.SearchResult;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder> {

    private final Context context;
    private final List<SearchResult> results;
    private OnResultClickListener listener;

    public interface OnResultClickListener {
        void onResultClick(SearchResult result);
    }

    public SearchResultAdapter(Context context, List<SearchResult> results) {
        this.context = context;
        this.results = results;
    }

    public void setListener(OnResultClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_result, parent, false);
        return new SearchResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        SearchResult result = results.get(position);
        holder.tvIcon.setText(result.getIcon());
        holder.tvTitle.setText(result.getTitle());
        holder.tvSubtitle.setText(result.getSubtitle());
        holder.tvType.setText(result.getTypeName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onResultClick(result);
        });
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class SearchResultViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvTitle, tvSubtitle, tvType;

        public SearchResultViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tv_result_icon);
            tvTitle = itemView.findViewById(R.id.tv_result_title);
            tvSubtitle = itemView.findViewById(R.id.tv_result_subtitle);
            tvType = itemView.findViewById(R.id.tv_result_type);
        }
    }
}
