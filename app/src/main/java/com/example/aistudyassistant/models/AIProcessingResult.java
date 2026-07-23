package com.example.aistudyassistant.models;

import java.util.List;

/**
 * Gom kết quả khi xử lý đầy đủ một tài liệu trong một lần.
 */
public class AIProcessingResult {
    private final DocumentAnalysis analysis;
    private final Summary summary;
    private final List<QuizQuestion> quizQuestions;
    private final List<Flashcard> flashcards;

    public AIProcessingResult(DocumentAnalysis analysis, Summary summary,
                              List<QuizQuestion> quizQuestions,
                              List<Flashcard> flashcards) {
        this.analysis = analysis;
        this.summary = summary;
        this.quizQuestions = quizQuestions;
        this.flashcards = flashcards;
    }

    public DocumentAnalysis getAnalysis() { return analysis; }
    public Summary getSummary() { return summary; }
    public List<QuizQuestion> getQuizQuestions() { return quizQuestions; }
    public List<Flashcard> getFlashcards() { return flashcards; }
}
