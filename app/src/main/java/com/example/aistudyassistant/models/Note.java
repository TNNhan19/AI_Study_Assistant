package com.example.aistudyassistant.models;

public class Note {
    private String id;
    private String userId;
    private String documentId;
    private String topicId;
    private String title;
    private String content;
    private boolean pinned;
    private String createdAt;
    private String updatedAt;

    public Note() {}

    public Note(String userId, String title, String content) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.pinned = false;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getDocumentId() { return documentId; }
    public String getTopicId() { return topicId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isPinned() { return pinned; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
