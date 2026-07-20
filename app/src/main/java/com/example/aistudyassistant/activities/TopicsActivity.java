package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.adapters.TopicAdapter;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.models.Topic;
import com.example.aistudyassistant.repositories.TopicRepository;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TopicsActivity extends AppCompatActivity {

    private RecyclerView rvTopics;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;
    private TextView tvTitle;
    
    private TopicAdapter adapter;
    private final List<Topic> topics = new ArrayList<>();
    private TopicRepository repository;
    
    private String projectId;
    private String projectName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topics);
        
        // Cập nhật Token
        String accessToken = SharedPrefManager.getInstance(this).getAccessToken();
        com.example.aistudyassistant.api.SupabaseClient.getInstance().setAccessToken(accessToken);

        projectId = getIntent().getStringExtra("project_id");
        projectName = getIntent().getStringExtra("project_name");
        
        repository = TopicRepository.getInstance();
        initViews();
        setupRecyclerView();
        loadTopics();
    }

    private void initViews() {
        rvTopics = findViewById(R.id.rv_topics);
        layoutEmpty = findViewById(R.id.layout_empty);
        progressBar = findViewById(R.id.progress_bar);
        fabAdd = findViewById(R.id.fab_add_topic);
        tvTitle = findViewById(R.id.tv_toolbar_title);

        tvTitle.setText(projectName != null ? projectName : "Topics");
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        fabAdd.setOnClickListener(v -> showAddTopicDialog());
    }

    private void setupRecyclerView() {
        adapter = new TopicAdapter(this, topics);
        adapter.setListener(new TopicAdapter.OnTopicClickListener() {
            @Override
            public void onTopicClick(Topic topic) {
                // Mở màn hình danh sách tài liệu và lọc theo Topic này
                Intent intent = new Intent(TopicsActivity.this, DocumentsActivity.class);
                intent.putExtra("project_id", projectId);
                intent.putExtra("topic_id", topic.getId());
                intent.putExtra("topic_name", topic.getName());
                startActivity(intent);
            }

            @Override
            public void onTopicMoreClick(Topic topic, View anchor) {
                showTopicPopupMenu(topic, anchor);
            }
        });
        rvTopics.setLayoutManager(new LinearLayoutManager(this));
        rvTopics.setAdapter(adapter);
    }

    private void loadTopics() {
        if (projectId == null) return;

        setLoading(true);
        repository.getTopicsByProject(projectId, new ApiCallback<List<Topic>>() {
            @Override
            public void onSuccess(List<Topic> result) {
                runOnUiThread(() -> {
                    setLoading(false);
                    topics.clear();
                    topics.addAll(result);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(TopicsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showAddTopicDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_project, null);
        EditText etName = view.findViewById(R.id.et_project_name);
        EditText etDesc = view.findViewById(R.id.et_project_description);
        
        com.google.android.material.textfield.TextInputLayout tilName = view.findViewById(R.id.til_project_name);
        if (tilName != null) tilName.setHint("Topic Name");

        new AlertDialog.Builder(this)
                .setTitle("Create New Topic")
                .setView(view)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    if (!name.isEmpty()) {
                        createTopic(name, desc);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createTopic(String name, String desc) {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Session expired, please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        Topic topic = new Topic(projectId, userId, name, desc);
        
        setLoading(true);
        repository.createTopic(topic, new ApiCallback<Topic>() {
            @Override
            public void onSuccess(Topic result) {
                runOnUiThread(() -> {
                    setLoading(false);
                    topics.add(0, result);
                    adapter.notifyItemInserted(0);
                    rvTopics.scrollToPosition(0);
                    updateEmptyState();
                    Toast.makeText(TopicsActivity.this, "Topic created", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(TopicsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showTopicPopupMenu(Topic topic, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("Edit");
        popup.getMenu().add(topic.isPinned() ? "Unpin" : "Pin");
        popup.getMenu().add("Delete");
        
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Edit".equals(title)) {
                showEditTopicDialog(topic);
            } else if ("Pin".equals(title) || "Unpin".equals(title)) {
                togglePin(topic);
            } else {
                confirmDeleteTopic(topic);
            }
            return true;
        });
        popup.show();
    }

    private void togglePin(Topic topic) {
        boolean newState = !topic.isPinned();
        topic.setPinned(newState);
        setLoading(true);
        repository.togglePin(topic.getId(), newState, new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    String msg = newState ? "Topic pinned" : "Topic unpinned";
                    Toast.makeText(TopicsActivity.this, msg, Toast.LENGTH_SHORT).show();
                    loadTopics(); // Tải lại để sắp xếp lại danh sách
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    topic.setPinned(!newState); // Hoàn tác nếu lỗi
                    Toast.makeText(TopicsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showEditTopicDialog(Topic topic) {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_project, null);
        EditText etName = view.findViewById(R.id.et_project_name);
        EditText etDesc = view.findViewById(R.id.et_project_description);
        
        etName.setText(topic.getName());
        etDesc.setText(topic.getDescription());

        new AlertDialog.Builder(this)
                .setTitle("Edit Topic")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    topic.setName(etName.getText().toString().trim());
                    topic.setDescription(etDesc.getText().toString().trim());
                    updateTopic(topic);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateTopic(Topic topic) {
        setLoading(true);
        repository.updateTopic(topic, new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    setLoading(false);
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(TopicsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void confirmDeleteTopic(Topic topic) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Topic")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTopic(topic))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTopic(Topic topic) {
        setLoading(true);
        repository.deleteTopic(topic.getId(), new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    setLoading(false);
                    int pos = topics.indexOf(topic);
                    topics.remove(topic);
                    adapter.notifyItemRemoved(pos);
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(TopicsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void updateEmptyState() {
        layoutEmpty.setVisibility(topics.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
