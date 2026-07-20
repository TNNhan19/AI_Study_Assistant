package com.example.aistudyassistant.repositories;

import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.StudyStats;
import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyRepository {
    private static StudyRepository instance;
    private final SupabaseClient supabaseClient;

    private StudyRepository() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public static synchronized StudyRepository getInstance() {
        if (instance == null) {
            instance = new StudyRepository();
        }
        return instance;
    }

    public void getStudyStats(String userId, ApiCallback<StudyStats> callback) {
        new Thread(() -> {
            try {
                StudyStats stats = new StudyStats();
                String userFilter = "user_id=eq." + userId;

                // 1. Count Documents
                String docsResp = supabaseClient.getFromTable(Constants.TABLE_DOCUMENTS, userFilter + "&select=id");
                if (docsResp != null) stats.setTotalDocuments(JsonParser.parseString(docsResp).getAsJsonArray().size());

                // 2. Count Summaries
                String sumResp = supabaseClient.getFromTable(Constants.TABLE_SUMMARIES, userFilter + "&select=id");
                if (sumResp != null) stats.setTotalSummaries(JsonParser.parseString(sumResp).getAsJsonArray().size());

                // 3. Count Flashcards
                String flashResp = supabaseClient.getFromTable(Constants.TABLE_FLASHCARDS, userFilter + "&select=id");
                if (flashResp != null) stats.setTotalFlashcards(JsonParser.parseString(flashResp).getAsJsonArray().size());

                // 4. Quiz Analytics
                String quizResultsResp = supabaseClient.getFromTable(Constants.TABLE_QUIZ_RESULTS, userFilter);
                if (quizResultsResp != null) {
                    JsonArray results = JsonParser.parseString(quizResultsResp).getAsJsonArray();
                    stats.setTotalQuizzes(results.size());
                    
                    if (results.size() > 0) {
                        float totalScore = 0;
                        for (JsonElement el : results) {
                            JsonObject obj = el.getAsJsonObject();
                            int total = obj.get("total_questions").getAsInt();
                            int correct = obj.get("correct_answers").getAsInt();
                            totalScore += (correct * 100.0f / total);
                        }
                        stats.setAverageQuizScore(totalScore / results.size());
                    }
                }

                // 5. Weak Topics Logic
                // We fetch topics and their average quiz scores
                // For this, we'd need document -> topic mapping from quiz_results
                // Assuming quiz_results has document_id, and documents have topic_id
                fetchWeakTopics(userId, stats, callback);

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private void fetchWeakTopics(String userId, StudyStats stats, ApiCallback<StudyStats> callback) {
        try {
            // This is a simplified version:
            // Fetch all topics for user
            String topicsResp = supabaseClient.getFromTable(Constants.TABLE_TOPICS, "user_id=eq." + userId);
            if (topicsResp == null) {
                callback.onSuccess(stats);
                return;
            }

            JsonArray topicsArr = JsonParser.parseString(topicsResp).getAsJsonArray();
            Map<String, String> topicIdToName = new HashMap<>();
            for (JsonElement el : topicsArr) {
                JsonObject o = el.getAsJsonObject();
                topicIdToName.put(o.get("id").getAsString(), o.get("name").getAsString());
            }

            // Fetch all documents with their topic_id
            String docsResp = supabaseClient.getFromTable(Constants.TABLE_DOCUMENTS, "user_id=eq." + userId + "&select=id,topic_id");
            Map<String, String> docIdToTopicId = new HashMap<>();
            if (docsResp != null) {
                JsonArray docsArr = JsonParser.parseString(docsResp).getAsJsonArray();
                for (JsonElement el : docsArr) {
                    JsonObject o = el.getAsJsonObject();
                    if (o.has("topic_id") && !o.get("topic_id").isJsonNull()) {
                        docIdToTopicId.put(o.get("id").getAsString(), o.get("topic_id").getAsString());
                    }
                }
            }

            // Fetch all quiz results
            String resultsResp = supabaseClient.getFromTable(Constants.TABLE_QUIZ_RESULTS, "user_id=eq." + userId);
            Map<String, List<Float>> topicScores = new HashMap<>();

            if (resultsResp != null) {
                JsonArray resArr = JsonParser.parseString(resultsResp).getAsJsonArray();
                for (JsonElement el : resArr) {
                    JsonObject o = el.getAsJsonObject();
                    String docId = o.get("document_id").getAsString();
                    String topicId = docIdToTopicId.get(docId);
                    
                    if (topicId != null) {
                        int total = o.get("total_questions").getAsInt();
                        int correct = o.get("correct_answers").getAsInt();
                        float score = (correct * 100.0f / total);
                        
                        if (!topicScores.containsKey(topicId)) topicScores.put(topicId, new ArrayList<>());
                        topicScores.get(topicId).add(score);
                    }
                }
            }

            List<StudyStats.TopicPerformance> performanceList = new ArrayList<>();
            for (Map.Entry<String, List<Float>> entry : topicScores.entrySet()) {
                float sum = 0;
                for (float s : entry.getValue()) sum += s;
                float avg = sum / entry.getValue().size();
                
                // Only consider as "weak" if average score < 60%
                if (avg < 60) {
                    performanceList.add(new StudyStats.TopicPerformance(entry.getKey(), topicIdToName.get(entry.getKey()), avg));
                }
            }
            
            // Sort by score ascending (weakest first)
            performanceList.sort((p1, p2) -> Float.compare(p1.getAverageScore(), p2.getAverageScore()));
            stats.setWeakTopics(performanceList);

            callback.onSuccess(stats);
        } catch (Exception e) {
            callback.onSuccess(stats); // Return what we have even if weak topics fail
        }
    }
}
