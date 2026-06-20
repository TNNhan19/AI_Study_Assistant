package com.example.aistudyassistant.models;

public class QuizQuestion {
    private String id;
    private String documentId;
    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctAnswer;  // "A", "B", "C", or "D"
    private String explanation;
    private int orderIndex;

    public QuizQuestion() {}

    public QuizQuestion(String question, String optionA, String optionB,
                        String optionC, String optionD,
                        String correctAnswer, String explanation) {
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
    }

    // Getters
    public String getId() { return id; }
    public String getDocumentId() { return documentId; }
    public String getQuestion() { return question; }
    public String getOptionA() { return optionA; }
    public String getOptionB() { return optionB; }
    public String getOptionC() { return optionC; }
    public String getOptionD() { return optionD; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getExplanation() { return explanation; }
    public int getOrderIndex() { return orderIndex; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setQuestion(String question) { this.question = question; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    /** Returns the option text for a given letter "A", "B", "C", "D" */
    public String getOptionByLetter(String letter) {
        switch (letter.toUpperCase()) {
            case "A": return optionA;
            case "B": return optionB;
            case "C": return optionC;
            case "D": return optionD;
            default: return "";
        }
    }
}
