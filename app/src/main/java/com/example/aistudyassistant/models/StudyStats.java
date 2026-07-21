package com.example.aistudyassistant.models;

import java.util.List;

public class StudyStats {
    private int totalDocuments;
    private int totalSummaries;
    private int totalFlashcards;
    private int totalQuizzes;
    private float averageQuizScore;
    private List<TopicPerformance> weakTopics;

    public static class TopicPerformance {
        private String topicId;
        private String topicName;
        private float averageScore;

        public TopicPerformance(String topicId, String topicName, float averageScore) {
            this.topicId = topicId;
            this.topicName = topicName;
            this.averageScore = averageScore;
        }

        public String getTopicId() { return topicId; }
        public String getTopicName() { return topicName; }
        public float getAverageScore() { return averageScore; }
    }

    public int getTotalDocuments() { return totalDocuments; }
    public void setTotalDocuments(int totalDocuments) { this.totalDocuments = totalDocuments; }

    public int getTotalSummaries() { return totalSummaries; }
    public void setTotalSummaries(int totalSummaries) { this.totalSummaries = totalSummaries; }

    public int getTotalFlashcards() { return totalFlashcards; }
    public void setTotalFlashcards(int totalFlashcards) { this.totalFlashcards = totalFlashcards; }

    public int getTotalQuizzes() { return totalQuizzes; }
    public void setTotalQuizzes(int totalQuizzes) { this.totalQuizzes = totalQuizzes; }

    public float getAverageQuizScore() { return averageQuizScore; }
    public void setAverageQuizScore(float averageQuizScore) { this.averageQuizScore = averageQuizScore; }

    public List<TopicPerformance> getWeakTopics() { return weakTopics; }
    public void setWeakTopics(List<TopicPerformance> weakTopics) { this.weakTopics = weakTopics; }
}
