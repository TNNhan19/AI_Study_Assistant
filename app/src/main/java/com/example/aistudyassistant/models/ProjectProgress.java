package com.example.aistudyassistant.models;

import java.util.ArrayList;
import java.util.List;

public class ProjectProgress {
    private int documentCount;
    private int summaryCount;
    private int flashcardCount;
    private QuizResult latestQuizResult;
    private String latestQuizDocumentName;
    private List<QuizResult> recentQuizResults = new ArrayList<>();

    public int getDocumentCount() { return documentCount; }
    public int getSummaryCount() { return summaryCount; }
    public int getFlashcardCount() { return flashcardCount; }
    public QuizResult getLatestQuizResult() { return latestQuizResult; }
    public String getLatestQuizDocumentName() { return latestQuizDocumentName; }
    public List<QuizResult> getRecentQuizResults() { return recentQuizResults; }

    public void setDocumentCount(int documentCount) {
        this.documentCount = documentCount;
    }

    public void setSummaryCount(int summaryCount) {
        this.summaryCount = summaryCount;
    }

    public void setFlashcardCount(int flashcardCount) {
        this.flashcardCount = flashcardCount;
    }

    public void setLatestQuizResult(QuizResult latestQuizResult) {
        this.latestQuizResult = latestQuizResult;
    }

    public void setLatestQuizDocumentName(String latestQuizDocumentName) {
        this.latestQuizDocumentName = latestQuizDocumentName;
    }

    public void setRecentQuizResults(List<QuizResult> recentQuizResults) {
        this.recentQuizResults = recentQuizResults == null
                ? new ArrayList<>()
                : recentQuizResults;
    }
}
