package com.example.aistudyassistant.repositories;

import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.models.QuizQuestion;
import com.example.aistudyassistant.models.Summary;
import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class AIContentRepository {

    private static AIContentRepository instance;
    private final SupabaseClient supabaseClient;

    private AIContentRepository() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public static synchronized AIContentRepository getInstance() {
        if (instance == null) {
            instance = new AIContentRepository();
        }
        return instance;
    }

    /**
     * Lấy bản tóm tắt mới nhất của một tài liệu thuộc user hiện tại.
     */
    public void getSummaryByDocument(String userId, String documentId,
                                     ApiCallback<Summary> callback) {
        new Thread(() -> {
            try {
                if (isBlank(userId) || isBlank(documentId)) {
                    callback.onError("Thiếu thông tin user hoặc tài liệu");
                    return;
                }

                String query = "user_id=eq." + userId
                        + "&document_id=eq." + documentId
                        + "&order=created_at.desc&limit=1";
                String response = supabaseClient.getFromTable(
                        Constants.TABLE_SUMMARIES, query);
                if (response == null) {
                    callback.onError("Không thể tải bản tóm tắt");
                    return;
                }

                JsonArray rows = parseRows(response, "Không thể tải bản tóm tắt");
                callback.onSuccess(rows.size() == 0
                        ? null
                        : parseSummary(rows.get(0).getAsJsonObject()));
            } catch (Exception error) {
                callback.onError(readError(error, "Không thể đọc bản tóm tắt"));
            }
        }).start();
    }

    /**
     * Insert summary mới hoặc update record đã có id.
     */
    public void saveSummary(Summary summary, ApiCallback<Summary> callback) {
        new Thread(() -> {
            try {
                validateSummary(summary);
                JsonObject json = buildSummaryJson(summary);
                String response;

                if (isBlank(summary.getId())) {
                    response = supabaseClient.insertIntoTable(
                            Constants.TABLE_SUMMARIES, json.toString());
                } else {
                    response = supabaseClient.updateInTable(
                            Constants.TABLE_SUMMARIES, summary.getId(), json.toString());
                }

                if (response == null) {
                    callback.onError("Không thể lưu bản tóm tắt");
                    return;
                }

                // Supabase trả array rỗng nếu RLS không cho phép update record.
                JsonArray rows = parseRows(response, "Không thể lưu bản tóm tắt");
                if (rows.size() == 0) {
                    callback.onError("Không tìm thấy bản tóm tắt để lưu");
                    return;
                }
                callback.onSuccess(parseSummary(rows.get(0).getAsJsonObject()));
            } catch (Exception error) {
                callback.onError(readError(error, "Không thể lưu bản tóm tắt"));
            }
        }).start();
    }

    /**
     * Lấy bộ câu hỏi đã lưu của một tài liệu.
     */
    public void getQuizQuestionsByDocument(String userId, String documentId,
                                           ApiCallback<List<QuizQuestion>> callback) {
        new Thread(() -> {
            try {
                if (isBlank(userId) || isBlank(documentId)) {
                    callback.onError("Thiếu thông tin user hoặc tài liệu");
                    return;
                }

                String query = "user_id=eq." + userId
                        + "&document_id=eq." + documentId
                        + "&order=created_at.asc,id.asc";
                String response = supabaseClient.getFromTable(
                        Constants.TABLE_QUIZZES, query);
                if (response == null) {
                    callback.onError("Không thể tải câu hỏi");
                    return;
                }

                JsonArray rows = parseRows(response, "Không thể tải câu hỏi");
                callback.onSuccess(parseQuizQuestions(rows));
            } catch (Exception error) {
                callback.onError(readError(error, "Không thể đọc câu hỏi"));
            }
        }).start();
    }

    /**
     * Bulk insert cả bộ câu hỏi sau khi AI sinh thành công.
     */
    public void saveQuizQuestions(Document document, List<QuizQuestion> questions,
                                  ApiCallback<List<QuizQuestion>> callback) {
        new Thread(() -> {
            try {
                validateQuizQuestions(document, questions);
                JsonArray requestRows = new JsonArray();
                for (QuizQuestion question : questions) {
                    requestRows.add(buildQuizJson(document, question));
                }

                String response = supabaseClient.insertIntoTable(
                        Constants.TABLE_QUIZZES, requestRows.toString());
                if (response == null) {
                    callback.onError("Không thể lưu bộ câu hỏi");
                    return;
                }

                JsonArray savedRows = parseRows(response, "Không thể lưu bộ câu hỏi");
                if (savedRows.size() != questions.size()) {
                    callback.onError("Số câu hỏi đã lưu không khớp kết quả AI");
                    return;
                }
                callback.onSuccess(parseQuizQuestions(savedRows));
            } catch (Exception error) {
                callback.onError(readError(error, "Không thể lưu bộ câu hỏi"));
            }
        }).start();
    }

    private JsonObject buildSummaryJson(Summary summary) {
        JsonObject json = new JsonObject();
        json.addProperty("user_id", summary.getUserId());
        json.addProperty("document_id", summary.getDocumentId());
        json.addProperty("summary_text", summary.getSummaryText());
        json.add("key_points", toJsonArray(summary.getKeyPoints()));
        json.add("keywords", toJsonArray(summary.getKeywords()));
        if (isBlank(summary.getConclusion())) {
            json.add("conclusion", null);
        } else {
            json.addProperty("conclusion", summary.getConclusion());
        }
        return json;
    }

    private JsonObject buildQuizJson(Document document, QuizQuestion question) {
        JsonObject json = new JsonObject();
        json.addProperty("user_id", document.getUserId());
        json.addProperty("document_id", document.getId());
        if (!isBlank(document.getTopicId())) {
            json.addProperty("topic_id", document.getTopicId());
        }
        json.addProperty("question", question.getQuestion());
        json.addProperty("option_a", question.getOptionA());
        json.addProperty("option_b", question.getOptionB());
        json.addProperty("option_c", question.getOptionC());
        json.addProperty("option_d", question.getOptionD());
        json.addProperty("correct_answer", question.getCorrectAnswer());
        // Bulk insert yêu cầu mọi row có cùng tập key.
        json.addProperty("explanation", question.getExplanation());
        json.addProperty("difficulty", "MEDIUM");
        return json;
    }

    private Summary parseSummary(JsonObject json) {
        Summary summary = new Summary();
        summary.setId(readString(json, "id"));
        summary.setUserId(readString(json, "user_id"));
        summary.setDocumentId(readString(json, "document_id"));
        summary.setSummaryText(readString(json, "summary_text"));
        summary.setKeyPoints(readStringList(json, "key_points"));
        summary.setKeywords(readStringList(json, "keywords"));
        summary.setConclusion(readString(json, "conclusion"));
        return summary;
    }

    private List<QuizQuestion> parseQuizQuestions(JsonArray rows) {
        List<QuizQuestion> questions = new ArrayList<>();
        for (JsonElement row : rows) {
            JsonObject json = row.getAsJsonObject();
            QuizQuestion question = new QuizQuestion(
                    readString(json, "question"),
                    readString(json, "option_a"),
                    readString(json, "option_b"),
                    readString(json, "option_c"),
                    readString(json, "option_d"),
                    readString(json, "correct_answer"),
                    readString(json, "explanation")
            );
            question.setId(readString(json, "id"));
            question.setDocumentId(readString(json, "document_id"));
            question.setOrderIndex(questions.size());
            questions.add(question);
        }
        return questions;
    }

    private JsonArray toJsonArray(List<String> values) {
        JsonArray array = new JsonArray();
        if (values == null) return array;
        for (String value : values) {
            if (!isBlank(value)) array.add(value.trim());
        }
        return array;
    }

    private List<String> readStringList(JsonObject json, String key) {
        List<String> values = new ArrayList<>();
        if (!json.has(key) || !json.get(key).isJsonArray()) return values;
        for (JsonElement element : json.getAsJsonArray(key)) {
            if (element.isJsonPrimitive()) values.add(element.getAsString());
        }
        return values;
    }

    private String readString(JsonObject json, String key) {
        return !json.has(key) || json.get(key).isJsonNull()
                ? ""
                : json.get(key).getAsString();
    }

    private JsonArray parseRows(String response, String fallback) {
        JsonElement root = JsonParser.parseString(response);
        if (root.isJsonArray()) return root.getAsJsonArray();
        if (root.isJsonObject()) {
            JsonObject error = root.getAsJsonObject();
            String message = readString(error, "message");
            if (!isBlank(message)) throw new IllegalStateException(message);
        }
        throw new IllegalStateException(fallback);
    }

    private void validateSummary(Summary summary) {
        if (summary == null || isBlank(summary.getUserId())
                || isBlank(summary.getDocumentId())
                || isBlank(summary.getSummaryText())) {
            throw new IllegalArgumentException("Bản tóm tắt thiếu dữ liệu bắt buộc");
        }
    }

    private void validateQuizQuestions(Document document,
                                       List<QuizQuestion> questions) {
        if (document == null || isBlank(document.getUserId())
                || isBlank(document.getId()) || questions == null
                || questions.isEmpty()) {
            throw new IllegalArgumentException("Bộ câu hỏi thiếu dữ liệu bắt buộc");
        }
        for (QuizQuestion question : questions) {
            if (question == null || isBlank(question.getQuestion())
                    || isBlank(question.getOptionA()) || isBlank(question.getOptionB())
                    || isBlank(question.getOptionC()) || isBlank(question.getOptionD())
                    || isBlank(question.getCorrectAnswer())
                    || !question.getCorrectAnswer().matches("[ABCD]")) {
                throw new IllegalArgumentException("Có câu hỏi hoặc đáp án không hợp lệ");
            }
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
