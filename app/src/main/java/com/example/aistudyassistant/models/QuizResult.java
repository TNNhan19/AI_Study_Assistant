package com.example.aistudyassistant.models;

public class QuizResult {
    private String id;
    private String documentId;
    private String userId;
    private int totalQuestions;
    private int correctAnswers;
    private long timeTaken;  // milliseconds
    private long createdAt;

    public QuizResult() {}

    public QuizResult(String documentId, int totalQuestions, int correctAnswers) {
        this.documentId = documentId;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters
    public String getId() { return id; }
    public String getDocumentId() { return documentId; }
    public String getUserId() { return userId; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getWrongAnswers() { return totalQuestions - correctAnswers; }
    public long getTimeTaken() { return timeTaken; }
    public long getCreatedAt() { return createdAt; }

    /** Returns percentage score 0-100 */
    public int getScorePercent() {
        if (totalQuestions == 0) return 0;
        return (int) ((correctAnswers * 100.0) / totalQuestions);
    }

    /** Returns performance label based on score */
    public String getPerformanceLabel() {
        int score = getScorePercent();
        if (score >= 90) return "🎉 Excellent!";
        else if (score >= 70) return "👍 Good Job!";
        else if (score >= 50) return "📚 Keep Practicing!";
        else return "💪 Try Again!";
    }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    public void setTimeTaken(long timeTaken) { this.timeTaken = timeTaken; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
