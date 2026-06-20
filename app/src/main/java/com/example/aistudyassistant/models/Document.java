package com.example.aistudyassistant.models;

public class Document {
    private String id;
    private String userId;
    private String name;
    private String fileUrl;
    private String fileType;  // pdf, txt, docx
    private long fileSize;    // bytes
    private String status;    // UPLOADED, PROCESSING, COMPLETED, FAILED
    private long createdAt;

    public Document() {}

    public Document(String id, String userId, String name, String fileUrl,
                    String fileType, long fileSize, String status) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getFileUrl() { return fileUrl; }
    public String getFileType() { return fileType; }
    public long getFileSize() { return fileSize; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    /** Converts bytes to a human-readable size string */
    public String getFileSizeFormatted() {
        if (fileSize < 1024) return fileSize + " B";
        else if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        else return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }

    /** Returns the file type icon emoji */
    public String getFileTypeIcon() {
        if (fileType == null) return "📄";
        switch (fileType.toLowerCase()) {
            case "pdf": return "📕";
            case "txt": return "📝";
            case "docx":
            case "doc": return "📘";
            default: return "📄";
        }
    }
}
