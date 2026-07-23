package com.example.aistudyassistant.models;

public class QuizResult {
    private String id;
    private String userId;
    private String quizId;
    private String documentId;
    private String projectId;
    private String documentName;
    private String projectName;
    private int score;
    private int totalQuestions;
    private int correctCount;
    private int wrongCount;
    private long completedAt;
    private long timeTaken;  // milliseconds
    private long createdAt;

    public QuizResult() {}

    public QuizResult(String documentId, int totalQuestions, int correctAnswers) {
        this.documentId = documentId;
        this.totalQuestions = totalQuestions;
        this.correctCount = correctAnswers;
        this.wrongCount = Math.max(0, totalQuestions - correctAnswers);
        this.score = correctAnswers;
        this.completedAt = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getQuizId() { return quizId; }
    public String getDocumentId() { return documentId; }
    public String getProjectId() { return projectId; }
    public String getDocumentName() { return documentName; }
    public String getProjectName() { return projectName; }
    public int getScore() { return score; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getCorrectCount() { return correctCount; }
    public int getWrongCount() { return wrongCount; }
    public int getCorrectAnswers() { return correctCount; }
    public int getWrongAnswers() { return wrongCount; }
    public long getCompletedAt() { return completedAt; }
    public long getTimeTaken() { return timeTaken; }
    public long getCreatedAt() { return createdAt; }

    /** Returns percentage score 0-100 */
    public int getScorePercent() {
        if (totalQuestions == 0) return 0;
        return (int) ((score * 100.0) / totalQuestions);
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
    public void setUserId(String userId) { this.userId = userId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public void setScore(int score) { this.score = score; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
    public void setWrongCount(int wrongCount) { this.wrongCount = wrongCount; }
    public void setCorrectAnswers(int correctAnswers) { this.correctCount = correctAnswers; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    public void setTimeTaken(long timeTaken) { this.timeTaken = timeTaken; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
