package com.example.aistudyassistant.repositories;

import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.SearchResult;
import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchRepository {

    private static final String GLOBAL_SEARCH_RPC = "global_search";
    private static final int MAX_RESULTS_PER_TYPE = 5;
    private static final Object SEARCH_REQUEST_TAG = new Object();

    private static SearchRepository instance;
    private final SupabaseClient supabaseClient;
    private final AtomicInteger searchGeneration = new AtomicInteger();
    private volatile boolean rpcUnavailable;

    private SearchRepository() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public static synchronized SearchRepository getInstance() {
        if (instance == null) {
            instance = new SearchRepository();
        }
        return instance;
    }

    public void globalSearch(
            String userId,
            String query,
            ApiCallback<List<SearchResult>> callback) {
        int generation = searchGeneration.incrementAndGet();
        supabaseClient.cancelRequestsWithTag(SEARCH_REQUEST_TAG);

        new Thread(() -> {
            try {
                if (!isCurrent(generation)) return;
                String normalizedQuery = query == null ? "" : query.trim();
                if (normalizedQuery.length() < 2) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }

                if (!rpcUnavailable) {
                    String rpcResponse = callGlobalSearchRpc(normalizedQuery);
                    if (!isCurrent(generation)) return;
                    List<SearchResult> rpcResults =
                            parseRpcResults(rpcResponse);
                    if (rpcResults != null) {
                        callback.onSuccess(rpcResults);
                        return;
                    }

                    if (isMissingRpc(rpcResponse)) {
                        rpcUnavailable = true;
                    } else {
                        callback.onError("Search service is temporarily unavailable");
                        return;
                    }
                }

                List<SearchResult> fallbackResults =
                        legacySearch(userId, normalizedQuery, generation);
                if (isCurrent(generation)) {
                    callback.onSuccess(fallbackResults);
                }
            } catch (Exception e) {
                if (isCurrent(generation)) {
                    callback.onError(
                            e.getMessage() == null
                                    ? "Search failed"
                                    : e.getMessage());
                }
            }
        }).start();
    }

    public void cancelActiveSearch() {
        searchGeneration.incrementAndGet();
        supabaseClient.cancelRequestsWithTag(SEARCH_REQUEST_TAG);
    }

    private boolean isCurrent(int generation) {
        return generation == searchGeneration.get();
    }

    private String callGlobalSearchRpc(String query) {
        JsonObject payload = new JsonObject();
        payload.addProperty("p_query", query);
        payload.addProperty(
                "p_limit_per_type", MAX_RESULTS_PER_TYPE);
        return supabaseClient.callRpc(
                GLOBAL_SEARCH_RPC,
                payload.toString(),
                SEARCH_REQUEST_TAG);
    }

    static List<SearchResult> parseRpcResults(String response) {
        if (response == null || response.trim().isEmpty()) return null;
        try {
            JsonElement root = JsonParser.parseString(response);
            if (!root.isJsonArray()) return null;

            List<SearchResult> results = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray()) {
                JsonObject object = element.getAsJsonObject();
                SearchResult.Type type = SearchResult.Type.valueOf(
                        object.get("result_type").getAsString());
                results.add(new SearchResult(
                        object.get("result_id").getAsString(),
                        readString(object, "result_title"),
                        readString(object, "result_subtitle"),
                        type,
                        null
                ));
            }
            return results;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isMissingRpc(String response) {
        if (response == null) return false;
        return response.contains("PGRST202")
                || response.contains("42883")
                || response.contains("Could not find the function");
    }

    private List<SearchResult> legacySearch(
            String userId,
            String query,
            int generation) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        String encodedQuery = URLEncoder.encode(query, "UTF-8")
                .replace("+", "%20");
        String ilikeQuery = "ilike.*" + encodedQuery + "*";

        searchInTable(
                Constants.TABLE_DOCUMENTS,
                userId,
                "name=" + ilikeQuery,
                "id,name,file_type",
                SearchResult.Type.DOCUMENT,
                results,
                generation);
        searchInTable(
                Constants.TABLE_PROJECTS,
                userId,
                "or=(name." + ilikeQuery
                        + ",description." + ilikeQuery + ")",
                "id,name,description",
                SearchResult.Type.PROJECT,
                results,
                generation);
        searchInTable(
                Constants.TABLE_TOPICS,
                userId,
                "or=(name." + ilikeQuery
                        + ",description." + ilikeQuery + ")",
                "id,name,description",
                SearchResult.Type.TOPIC,
                results,
                generation);
        searchInTable(
                Constants.TABLE_NOTES,
                userId,
                "or=(title." + ilikeQuery
                        + ",content." + ilikeQuery + ")",
                "id,title,content",
                SearchResult.Type.NOTE,
                results,
                generation);
        searchInTable(
                Constants.TABLE_FLASHCARDS,
                userId,
                "or=(front." + ilikeQuery
                        + ",back." + ilikeQuery + ")",
                "id,document_id,front,back",
                SearchResult.Type.FLASHCARD,
                results,
                generation);
        searchInTable(
                Constants.TABLE_QUIZZES,
                userId,
                "or=(question." + ilikeQuery
                        + ",explanation." + ilikeQuery + ")",
                "id,document_id,question,explanation",
                SearchResult.Type.QUIZ,
                results,
                generation);
        searchInSummaries(
                userId, ilikeQuery, results, generation);
        return results;
    }

    private void searchInTable(
            String table,
            String userId,
            String filter,
            String columns,
            SearchResult.Type type,
            List<SearchResult> results,
            int generation) {
        if (!isCurrent(generation)) return;
        String queryParams = "user_id=eq." + userId
                + "&" + filter
                + "&select=" + columns
                + "&limit=" + MAX_RESULTS_PER_TYPE;
        String response = supabaseClient.getFromTable(
                table, queryParams);
        if (response == null) return;

        JsonElement root = JsonParser.parseString(response);
        if (!root.isJsonArray()) return;

        for (JsonElement element : root.getAsJsonArray()) {
            JsonObject object = element.getAsJsonObject();
            String id = getNavigationId(object, type);
            String title = readFirstString(
                    object, "name", "title", "front", "question");
            String subtitle = readFirstString(
                    object,
                    "description",
                    "content",
                    "back",
                    "explanation",
                    "file_type");

            results.add(new SearchResult(
                    id, title, subtitle, type, null));
        }
    }

    private String getNavigationId(
            JsonObject object, SearchResult.Type type) {
        if ((type == SearchResult.Type.FLASHCARD
                || type == SearchResult.Type.QUIZ)
                && object.has("document_id")
                && !object.get("document_id").isJsonNull()) {
            return object.get("document_id").getAsString();
        }
        return object.get("id").getAsString();
    }

    private void searchInSummaries(
            String userId,
            String ilikeQuery,
            List<SearchResult> results,
            int generation) {
        if (!isCurrent(generation)) return;
        String queryParams = "user_id=eq." + userId
                + "&summary_text=" + ilikeQuery
                + "&select=document_id,summary_text"
                + "&limit=" + MAX_RESULTS_PER_TYPE;
        String response = supabaseClient.getFromTable(
                Constants.TABLE_SUMMARIES, queryParams);
        if (response == null) return;

        JsonElement root = JsonParser.parseString(response);
        if (!root.isJsonArray()) return;

        JsonArray array = root.getAsJsonArray();
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            results.add(new SearchResult(
                    object.get("document_id").getAsString(),
                    "Match in Summary",
                    readString(object, "summary_text"),
                    SearchResult.Type.DOCUMENT,
                    null
            ));
        }
    }

    private static String readFirstString(
            JsonObject object, String... fields) {
        for (String field : fields) {
            String value = readString(object, field);
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private static String readString(
            JsonObject object, String field) {
        return object.has(field) && !object.get(field).isJsonNull()
                ? object.get(field).getAsString()
                : "";
    }
}
