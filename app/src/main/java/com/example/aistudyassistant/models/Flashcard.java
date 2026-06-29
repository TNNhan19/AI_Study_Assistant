package com.example.aistudyassistant.models;

public class Flashcard {
    private String id;
    private String userId;
    private String documentId;
    private String topicId;
    private String front;   // Question / Term
    private String back;    // Answer / Explanation
    private String difficulty;
    private long nextReviewAt;
    private long createdAt;
    private boolean isKnown;

    public Flashcard() {}

    public Flashcard(String front, String back) {
        this.front = front;
        this.back = back;
        this.difficulty = "MEDIUM";
        this.createdAt = System.currentTimeMillis();
        // isKnown chỉ phục vụ UI ôn tập hiện tại, không phải cột trong bảng flashcards.
        this.isKnown = false;
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getDocumentId() { return documentId; }
    public String getTopicId() { return topicId; }
    public String getFront() { return front; }
    public String getBack() { return back; }
    public String getDifficulty() { return difficulty; }
    public long getNextReviewAt() { return nextReviewAt; }
    public long getCreatedAt() { return createdAt; }
    public boolean isKnown() { return isKnown; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }
    public void setFront(String front) { this.front = front; }
    public void setBack(String back) { this.back = back; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setNextReviewAt(long nextReviewAt) { this.nextReviewAt = nextReviewAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setKnown(boolean known) { isKnown = known; }
}
