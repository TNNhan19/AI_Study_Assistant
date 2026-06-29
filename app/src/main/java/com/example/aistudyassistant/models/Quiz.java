package com.example.aistudyassistant.models;

public class Quiz {
    // Vì database của mình đang dùng bảng quizzes để lưu từng câu hỏi quiz, model Quiz có thể đại diện cho 1 câu hỏi.
    private String id;
    private String userId;
    private String documentId;
    private String topicId;
    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctAnswer;
    private String explanation;
    private String difficulty;
    private String createdAt;

    public Quiz() {}

    public Quiz(String question, String optionA, String optionB, String optionC,
                String optionD, String correctAnswer) {
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctAnswer = correctAnswer;
        this.difficulty = "MEDIUM";
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getDocumentId() { return documentId; }
    public String getTopicId() { return topicId; }
    public String getQuestion() { return question; }
    public String getOptionA() { return optionA; }
    public String getOptionB() { return optionB; }
    public String getOptionC() { return optionC; }
    public String getOptionD() { return optionD; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getExplanation() { return explanation; }
    public String getDifficulty() { return difficulty; }
    public String getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }
    public void setQuestion(String question) { this.question = question; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
