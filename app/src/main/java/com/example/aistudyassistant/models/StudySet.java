package com.example.aistudyassistant.models;

public class StudySet {
    private final String documentId;
    private final String documentName;
    private final String documentPath;
    private final String documentType;
    private final String topicId;
    private final String projectName;
    private final String topicName;
    private final String latestCreatedAt;
    private String quizSetId;
    private String title;
    private String difficulty = "MEDIUM";
    private boolean pinned;
    private int itemCount;

    public StudySet(String documentId, String documentName, String documentPath,
                    String documentType, String topicId, String projectName,
                    String topicName, String latestCreatedAt) {
        this.documentId = documentId;
        this.documentName = documentName;
        this.documentPath = documentPath;
        this.documentType = documentType;
        this.topicId = topicId;
        this.projectName = projectName;
        this.topicName = topicName;
        this.latestCreatedAt = latestCreatedAt;
        this.title = documentName;
        this.itemCount = 1;
    }

    public String getDocumentId() { return documentId; }
    public String getDocumentName() { return documentName; }
    public String getDocumentPath() { return documentPath; }
    public String getDocumentType() { return documentType; }
    public String getTopicId() { return topicId; }
    public String getProjectName() { return projectName; }
    public String getTopicName() { return topicName; }
    public String getLatestCreatedAt() { return latestCreatedAt; }
    public int getItemCount() { return itemCount; }
    public String getQuizSetId() { return quizSetId; }
    public String getTitle() { return title; }
    public boolean isPinned() { return pinned; }
    public String getDifficulty() { return difficulty; }

    public void applyQuizMetadata(String quizSetId, String title, boolean pinned,
                                  String difficulty) {
        this.quizSetId = quizSetId;
        if (title != null && !title.trim().isEmpty()) {
            this.title = title.trim();
        }
        this.pinned = pinned;
        if (difficulty != null && !difficulty.trim().isEmpty()) {
            this.difficulty = difficulty.trim();
        }
    }

    public void setTitle(String title) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title.trim();
        }
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public void incrementItemCount() {
        itemCount++;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = Math.max(0, itemCount);
    }
}
