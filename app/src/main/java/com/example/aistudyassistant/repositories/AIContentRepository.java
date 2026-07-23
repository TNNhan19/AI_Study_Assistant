package com.example.aistudyassistant.repositories;

import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.models.Flashcard;
import com.example.aistudyassistant.models.QuizQuestion;
import com.example.aistudyassistant.models.StudySet;
import com.example.aistudyassistant.models.Summary;
import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Map;

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
     * Returns one study set per source document. Rows are ordered newest first,
     * so users can immediately reopen generated quizzes or flashcards.
     */
    public void getStudySets(String userId, boolean quizMode,
                             ApiCallback<List<StudySet>> callback) {
        new Thread(() -> {
            try {
                if (isBlank(userId)) {
                    callback.onError("Thiếu thông tin người dùng");
                    return;
                }
                if (quizMode) {
                    callback.onSuccess(loadQuizStudySets(userId));
                    return;
                }

                String table = Constants.TABLE_FLASHCARDS;
                String query = "user_id=eq." + userId
                        + "&select=document_id,created_at,"
                        + "documents(name,file_path,file_type,topic_id,project_id)"
                        + "&order=created_at.desc";
                String response = supabaseClient.getFromTable(table, query);
                if (response == null) {
                    callback.onError(quizMode
                            ? "Không thể tải danh sách quiz"
                            : "Không thể tải danh sách flashcard");
                    return;
                }

                JsonArray rows = parseRows(response, quizMode
                        ? "Không thể tải danh sách quiz"
                        : "Không thể tải danh sách flashcard");
                Map<String, String> projectNames = loadNames(
                        Constants.TABLE_PROJECTS, userId);
                Map<String, String> topicNames = loadNames(
                        Constants.TABLE_TOPICS, userId);
                Map<String, StudySet> grouped = new LinkedHashMap<>();
                for (JsonElement row : rows) {
                    JsonObject json = row.getAsJsonObject();
                    String documentId = readString(json, "document_id");
                    if (isBlank(documentId)) continue;

                    StudySet existing = grouped.get(documentId);
                    if (existing != null) {
                        existing.incrementItemCount();
                        continue;
                    }

                    JsonObject document = readRelatedDocument(json);
                    if (document == null) continue;
                    String projectName = projectNames.getOrDefault(
                            readString(document, "project_id"), "");
                    String topicName = topicNames.getOrDefault(
                            readString(document, "topic_id"), "");
                    StudySet studySet = new StudySet(
                            documentId,
                            readString(document, "name"),
                            readString(document, "file_path"),
                            readString(document, "file_type"),
                            readString(document, "topic_id"),
                            projectName,
                            topicName,
                            readString(json, "created_at")
                    );
                    grouped.put(documentId, studySet);
                }
                List<StudySet> result = new ArrayList<>(grouped.values());
                callback.onSuccess(result);
            } catch (Exception error) {
                callback.onError(readError(error, quizMode
                        ? "Không thể đọc danh sách quiz"
                        : "Không thể đọc danh sách flashcard"));
            }
        }).start();
    }

    private List<StudySet> loadQuizStudySets(String userId) {
        Map<String, Integer> questionCounts = new LinkedHashMap<>();
        String questionResponse = supabaseClient.getFromTable(
                Constants.TABLE_QUIZZES,
                "user_id=eq." + userId + "&select=quiz_set_id");
        for (JsonElement row : parseRows(
                questionResponse, "Không thể đếm câu hỏi quiz")) {
            String quizSetId = readString(row.getAsJsonObject(), "quiz_set_id");
            if (!isBlank(quizSetId)) {
                questionCounts.put(
                        quizSetId, questionCounts.getOrDefault(quizSetId, 0) + 1);
            }
        }

        Map<String, String> projectNames = loadNames(Constants.TABLE_PROJECTS, userId);
        Map<String, String> topicNames = loadNames(Constants.TABLE_TOPICS, userId);
        String response = supabaseClient.getFromTable(
                Constants.TABLE_QUIZ_SETS,
                "user_id=eq." + userId
                        + "&select=id,document_id,title,is_pinned,difficulty,created_at,"
                        + "documents(name,file_path,file_type,topic_id,project_id)"
                        + "&order=is_pinned.desc,updated_at.desc");
        JsonArray rows = parseRows(response, "Không thể tải danh sách quiz");
        List<StudySet> result = new ArrayList<>();
        for (JsonElement row : rows) {
            JsonObject json = row.getAsJsonObject();
            JsonObject document = readRelatedDocument(json);
            String documentId = readString(json, "document_id");
            if (isBlank(documentId) || document == null) continue;

            StudySet studySet = new StudySet(
                    documentId,
                    readString(document, "name"),
                    readString(document, "file_path"),
                    readString(document, "file_type"),
                    readString(document, "topic_id"),
                    projectNames.getOrDefault(readString(document, "project_id"), ""),
                    topicNames.getOrDefault(readString(document, "topic_id"), ""),
                    readString(json, "created_at")
            );
            String quizSetId = readString(json, "id");
            studySet.applyQuizMetadata(
                    quizSetId,
                    readString(json, "title"),
                    json.has("is_pinned")
                            && !json.get("is_pinned").isJsonNull()
                            && json.get("is_pinned").getAsBoolean(),
                    readString(json, "difficulty")
            );
            studySet.setItemCount(questionCounts.getOrDefault(quizSetId, 0));
            result.add(studySet);
        }
        return result;
    }

    public void updateQuizSet(String quizSetId, String title, Boolean pinned,
                              ApiCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                if (isBlank(quizSetId)) {
                    callback.onError("Không tìm thấy bộ quiz");
                    return;
                }
                JsonObject body = new JsonObject();
                if (title != null) {
                    String normalizedTitle = title.trim();
                    if (normalizedTitle.isEmpty()) {
                        callback.onError("Tên quiz không được để trống");
                        return;
                    }
                    body.addProperty("title", normalizedTitle);
                }
                if (pinned != null) body.addProperty("is_pinned", pinned);

                String response = supabaseClient.updateInTable(
                        Constants.TABLE_QUIZ_SETS, quizSetId, body.toString());
                JsonArray rows = parseRows(response, "Không thể cập nhật bộ quiz");
                if (rows.size() == 0) {
                    callback.onError("Không tìm thấy bộ quiz để cập nhật");
                    return;
                }
                callback.onSuccess(true);
            } catch (Exception error) {
                callback.onError(readError(error, "Không thể cập nhật bộ quiz"));
            }
        }).start();
    }

    public void deleteQuizSet(String quizSetId, ApiCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                if (isBlank(quizSetId)) {
                    callback.onError("Không tìm thấy bộ quiz");
                    return;
                }
                JsonObject body = new JsonObject();
                body.addProperty("p_quiz_set_id", quizSetId);
                String response = supabaseClient.callRpc(
                        "delete_quiz_set", body.toString());
                if (response == null
                        || !JsonParser.parseString(response).getAsBoolean()) {
                    callback.onError("Không thể xóa bộ quiz");
                    return;
                }
                callback.onSuccess(true);
            } catch (Exception error) {
                callback.onError(readError(error, "Không thể xóa bộ quiz"));
            }
        }).start();
    }

    private Map<String, String> loadNames(String table, String userId) {
        Map<String, String> names = new LinkedHashMap<>();
        String response = supabaseClient.getFromTable(
                table,
                "user_id=eq." + userId + "&select=id,name");
        if (response == null) return names;

        JsonArray rows = parseRows(response, "Không thể tải dữ liệu phân loại");
        for (JsonElement row : rows) {
            JsonObject json = row.getAsJsonObject();
            String id = readString(json, "id");
            if (!isBlank(id)) {
                names.put(id, readString(json, "name"));
            }
        }
        return names;
    }

    private JsonObject readRelatedDocument(JsonObject row) {
        if (!row.has("documents") || row.get("documents").isJsonNull()) {
            return null;
        }
        JsonElement related = row.get("documents");
        if (related.isJsonObject()) {
            return related.getAsJsonObject();
        }
        if (related.isJsonArray() && related.getAsJsonArray().size() > 0) {
            return related.getAsJsonArray().get(0).getAsJsonObject();
        }
        return null;
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

    public void getQuizQuestionsBySet(String userId, String quizSetId,
                                      ApiCallback<List<QuizQuestion>> callback) {
        new Thread(() -> {
            try {
                if (isBlank(userId) || isBlank(quizSetId)) {
                    callback.onError("Thiếu thông tin user hoặc bộ quiz");
                    return;
                }
                String query = "user_id=eq." + userId
                        + "&quiz_set_id=eq." + quizSetId
                        + "&order=created_at.asc,id.asc";
                JsonArray rows = parseRows(
                        supabaseClient.getFromTable(Constants.TABLE_QUIZZES, query),
                        "Không thể tải câu hỏi");
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
        saveQuizQuestions(document, questions, "MEDIUM", callback);
    }

    public void saveQuizQuestions(Document document, List<QuizQuestion> questions,
                                  String difficulty,
                                  ApiCallback<List<QuizQuestion>> callback) {
        new Thread(() -> {
            try {
                validateQuizQuestions(document, questions);
                String normalizedDifficulty = normalizeQuizDifficulty(difficulty);
                String quizSetId = createQuizSet(document, normalizedDifficulty);
                JsonArray requestRows = new JsonArray();
                for (QuizQuestion question : questions) {
                    requestRows.add(buildQuizJson(
                            document, question, quizSetId, normalizedDifficulty));
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

    private String createQuizSet(Document document, String difficulty) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", document.getUserId());
        body.addProperty("document_id", document.getId());
        String levelName = "EASY".equals(difficulty)
                ? "Easy"
                : ("HARD".equals(difficulty) ? "Hard" : "Medium");
        body.addProperty("title", (isBlank(document.getName())
                ? "Quiz"
                : document.getName().trim()) + " - " + levelName);
        body.addProperty("difficulty", difficulty);
        JsonArray inserted = parseRows(
                supabaseClient.insertIntoTable(
                        Constants.TABLE_QUIZ_SETS, body.toString()),
                "Không thể tạo bộ quiz");
        if (inserted.size() == 0) {
            throw new IllegalStateException("Không thể tạo bộ quiz");
        }
        return readString(inserted.get(0).getAsJsonObject(), "id");
    }

    /**
     * Lấy flashcard đã lưu của một tài liệu.
     */
    public void getFlashcardsByDocument(String userId, String documentId,
                                        ApiCallback<List<Flashcard>> callback) {
        if (isBlank(userId) || isBlank(documentId)) {
            callback.onError("Thiếu thông tin user hoặc tài liệu");
            return;
        }
        String query = "user_id=eq." + userId
                + "&document_id=eq." + documentId
                + "&order=created_at.asc,id.asc";
        fetchFlashcards(query, callback);
    }

    /**
     * Lấy flashcard theo topic để tái sử dụng cho màn hình ôn tập.
     */
    public void getFlashcardsByTopic(String userId, String topicId,
                                     ApiCallback<List<Flashcard>> callback) {
        if (isBlank(userId) || isBlank(topicId)) {
            callback.onError("Thiếu thông tin user hoặc chủ đề");
            return;
        }
        String query = "user_id=eq." + userId
                + "&topic_id=eq." + topicId
                + "&order=created_at.asc,id.asc";
        fetchFlashcards(query, callback);
    }

    public void getFlashcardsByUser(String userId, ApiCallback<List<Flashcard>> callback) {
        if (isBlank(userId)) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        String query = "user_id=eq." + userId
                + "&order=created_at.desc,id.desc"
                + "&limit=500";
        fetchFlashcards(query, callback);
    }

    /**
     * Bulk insert toàn bộ flashcard do AI tạo.
     */
    public void saveFlashcards(Document document, List<Flashcard> flashcards,
                               ApiCallback<List<Flashcard>> callback) {
        new Thread(() -> {
            try {
                validateFlashcards(document, flashcards);
                JsonArray requestRows = new JsonArray();
                for (Flashcard flashcard : flashcards) {
                    requestRows.add(buildFlashcardJson(document, flashcard));
                }

                String response = supabaseClient.insertIntoTable(
                        Constants.TABLE_FLASHCARDS, requestRows.toString());
                if (response == null) {
                    callback.onError("Không thể lưu flashcard");
                    return;
                }

                JsonArray savedRows = parseRows(response, "Không thể lưu flashcard");
                if (savedRows.size() != flashcards.size()) {
                    callback.onError("Số flashcard đã lưu không khớp kết quả AI");
                    return;
                }
                callback.onSuccess(parseFlashcards(savedRows));
            } catch (Exception error) {
                callback.onError(readError(error, "Không thể lưu flashcard"));
            }
        }).start();
    }

    private void fetchFlashcards(String query, ApiCallback<List<Flashcard>> callback) {
        new Thread(() -> {
            try {
                String response = supabaseClient.getFromTable(
                        Constants.TABLE_FLASHCARDS, query);
                if (response == null) {
                    callback.onError("Không thể tải flashcard");
                    return;
                }
                JsonArray rows = parseRows(response, "Không thể tải flashcard");
                callback.onSuccess(parseFlashcards(rows));
            } catch (Exception error) {
                callback.onError(readError(error, "Không thể đọc flashcard"));
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

    private JsonObject buildQuizJson(Document document, QuizQuestion question,
                                     String quizSetId, String difficulty) {
        JsonObject json = new JsonObject();
        json.addProperty("user_id", document.getUserId());
        json.addProperty("document_id", document.getId());
        json.addProperty("quiz_set_id", quizSetId);
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
        json.addProperty("difficulty", difficulty);
        return json;
    }

    private JsonObject buildFlashcardJson(Document document, Flashcard flashcard) {
        JsonObject json = new JsonObject();
        json.addProperty("user_id", document.getUserId());
        json.addProperty("document_id", document.getId());
        if (!isBlank(document.getTopicId())) {
            json.addProperty("topic_id", document.getTopicId());
        }
        json.addProperty("front", flashcard.getFront());
        json.addProperty("back", flashcard.getBack());
        json.addProperty("difficulty", isBlank(flashcard.getDifficulty())
                ? "MEDIUM"
                : flashcard.getDifficulty());
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
            question.setQuizSetId(readString(json, "quiz_set_id"));
            question.setDifficulty(readString(json, "difficulty"));
            question.setOrderIndex(questions.size());
            questions.add(question);
        }
        return questions;
    }

    private List<Flashcard> parseFlashcards(JsonArray rows) {
        List<Flashcard> flashcards = new ArrayList<>();
        for (JsonElement row : rows) {
            JsonObject json = row.getAsJsonObject();
            Flashcard flashcard = new Flashcard(
                    readString(json, "front"),
                    readString(json, "back")
            );
            flashcard.setId(readString(json, "id"));
            flashcard.setUserId(readString(json, "user_id"));
            flashcard.setDocumentId(readString(json, "document_id"));
            flashcard.setTopicId(readString(json, "topic_id"));
            String difficulty = readString(json, "difficulty");
            flashcard.setDifficulty(isBlank(difficulty) ? "MEDIUM" : difficulty);
            flashcard.setCreatedAt(parseTimestamp(readString(json, "created_at")));
            flashcards.add(flashcard);
        }
        return flashcards;
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

    private void validateFlashcards(Document document, List<Flashcard> flashcards) {
        if (document == null || isBlank(document.getUserId())
                || isBlank(document.getId()) || flashcards == null
                || flashcards.isEmpty()) {
            throw new IllegalArgumentException("Bộ flashcard thiếu dữ liệu bắt buộc");
        }
        for (Flashcard flashcard : flashcards) {
            if (flashcard == null || isBlank(flashcard.getFront())
                    || isBlank(flashcard.getBack())) {
                throw new IllegalArgumentException("Có flashcard thiếu câu hỏi hoặc đáp án");
            }
        }
    }

    private String readError(Exception error, String fallback) {
        return error.getMessage() == null || error.getMessage().trim().isEmpty()
                ? fallback
                : error.getMessage();
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeQuizDifficulty(String difficulty) {
        String normalized = isBlank(difficulty)
                ? "MEDIUM"
                : difficulty.trim().toUpperCase(Locale.US);
        if (!"EASY".equals(normalized)
                && !"MEDIUM".equals(normalized)
                && !"HARD".equals(normalized)) {
            throw new IllegalArgumentException("Độ khó quiz không hợp lệ");
        }
        return normalized;
    }

}
