package com.example.aistudyassistant.models;

public class Project {
    private String id;
    private String userId;
    private String name;
    private String description;
    private String createdAt;
    private String updatedAt;

    public Project() {}

    public Project(String userId, String name, String description) {
        this.userId = userId;
        this.name = name;
        this.description = description;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
