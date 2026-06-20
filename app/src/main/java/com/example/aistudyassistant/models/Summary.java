package com.example.aistudyassistant.models;

import java.util.List;

public class Summary {
    private String id;
    private String documentId;
    private String summaryText;
    private List<String> keyPoints;
    private List<String> keywords;
    private String conclusion;
    private long createdAt;

    public Summary() {}

    // Getters
    public String getId() { return id; }
    public String getDocumentId() { return documentId; }
    public String getSummaryText() { return summaryText; }
    public List<String> getKeyPoints() { return keyPoints; }
    public List<String> getKeywords() { return keywords; }
    public String getConclusion() { return conclusion; }
    public long getCreatedAt() { return createdAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public void setKeyPoints(List<String> keyPoints) { this.keyPoints = keyPoints; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
