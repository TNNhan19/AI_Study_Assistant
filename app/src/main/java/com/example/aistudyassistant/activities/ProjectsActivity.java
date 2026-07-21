package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.adapters.ProjectAdapter;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.models.Project;
import com.example.aistudyassistant.repositories.ProjectRepository;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ProjectsActivity extends AppCompatActivity {

    private RecyclerView rvProjects;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;
    private ProjectAdapter adapter;
    private final List<Project> projects = new ArrayList<>();
    private ProjectRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);
        
        // Cập nhật Token mới nhất cho SupabaseClient
        String accessToken = SharedPrefManager.getInstance(this).getAccessToken();
        com.example.aistudyassistant.api.SupabaseClient.getInstance().setAccessToken(accessToken);
        
        repository = ProjectRepository.getInstance();
        initViews();
        setupRecyclerView();
        loadProjects();
    }

    private void initViews() {
        rvProjects = findViewById(R.id.rv_projects);
        layoutEmpty = findViewById(R.id.layout_empty);
        progressBar = findViewById(R.id.progress_bar);
        fabAdd = findViewById(R.id.fab_add_project);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        fabAdd.setOnClickListener(v -> showAddProjectDialog());
    }

    private void setupRecyclerView() {
        adapter = new ProjectAdapter(this, projects);
        adapter.setListener(new ProjectAdapter.OnProjectClickListener() {
            @Override
            public void onProjectClick(Project project) {
                Intent intent = new Intent(ProjectsActivity.this, TopicsActivity.class);
                intent.putExtra("project_id", project.getId());
                intent.putExtra("project_name", project.getName());
                startActivity(intent);
            }

            @Override
            public void onProjectMoreClick(Project project, View anchor) {
                showProjectPopupMenu(project, anchor);
            }
        });
        rvProjects.setLayoutManager(new LinearLayoutManager(this));
        rvProjects.setAdapter(adapter);
    }

    private void loadProjects() {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        if (userId == null) return;

        setLoading(true);
        repository.getAllProjects(userId, new ApiCallback<List<Project>>() {
            @Override
            public void onSuccess(List<Project> result) {
                runOnUiThread(() -> {
                    setLoading(false);
                    projects.clear();
                    projects.addAll(result);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(ProjectsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showAddProjectDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_project, null);
        EditText etName = view.findViewById(R.id.et_project_name);
        EditText etDesc = view.findViewById(R.id.et_project_description);

        new AlertDialog.Builder(this)
                .setTitle("Create New Project")
                .setView(view)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    if (!name.isEmpty()) {
                        createProject(name, desc);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createProject(String name, String desc) {
        String userId = SharedPrefManager.getInstance(this).getUserId();
        setLoading(true);
        repository.createProject(userId, name, desc, new ApiCallback<Project>() {
            @Override
            public void onSuccess(Project result) {
                runOnUiThread(() -> {
                    setLoading(false);
                    projects.add(0, result);
                    adapter.notifyItemInserted(0);
                    rvProjects.scrollToPosition(0);
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(ProjectsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showProjectPopupMenu(Project project, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("Edit");
        popup.getMenu().add("Delete");
        popup.setOnMenuItemClickListener(item -> {
            if ("Edit".equals(item.getTitle())) {
                showEditProjectDialog(project);
            } else {
                confirmDeleteProject(project);
            }
            return true;
        });
        popup.show();
    }

    private void showEditProjectDialog(Project project) {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_project, null);
        EditText etName = view.findViewById(R.id.et_project_name);
        EditText etDesc = view.findViewById(R.id.et_project_description);
        
        etName.setText(project.getName());
        etDesc.setText(project.getDescription());

        new AlertDialog.Builder(this)
                .setTitle("Edit Project")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    project.setName(etName.getText().toString().trim());
                    project.setDescription(etDesc.getText().toString().trim());
                    updateProject(project);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateProject(Project project) {
        setLoading(true);
        repository.updateProject(project, new ApiCallback<Boolean>() {
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
                    Toast.makeText(ProjectsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void confirmDeleteProject(Project project) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Project")
                .setMessage("Are you sure? All topics and documents in this project will remain but won't be linked to this project anymore.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteProject(project);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProject(Project project) {
        setLoading(true);
        repository.deleteProject(project.getId(), new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    setLoading(false);
                    int pos = projects.indexOf(project);
                    projects.remove(project);
                    adapter.notifyItemRemoved(pos);
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(ProjectsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void updateEmptyState() {
        layoutEmpty.setVisibility(projects.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
