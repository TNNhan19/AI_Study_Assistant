package com.example.aistudyassistant.models;

public class Topic {
    private String id;
    private String projectId;
    private String userId;
    private String name;
    private String description;
    private boolean pinned;
    private String createdAt;
    private String updatedAt;

    public Topic() {}

    public Topic(String projectId, String userId, String name, String description) {
        this.projectId = projectId;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.pinned = false;
    }

    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isPinned() { return pinned; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    public void setId(String id) { this.id = id; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
