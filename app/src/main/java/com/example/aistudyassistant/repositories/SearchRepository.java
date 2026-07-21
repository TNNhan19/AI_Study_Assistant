package com.example.aistudyassistant.repositories;

import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.SearchResult;
import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class SearchRepository {
    private static SearchRepository instance;
    private final SupabaseClient supabaseClient;

    private SearchRepository() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public static synchronized SearchRepository getInstance() {
        if (instance == null) {
            instance = new SearchRepository();
        }
        return instance;
    }

    public void globalSearch(String userId, String query, ApiCallback<List<SearchResult>> callback) {
        new Thread(() -> {
            try {
                List<SearchResult> results = new ArrayList<>();
                String ilikeQuery = "ilike.*" + query + "*";

                // 1. Search Documents (by name)
                searchInTable(Constants.TABLE_DOCUMENTS, userId, "name=" + ilikeQuery, SearchResult.Type.DOCUMENT, results);

                // 2. Search Projects (by name or description)
                // Note: Complex OR queries are harder in simple GET, so we search by name for now
                searchInTable(Constants.TABLE_PROJECTS, userId, "name=" + ilikeQuery, SearchResult.Type.PROJECT, results);

                // 3. Search Topics (by name)
                searchInTable(Constants.TABLE_TOPICS, userId, "name=" + ilikeQuery, SearchResult.Type.TOPIC, results);

                // 4. Search Notes (by title or content)
                searchInTable(Constants.TABLE_NOTES, userId, "title=" + ilikeQuery, SearchResult.Type.NOTE, results);

                // 5. Search Flashcards (by front)
                searchInTable(Constants.TABLE_FLASHCARDS, userId, "front=" + ilikeQuery, SearchResult.Type.FLASHCARD, results);

                // 6. Search Quizzes (by question)
                searchInTable(Constants.TABLE_QUIZZES, userId, "question=" + ilikeQuery, SearchResult.Type.QUIZ, results);

                // 7. Search in Summaries content
                searchInSummaries(userId, ilikeQuery, results);

                callback.onSuccess(results);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private void searchInTable(String table, String userId, String filter, SearchResult.Type type, List<SearchResult> results) {
        String queryParams = "user_id=eq." + userId + "&" + filter;
        String response = supabaseClient.getFromTable(table, queryParams);
        if (response != null) {
            JsonArray array = JsonParser.parseString(response).getAsJsonArray();
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String title = "";
                String subtitle = "";

                if (obj.has("name")) title = obj.get("name").getAsString();
                else if (obj.has("title")) title = obj.get("title").getAsString();
                else if (obj.has("front")) title = obj.get("front").getAsString();
                else if (obj.has("question")) title = obj.get("question").getAsString();

                if (obj.has("description")) subtitle = obj.get("description").getAsString();
                else if (obj.has("content")) subtitle = obj.get("content").getAsString();
                else if (obj.has("back")) subtitle = obj.get("back").getAsString();

                results.add(new SearchResult(id, title, subtitle, type, null));
            }
        }
    }

    private void searchInSummaries(String userId, String ilikeQuery, List<SearchResult> results) {
        String queryParams = "user_id=eq." + userId + "&summary_text=" + ilikeQuery;
        String response = supabaseClient.getFromTable(Constants.TABLE_SUMMARIES, queryParams);
        if (response != null) {
            JsonArray array = JsonParser.parseString(response).getAsJsonArray();
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                String docId = obj.get("document_id").getAsString();
                String content = obj.get("summary_text").getAsString();
                // For summary results, we point to the document but label it as a summary match
                results.add(new SearchResult(docId, "Match in Summary", content, SearchResult.Type.DOCUMENT, null));
            }
        }
    }
}
