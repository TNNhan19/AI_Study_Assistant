package com.example.aistudyassistant.repositories;

import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.QuizResult;
import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class QuizRepository {

    private static QuizRepository instance;
    private final SupabaseClient supabaseClient;

    private QuizRepository() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public static synchronized QuizRepository getInstance() {
        if (instance == null) {
            instance = new QuizRepository();
        }
        return instance;
    }

    public void saveQuizResult(QuizResult result, ApiCallback<QuizResult> callback) {
        new Thread(() -> {
            try {
                validateResult(result);
                if (result.getCompletedAt() <= 0) {
                    result.setCompletedAt(System.currentTimeMillis());
                }

                String response = supabaseClient.insertIntoTable(
                        Constants.TABLE_QUIZ_RESULTS,
                        buildQuizResultJson(result, true).toString()
                );
                if (response == null) {
                    callback.onError("Could not save quiz result");
                    return;
                }

                JsonArray rows;
                try {
                    rows = parseRows(response);
                } catch (Exception answerDataSchemaError) {
                    String modernResponse = supabaseClient.insertIntoTable(
                            Constants.TABLE_QUIZ_RESULTS,
                            buildQuizResultJson(result, false).toString()
                    );
                    try {
                        rows = parseRows(modernResponse);
                    } catch (Exception modernSchemaError) {
                        String legacyResponse = supabaseClient.insertIntoTable(
                                Constants.TABLE_QUIZ_RESULTS,
                                buildLegacyQuizResultJson(result).toString()
                        );
                        if (legacyResponse == null) {
                            callback.onError(readError(
                                    modernSchemaError, "Could not save quiz result"));
                            return;
                        }
                        rows = parseRows(legacyResponse);
                    }
                }
                if (rows.size() == 0) {
                    callback.onError("Could not read saved quiz result");
                    return;
                }
                callback.onSuccess(parseQuizResult(rows.get(0).getAsJsonObject()));
            } catch (Exception error) {
                callback.onError(readError(error, "Could not save quiz result"));
            }
        }).start();
    }

    public void getLatestQuizResult(String userId, ApiCallback<QuizResult> callback) {
        new Thread(() -> {
            try {
                if (isBlank(userId)) {
                    callback.onSuccess(null);
                    return;
                }

                String query = "user_id=eq." + userId
                        + "&order=completed_at.desc"
                        + "&limit=1";
                String response = supabaseClient.getFromTable(Constants.TABLE_QUIZ_RESULTS, query);
                if (response == null) {
                    callback.onSuccess(null);
                    return;
                }

                JsonArray rows;
                try {
                    rows = parseRows(response);
                } catch (Exception modernSchemaError) {
                    String legacyQuery = "user_id=eq." + userId
                            + "&order=created_at.desc"
                            + "&limit=1";
                    String legacyResponse = supabaseClient.getFromTable(
                            Constants.TABLE_QUIZ_RESULTS, legacyQuery);
                    if (legacyResponse == null) {
                        callback.onSuccess(null);
                        return;
                    }
                    rows = parseRows(legacyResponse);
                }
                callback.onSuccess(rows.size() == 0
                        ? null
                        : parseQuizResult(rows.get(0).getAsJsonObject()));
            } catch (Exception error) {
                callback.onSuccess(null);
            }
        }).start();
    }

    public void getQuizResults(String userId, ApiCallback<List<QuizResult>> callback) {
        new Thread(() -> {
            try {
                if (isBlank(userId)) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }

                String query = "user_id=eq." + userId
                        + "&order=completed_at.desc"
                        + "&limit=100";
                String response = supabaseClient.getFromTable(Constants.TABLE_QUIZ_RESULTS, query);
                if (response == null) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }

                JsonArray rows;
                try {
                    rows = parseRows(response);
                } catch (Exception modernSchemaError) {
                    String legacyQuery = "user_id=eq." + userId
                            + "&order=created_at.desc"
                            + "&limit=100";
                    String legacyResponse = supabaseClient.getFromTable(
                            Constants.TABLE_QUIZ_RESULTS, legacyQuery);
                    if (legacyResponse == null) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }
                    rows = parseRows(legacyResponse);
                }

                List<QuizResult> results = new ArrayList<>();
                for (JsonElement row : rows) {
                    results.add(parseQuizResult(row.getAsJsonObject()));
                }
                callback.onSuccess(results);
            } catch (Exception error) {
                callback.onError(readError(error, "Could not load quiz results"));
            }
        }).start();
    }

    private JsonObject buildQuizResultJson(QuizResult result, boolean includeReviewData) {
        JsonObject json = new JsonObject();
        json.addProperty("user_id", result.getUserId());
        addOptionalString(json, "quiz_id", result.getQuizId());
        if (includeReviewData) {
            addOptionalString(json, "quiz_set_id", result.getQuizSetId());
        }
        addOptionalString(json, "document_id", result.getDocumentId());
        addOptionalString(json, "project_id", result.getProjectId());
        json.addProperty("score", result.getScore());
        json.addProperty("total_questions", result.getTotalQuestions());
        json.addProperty("correct_count", result.getCorrectCount());
        json.addProperty("wrong_count", result.getWrongCount());
        json.addProperty("completed_at", formatTimestamp(result.getCompletedAt()));
        if (includeReviewData && !isBlank(result.getAnswerData())) {
            json.add("answer_data", JsonParser.parseString(result.getAnswerData()));
        }
        return json;
    }

    private JsonObject buildLegacyQuizResultJson(QuizResult result) {
        JsonObject json = new JsonObject();
        json.addProperty("user_id", result.getUserId());
        addOptionalString(json, "document_id", result.getDocumentId());
        json.addProperty("total_questions", result.getTotalQuestions());
        json.addProperty("correct_answers", result.getCorrectCount());
        return json;
    }

    private QuizResult parseQuizResult(JsonObject json) {
        QuizResult result = new QuizResult();
        result.setId(readString(json, "id"));
        result.setUserId(readString(json, "user_id"));
        result.setQuizId(readString(json, "quiz_id"));
        result.setQuizSetId(readString(json, "quiz_set_id"));
        result.setDocumentId(readString(json, "document_id"));
        result.setProjectId(readString(json, "project_id"));
        result.setAnswerData(readJsonString(json, "answer_data"));
        result.setTotalQuestions(readInt(json, "total_questions"));
        int correct = json.has("correct_count")
                ? readInt(json, "correct_count")
                : readInt(json, "correct_answers");
        int score = json.has("score") ? readInt(json, "score") : correct;
        int wrong = json.has("wrong_count")
                ? readInt(json, "wrong_count")
                : Math.max(0, result.getTotalQuestions() - correct);

        result.setScore(score);
        result.setCorrectCount(correct);
        result.setWrongCount(wrong);
        result.setCompletedAt(parseTimestamp(json.has("completed_at")
                ? readString(json, "completed_at")
                : readString(json, "created_at")));
        return result;
    }

    private JsonArray parseRows(String response) {
        JsonElement root = JsonParser.parseString(response);
        if (root.isJsonArray()) return root.getAsJsonArray();
        if (root.isJsonObject()) {
            JsonObject error = root.getAsJsonObject();
            String message = readString(error, "message");
            if (!isBlank(message)) throw new IllegalStateException(message);
        }
        throw new IllegalStateException("Invalid Supabase response");
    }

    private void validateResult(QuizResult result) {
        if (result == null || isBlank(result.getUserId())
                || result.getTotalQuestions() <= 0
                || result.getCorrectCount() < 0
                || result.getWrongCount() < 0) {
            throw new IllegalArgumentException("Quiz result is missing required data");
        }
    }

    private void addOptionalString(JsonObject json, String key, String value) {
        if (!isBlank(value)) {
            json.addProperty(key, value);
        }
    }

    private String readString(JsonObject json, String key) {
        return !json.has(key) || json.get(key).isJsonNull()
                ? ""
                : json.get(key).getAsString();
    }

    private String readJsonString(JsonObject json, String key) {
        return !json.has(key) || json.get(key).isJsonNull()
                ? ""
                : json.get(key).toString();
    }

    private int readInt(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) return 0;
        try {
            return json.get(key).getAsInt();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String formatTimestamp(long millis) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(millis);
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
                // Try the next supported Supabase timestamp shape.
            }
        }
        return 0;
    }

    private String normalizeTimestampFraction(String value) {
        int dotIndex = value.indexOf('.');
        if (dotIndex < 0) return value;

        int fractionStart = dotIndex + 1;
        int fractionEnd = fractionStart;
        while (fractionEnd < value.length() && Character.isDigit(value.charAt(fractionEnd))) {
            fractionEnd++;
        }

        String fraction = value.substring(fractionStart, fractionEnd);
        if (fraction.length() > 3) {
            fraction = fraction.substring(0, 3);
        } else {
            while (fraction.length() < 3) {
                fraction += "0";
            }
        }

        return value.substring(0, fractionStart) + fraction + value.substring(fractionEnd);
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
