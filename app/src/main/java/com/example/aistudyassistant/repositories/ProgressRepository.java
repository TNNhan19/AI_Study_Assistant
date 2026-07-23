package com.example.aistudyassistant.repositories;

import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.ProjectProgress;
import com.example.aistudyassistant.models.QuizResult;
import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ProgressRepository {
    private static ProgressRepository instance;
    private final SupabaseClient supabaseClient;

    private ProgressRepository() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public static synchronized ProgressRepository getInstance() {
        if (instance == null) {
            instance = new ProgressRepository();
        }
        return instance;
    }

    public void getProjectProgress(String userId, String projectId,
                                   ApiCallback<ProjectProgress> callback) {
        new Thread(() -> {
            try {
                validateInput(userId, projectId);
                Map<String, String> documents = getDocumentNamesByProjectBlocking(
                        userId, projectId);
                List<String> documentIds = new ArrayList<>(documents.keySet());

                ProjectProgress progress = new ProjectProgress();
                progress.setDocumentCount(documentIds.size());
                progress.setSummaryCount(countByDocuments(
                        Constants.TABLE_SUMMARIES, userId, documentIds));
                progress.setFlashcardCount(countByDocuments(
                        Constants.TABLE_FLASHCARDS, userId, documentIds));

                List<QuizResult> recent = getRecentQuizResultsByProjectBlocking(
                        userId, projectId, documentIds, 5);
                progress.setRecentQuizResults(toOldestFirst(recent));
                if (!recent.isEmpty()) {
                    QuizResult latest = recent.get(0);
                    progress.setLatestQuizResult(latest);
                    progress.setLatestQuizDocumentName(documents.get(latest.getDocumentId()));
                }
                callback.onSuccess(progress);
            } catch (Exception error) {
                callback.onError(readError(error, "Could not load study progress"));
            }
        }).start();
    }

    public void getDocumentCountByProject(String userId, String projectId,
                                          ApiCallback<Integer> callback) {
        new Thread(() -> {
            try {
                callback.onSuccess(getDocumentNamesByProjectBlocking(
                        userId, projectId).size());
            } catch (Exception error) {
                callback.onError(readError(error, "Could not count documents"));
            }
        }).start();
    }

    public void getSummaryCountByProject(String userId, String projectId,
                                         ApiCallback<Integer> callback) {
        new Thread(() -> {
            try {
                List<String> documentIds = new ArrayList<>(
                        getDocumentNamesByProjectBlocking(userId, projectId).keySet());
                callback.onSuccess(countByDocuments(
                        Constants.TABLE_SUMMARIES, userId, documentIds));
            } catch (Exception error) {
                callback.onError(readError(error, "Could not count summaries"));
            }
        }).start();
    }

    public void getFlashcardCountByProject(String userId, String projectId,
                                           ApiCallback<Integer> callback) {
        new Thread(() -> {
            try {
                List<String> documentIds = new ArrayList<>(
                        getDocumentNamesByProjectBlocking(userId, projectId).keySet());
                callback.onSuccess(countByDocuments(
                        Constants.TABLE_FLASHCARDS, userId, documentIds));
            } catch (Exception error) {
                callback.onError(readError(error, "Could not count flashcards"));
            }
        }).start();
    }

    public void getLatestQuizResultByProject(String userId, String projectId,
                                             ApiCallback<QuizResult> callback) {
        new Thread(() -> {
            try {
                List<String> documentIds = new ArrayList<>(
                        getDocumentNamesByProjectBlocking(userId, projectId).keySet());
                List<QuizResult> results = getRecentQuizResultsByProjectBlocking(
                        userId, projectId, documentIds, 1);
                callback.onSuccess(results.isEmpty() ? null : results.get(0));
            } catch (Exception error) {
                callback.onError(readError(error, "Could not load latest quiz result"));
            }
        }).start();
    }

    public void getRecentQuizResultsByProject(String userId, String projectId, int limit,
                                              ApiCallback<List<QuizResult>> callback) {
        new Thread(() -> {
            try {
                List<String> documentIds = new ArrayList<>(
                        getDocumentNamesByProjectBlocking(userId, projectId).keySet());
                callback.onSuccess(getRecentQuizResultsByProjectBlocking(
                        userId, projectId, documentIds, limit));
            } catch (Exception error) {
                callback.onError(readError(error, "Could not load quiz performance"));
            }
        }).start();
    }

    private Map<String, String> getDocumentNamesByProjectBlocking(
            String userId, String projectId) {
        validateInput(userId, projectId);
        String query = "user_id=eq." + userId
                + "&project_id=eq." + projectId
                + "&select=id,name"
                + "&order=created_at.desc";
        JsonArray rows = parseRows(
                supabaseClient.getFromTable(Constants.TABLE_DOCUMENTS, query));
        Map<String, String> documents = new LinkedHashMap<>();
        for (JsonElement row : rows) {
            JsonObject json = row.getAsJsonObject();
            String id = readString(json, "id");
            if (!isBlank(id)) documents.put(id, readString(json, "name"));
        }
        return documents;
    }

    private int countByDocuments(String table, String userId, List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) return 0;
        String query = "user_id=eq." + userId
                + "&document_id=in.(" + joinIds(documentIds) + ")"
                + "&select=id";
        return parseRows(supabaseClient.getFromTable(table, query)).size();
    }

    private List<QuizResult> getRecentQuizResultsByProjectBlocking(
            String userId, String projectId, List<String> documentIds, int limit) {
        Map<String, QuizResult> merged = new HashMap<>();
        String projectQuery = "user_id=eq." + userId
                + "&project_id=eq." + projectId
                + "&order=completed_at.desc"
                + "&limit=" + Math.max(limit, 5);
        addQuizResults(merged, supabaseClient.getFromTable(
                Constants.TABLE_QUIZ_RESULTS, projectQuery));

        if (documentIds != null && !documentIds.isEmpty()) {
            String documentQuery = "user_id=eq." + userId
                    + "&document_id=in.(" + joinIds(documentIds) + ")"
                    + "&order=completed_at.desc"
                    + "&limit=100";
            addQuizResults(merged, supabaseClient.getFromTable(
                    Constants.TABLE_QUIZ_RESULTS, documentQuery));
        }

        List<QuizResult> results = new ArrayList<>(merged.values());
        results.sort((first, second) ->
                Long.compare(second.getCompletedAt(), first.getCompletedAt()));
        if (results.size() > limit) {
            return new ArrayList<>(results.subList(0, limit));
        }
        return results;
    }

    private void addQuizResults(Map<String, QuizResult> target, String response) {
        JsonArray rows;
        try {
            rows = parseRows(response);
        } catch (Exception ignored) {
            return;
        }
        for (JsonElement row : rows) {
            QuizResult result = parseQuizResult(row.getAsJsonObject());
            String key = isBlank(result.getId())
                    ? result.getDocumentId() + ":" + result.getCompletedAt()
                    : result.getId();
            target.put(key, result);
        }
    }

    private List<QuizResult> toOldestFirst(List<QuizResult> newestFirst) {
        List<QuizResult> copy = new ArrayList<>(newestFirst);
        Collections.reverse(copy);
        return copy;
    }

    private QuizResult parseQuizResult(JsonObject json) {
        QuizResult result = new QuizResult();
        result.setId(readString(json, "id"));
        result.setUserId(readString(json, "user_id"));
        result.setQuizId(readString(json, "quiz_id"));
        result.setQuizSetId(readString(json, "quiz_set_id"));
        result.setDocumentId(readString(json, "document_id"));
        result.setProjectId(readString(json, "project_id"));
        result.setTotalQuestions(readInt(json, "total_questions"));
        int correct = json.has("correct_count")
                ? readInt(json, "correct_count")
                : readInt(json, "correct_answers");
        result.setScore(json.has("score") ? readInt(json, "score") : correct);
        result.setCorrectCount(correct);
        result.setWrongCount(json.has("wrong_count")
                ? readInt(json, "wrong_count")
                : Math.max(0, result.getTotalQuestions() - correct));
        result.setCompletedAt(parseTimestamp(json.has("completed_at")
                ? readString(json, "completed_at")
                : readString(json, "created_at")));
        return result;
    }

    private JsonArray parseRows(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new IllegalStateException("No response from server");
        }

        JsonElement root = JsonParser.parseString(response);
        if (root.isJsonArray()) return root.getAsJsonArray();
        if (root.isJsonObject()) {
            String message = readString(root.getAsJsonObject(), "message");
            throw new IllegalStateException(
                    isBlank(message) ? "Invalid server response" : message);
        }
        throw new IllegalStateException("Invalid server response");
    }

    private String joinIds(List<String> ids) {
        StringBuilder builder = new StringBuilder();
        for (String id : ids) {
            if (isBlank(id)) continue;
            if (builder.length() > 0) builder.append(',');
            builder.append(id);
        }
        return builder.toString();
    }

    private String readString(JsonObject json, String key) {
        return !json.has(key) || json.get(key).isJsonNull()
                ? ""
                : json.get(key).getAsString();
    }

    private int readInt(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) return 0;
        try {
            return json.get(key).getAsInt();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private long parseTimestamp(String value) {
        if (isBlank(value)) return 0;
        value = normalizeTimestampFraction(value.trim());
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                return format.parse(value).getTime();
            } catch (Exception ignored) {
                // Try next format.
            }
        }
        return 0;
    }

    private String normalizeTimestampFraction(String value) {
        int dotIndex = value.indexOf('.');
        if (dotIndex < 0) return value;

        int fractionStart = dotIndex + 1;
        int fractionEnd = fractionStart;
        while (fractionEnd < value.length()
                && Character.isDigit(value.charAt(fractionEnd))) {
            fractionEnd++;
        }

        String fraction = value.substring(fractionStart, fractionEnd);
        if (fraction.length() > 3) {
            fraction = fraction.substring(0, 3);
        } else {
            while (fraction.length() < 3) fraction += "0";
        }
        return value.substring(0, fractionStart)
                + fraction
                + value.substring(fractionEnd);
    }

    private void validateInput(String userId, String projectId) {
        if (isBlank(userId) || isBlank(projectId)) {
            throw new IllegalArgumentException("No project selected");
        }
    }

    private String readError(Exception error, String fallback) {
        return error.getMessage() == null || error.getMessage().trim().isEmpty()
                ? fallback
                : error.getMessage();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
