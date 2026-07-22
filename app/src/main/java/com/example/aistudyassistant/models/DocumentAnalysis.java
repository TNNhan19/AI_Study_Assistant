package com.example.aistudyassistant.models;

/**
 * Kết quả đọc và chuẩn hóa nội dung một tài liệu trước khi gửi sang AI.
 */
public class DocumentAnalysis {
    private final String documentId;
    private final String fileType;
    private final String text;
    private final int characterCount;
    private final int wordCount;
    private final boolean truncated;

    public DocumentAnalysis(String documentId, String fileType, String text,
                            int characterCount, int wordCount, boolean truncated) {
        this.documentId = documentId;
        this.fileType = fileType;
        this.text = text;
        this.characterCount = characterCount;
        this.wordCount = wordCount;
        this.truncated = truncated;
    }

    public String getDocumentId() { return documentId; }
    public String getFileType() { return fileType; }
    public String getText() { return text; }
    public int getCharacterCount() { return characterCount; }
    public int getWordCount() { return wordCount; }
    public boolean isTruncated() { return truncated; }
}
