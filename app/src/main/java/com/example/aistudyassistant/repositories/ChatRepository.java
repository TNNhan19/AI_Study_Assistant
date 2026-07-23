package com.example.aistudyassistant.repositories;

import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.ChatContextScope;
import com.example.aistudyassistant.models.ChatMessage;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatRepository {

    private static final int HISTORY_LIMIT = 30;
    private static ChatRepository instance;

    private final SupabaseClient supabaseClient;

    private ChatRepository() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public static synchronized ChatRepository getInstance() {
        if (instance == null) {
            instance = new ChatRepository();
        }
        return instance;
    }

    public void getHistory(String userId, Document document, ChatContextScope scope,
                           ApiCallback<List<ChatMessage>> callback) {
        new Thread(() -> {
            try {
                String query = buildHistoryQuery(userId, document, scope)
                        + "&order=created_at.desc&limit=" + HISTORY_LIMIT;
                String response = supabaseClient.getFromTable(
                        Constants.TABLE_CHAT_HISTORY, query);
                if (response == null) {
                    callback.onError("Không thể tải lịch sử trò chuyện");
                    return;
                }

                JsonArray array = requireArrayResponse(response);
                List<ChatMessage> history = new ArrayList<>();
                for (JsonElement element : array) {
                    history.add(parseMessage(element.getAsJsonObject()));
                }
                // API lấy bản ghi mới nhất trước, UI cần hiển thị theo thời gian tăng dần.
                Collections.reverse(history);
                callback.onSuccess(history);
            } catch (Exception error) {
                callback.onError(readError(error, "Không thể đọc lịch sử trò chuyện"));
            }
        }).start();
    }

    public void saveMessage(String userId, Document document, ChatContextScope scope,
                            ChatMessage message, ApiCallback<ChatMessage> callback) {
        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("user_id", userId);
                addContextIds(json, document, scope);
                json.addProperty("role", message.isUserMessage() ? "USER" : "ASSISTANT");
                json.addProperty("content", message.getContent());

                String response = supabaseClient.insertIntoTable(
                        Constants.TABLE_CHAT_HISTORY, json.toString());
                if (response == null) {
                    callback.onError("Không thể lưu tin nhắn");
                    return;
                }

                JsonArray array = requireArrayResponse(response);
                if (array.size() == 0) {
                    callback.onError("Supabase không trả về tin nhắn vừa lưu");
                    return;
                }
                callback.onSuccess(parseMessage(array.get(0).getAsJsonObject()));
            } catch (Exception error) {
                callback.onError(readError(error, "Không thể lưu tin nhắn"));
            }
        }).start();
    }

    private String buildHistoryQuery(String userId, Document document,
                                     ChatContextScope scope) {
        String query = "user_id=eq." + requireValue(userId, "Thiếu user_id");
        switch (scope) {
            case DOCUMENT:
                return query + "&document_id=eq." + requireValue(
                        document.getId(), "Thiếu document_id");
            case TOPIC:
                return query + "&topic_id=eq." + requireValue(
                        document.getTopicId(), "Tài liệu chưa thuộc chủ đề")
                        + "&document_id=is.null";
            case PROJECT:
                return query + "&project_id=eq." + requireValue(
                        document.getProjectId(), "Tài liệu chưa thuộc dự án")
                        + "&topic_id=is.null&document_id=is.null";
            default:
                throw new IllegalArgumentException("Phạm vi trò chuyện không hợp lệ");
        }
    }

    private void addContextIds(JsonObject json, Document document,
                               ChatContextScope scope) {
        if (scope == ChatContextScope.DOCUMENT) {
            json.addProperty("document_id", requireValue(
                    document.getId(), "Thiếu document_id"));
            return;
        }

        if (scope == ChatContextScope.TOPIC) {
            json.addProperty("topic_id", requireValue(
                    document.getTopicId(), "Tài liệu chưa thuộc chủ đề"));
            return;
        }

        json.addProperty("project_id", requireValue(
                document.getProjectId(), "Tài liệu chưa thuộc dự án"));
    }

    private ChatMessage parseMessage(JsonObject json) {
        int type = "USER".equalsIgnoreCase(readString(json, "role"))
                ? Constants.MSG_TYPE_USER : Constants.MSG_TYPE_AI;
        ChatMessage message = new ChatMessage(readString(json, "content"), type);
        message.setId(readString(json, "id"));
        message.setUserId(readString(json, "user_id"));
        message.setDocumentId(readString(json, "document_id"));
        message.setTimestamp(parseTimestamp(readString(json, "created_at")));
        return message;
    }

    private long parseTimestamp(String value) {
        if (!hasValue(value)) return System.currentTimeMillis();
        String normalized = value.replaceFirst("(\\.\\d{3})\\d+", "$1");
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX"
        };
        for (String pattern : patterns) {
            try {
                Date date = new SimpleDateFormat(pattern, Locale.US).parse(normalized);
                if (date != null) return date.getTime();
            } catch (Exception ignored) {
                // Thử định dạng timestamp tiếp theo.
            }
        }
        return System.currentTimeMillis();
    }

    private String readString(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) return null;
        return json.get(key).getAsString();
    }

    private JsonArray requireArrayResponse(String response) {
        JsonElement root = JsonParser.parseString(response);
        if (root.isJsonArray()) return root.getAsJsonArray();
        if (root.isJsonObject()) {
            String message = readString(root.getAsJsonObject(), "message");
            throw new IllegalStateException(hasValue(message) ? message : response);
        }
        throw new IllegalStateException("Supabase trả về dữ liệu không hợp lệ");
    }

    private String requireValue(String value, String errorMessage) {
        if (!hasValue(value)) throw new IllegalArgumentException(errorMessage);
        return value;
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String readError(Exception error, String fallback) {
        return error.getMessage() == null || error.getMessage().trim().isEmpty()
                ? fallback : error.getMessage();
    }
}
