package com.example.aistudyassistant.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.models.Topic;

import java.util.List;

public class TopicAdapter extends RecyclerView.Adapter<TopicAdapter.TopicViewHolder> {

    private final Context context;
    private final List<Topic> topics;
    private OnTopicClickListener listener;

    public interface OnTopicClickListener {
        void onTopicClick(Topic topic);
        void onTopicMoreClick(Topic topic, View anchor);
    }

    public TopicAdapter(Context context, List<Topic> topics) {
        this.context = context;
        this.topics = topics;
    }

    public void setListener(OnTopicClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_topic, parent, false);
        return new TopicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
        Topic topic = topics.get(position);
        holder.tvName.setText(topic.getName());
        holder.tvDescription.setText(topic.getDescription());
        holder.ivPinned.setVisibility(topic.isPinned() ? View.VISIBLE : View.GONE);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTopicClick(topic);
        });

        holder.btnMore.setOnClickListener(v -> {
            if (listener != null) listener.onTopicMoreClick(topic, v);
        });
    }

    @Override
    public int getItemCount() {
        return topics.size();
    }

    static class TopicViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription;
        ImageButton btnMore;
        ImageView ivPinned;

        public TopicViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_topic_name);
            tvDescription = itemView.findViewById(R.id.tv_topic_description);
            btnMore = itemView.findViewById(R.id.btn_more);
            ivPinned = itemView.findViewById(R.id.iv_pinned);
        }
    }
}
