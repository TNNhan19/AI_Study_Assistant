package com.example.aistudyassistant.models;

public class ChatMessage {
    private String id;
    private String userId;
    private String documentId;  // null for general chat
    private String content;
    private int type;           // 0 = USER, 1 = AI
    private long timestamp;

    public ChatMessage() {}

    public ChatMessage(String content, int type) {
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String content, int type, String documentId) {
        this(content, type);
        this.documentId = documentId;
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getDocumentId() { return documentId; }
    public String getContent() { return content; }
    public int getType() { return type; }
    public long getTimestamp() { return timestamp; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setContent(String content) { this.content = content; }
    public void setType(int type) { this.type = type; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isUserMessage() { return type == 0; }
    public boolean isAiMessage() { return type == 1; }
}
