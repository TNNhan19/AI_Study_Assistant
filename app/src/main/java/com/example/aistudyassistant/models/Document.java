package com.example.aistudyassistant.models;

public class Document {
    private String id;
    private String userId;
    private String projectId;
    private String topicId;
    private String name;
    private String filePath;
    private String fileType;  // pdf, txt, docx
    private long fileSize;    // bytes
    private String status;    // UPLOADED, PROCESSING, COMPLETED, FAILED
    private boolean favorite;
    private long createdAt;
    private long updatedAt;

    public Document() {}

    public Document(String id, String userId, String name, String fileUrl,
                    String fileType, long fileSize, String status) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        // fileUrl cũ được map sang filePath vì bucket private lưu path thay vì public URL.
        this.filePath = fileUrl;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getProjectId() { return projectId; }
    public String getTopicId() { return topicId; }
    public String getName() { return name; }
    public String getFilePath() { return filePath; }
    public String getFileUrl() { return filePath; }
    public String getFileType() { return fileType; }
    public long getFileSize() { return fileSize; }
    public String getStatus() { return status; }
    public boolean isFavorite() { return favorite; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }
    public void setName(String name) { this.name = name; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setFileUrl(String fileUrl) { this.filePath = fileUrl; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public void setStatus(String status) { this.status = status; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

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
