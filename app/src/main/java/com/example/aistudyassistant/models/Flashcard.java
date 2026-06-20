package com.example.aistudyassistant.models;

public class Flashcard {
    private String id;
    private String documentId;
    private String front;   // Question / Term
    private String back;    // Answer / Explanation
    private int orderIndex;
    private boolean isKnown;

    public Flashcard() {}

    public Flashcard(String front, String back) {
        this.front = front;
        this.back = back;
        this.isKnown = false;
    }

    // Getters
    public String getId() { return id; }
    public String getDocumentId() { return documentId; }
    public String getFront() { return front; }
    public String getBack() { return back; }
    public int getOrderIndex() { return orderIndex; }
    public boolean isKnown() { return isKnown; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setFront(String front) { this.front = front; }
    public void setBack(String back) { this.back = back; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public void setKnown(boolean known) { isKnown = known; }
}
