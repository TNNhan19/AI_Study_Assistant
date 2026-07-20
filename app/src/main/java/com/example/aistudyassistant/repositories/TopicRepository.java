package com.example.aistudyassistant.repositories;

import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.Topic;
import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class TopicRepository {
    private static TopicRepository instance;
    private final SupabaseClient supabaseClient;

    private TopicRepository() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public static synchronized TopicRepository getInstance() {
        if (instance == null) {
            instance = new TopicRepository();
        }
        return instance;
    }

    public void getTopicsByProject(String projectId, ApiCallback<List<Topic>> callback) {
        new Thread(() -> {
            try {
                // Filter by project_id, order by pinned desc then created_at desc
                String query = "project_id=eq." + projectId + "&order=is_pinned.desc,created_at.desc";
                String response = supabaseClient.getFromTable(Constants.TABLE_TOPICS, query);

                if (response == null) {
                    callback.onError("Failed to fetch topics");
                    return;
                }

                JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
                List<Topic> topics = new ArrayList<>();
                for (JsonElement element : jsonArray) {
                    JsonObject obj = element.getAsJsonObject();
                    Topic topic = new Topic();
                    topic.setId(obj.get("id").getAsString());
                    topic.setProjectId(obj.get("project_id").getAsString());
                    topic.setUserId(obj.get("user_id").getAsString());
                    topic.setName(obj.get("name").getAsString());
                    topic.setDescription(obj.has("description") && !obj.get("description").isJsonNull() ? obj.get("description").getAsString() : "");
                    topic.setPinned(obj.has("is_pinned") && !obj.get("is_pinned").isJsonNull() && obj.get("is_pinned").getAsBoolean());
                    topics.add(topic);
                }
                callback.onSuccess(topics);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void getAllTopics(String userId, ApiCallback<List<Topic>> callback) {
        new Thread(() -> {
            try {
                String query = "user_id=eq." + userId + "&order=created_at.desc";
                String response = supabaseClient.getFromTable(Constants.TABLE_TOPICS, query);

                if (response == null) {
                    callback.onError("Failed to fetch all topics");
                    return;
                }

                JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
                List<Topic> topics = new ArrayList<>();
                for (JsonElement element : jsonArray) {
                    JsonObject obj = element.getAsJsonObject();
                    Topic topic = new Topic();
                    topic.setId(obj.get("id").getAsString());
                    if (obj.has("project_id") && !obj.get("project_id").isJsonNull())
                        topic.setProjectId(obj.get("project_id").getAsString());
                    topic.setUserId(obj.get("user_id").getAsString());
                    topic.setName(obj.get("name").getAsString());
                    topic.setDescription(obj.has("description") && !obj.get("description").isJsonNull() ? obj.get("description").getAsString() : "");
                    topic.setPinned(obj.has("is_pinned") && !obj.get("is_pinned").isJsonNull() && obj.get("is_pinned").getAsBoolean());
                    topics.add(topic);
                }
                callback.onSuccess(topics);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void createTopic(Topic topic, ApiCallback<Topic> callback) {
        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                // Đảm bảo không gửi chuỗi rỗng cho UUID
                if (topic.getProjectId() != null && !topic.getProjectId().isEmpty()) {
                    json.addProperty("project_id", topic.getProjectId());
                }
                
                // Luôn đính kèm user_id cho RLS
                json.addProperty("user_id", topic.getUserId());
                json.addProperty("name", topic.getName());
                json.addProperty("description", topic.getDescription());
                json.addProperty("is_pinned", topic.isPinned());

                String response = supabaseClient.insertIntoTable(Constants.TABLE_TOPICS, json.toString());

                if (response != null) {
                    com.google.gson.JsonElement jsonElement = JsonParser.parseString(response);
                    if (jsonElement.isJsonArray()) {
                        JsonArray resultArray = jsonElement.getAsJsonArray();
                        if (resultArray.size() > 0) {
                            JsonObject obj = resultArray.get(0).getAsJsonObject();
                            topic.setId(obj.get("id").getAsString());
                            callback.onSuccess(topic);
                        } else {
                            callback.onError("Failed to create topic: Empty response (check RLS policies)");
                        }
                    } else {
                        callback.onError("Server error: " + response);
                    }
                } else {
                    callback.onError("Create topic failed: No response");
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void updateTopic(Topic topic, ApiCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("name", topic.getName());
                json.addProperty("description", topic.getDescription());
                json.addProperty("is_pinned", topic.isPinned());

                String response = supabaseClient.updateInTable(Constants.TABLE_TOPICS, topic.getId(), json.toString());
                // Thành công trả về mảng kết quả "[...]"
                if (response != null && response.startsWith("[")) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("Update failed: " + response);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void togglePin(String topicId, boolean isPinned, ApiCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("is_pinned", isPinned);

                String response = supabaseClient.updateInTable(Constants.TABLE_TOPICS, topicId, json.toString());
                if (response != null && response.startsWith("[")) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("Toggle pin failed: " + response);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void deleteTopic(String topicId, ApiCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                String response = supabaseClient.deleteFromTable(Constants.TABLE_TOPICS, topicId);
                if ("success".equals(response)) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("Delete failed: " + response);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
}
