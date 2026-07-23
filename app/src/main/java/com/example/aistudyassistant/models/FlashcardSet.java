package com.example.aistudyassistant.models;

public class FlashcardSet {
    private String documentId;
    private String topicId;
    private String projectId;
    private String documentName;
    private String projectName;
    private int cardCount;
    private long createdAt;

    public String getDocumentId() { return documentId; }
    public String getTopicId() { return topicId; }
    public String getProjectId() { return projectId; }
    public String getDocumentName() { return documentName; }
    public String getProjectName() { return projectName; }
    public int getCardCount() { return cardCount; }
    public long getCreatedAt() { return createdAt; }

    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public void setCardCount(int cardCount) { this.cardCount = cardCount; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
