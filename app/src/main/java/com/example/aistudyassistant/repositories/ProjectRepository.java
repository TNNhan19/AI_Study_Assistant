package com.example.aistudyassistant.repositories;

import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.Project;
import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class ProjectRepository {
    private static ProjectRepository instance;
    private final SupabaseClient supabaseClient;

    private ProjectRepository() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public static synchronized ProjectRepository getInstance() {
        if (instance == null) {
            instance = new ProjectRepository();
        }
        return instance;
    }

    public void getAllProjects(String userId, ApiCallback<List<Project>> callback) {
        new Thread(() -> {
            try {
                String query = "user_id=eq." + userId + "&order=created_at.desc";
                String response = supabaseClient.getFromTable(Constants.TABLE_PROJECTS, query);

                if (response == null) {
                    callback.onError("Failed to fetch projects");
                    return;
                }

                JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
                List<Project> projects = new ArrayList<>();
                for (JsonElement element : jsonArray) {
                    JsonObject obj = element.getAsJsonObject();
                    Project project = new Project();
                    project.setId(obj.get("id").getAsString());
                    project.setUserId(obj.get("user_id").getAsString());
                    project.setName(obj.get("name").getAsString());
                    project.setDescription(obj.has("description") && !obj.get("description").isJsonNull() ? obj.get("description").getAsString() : "");
                    // createdAt is optional for now in model
                    projects.add(project);
                }
                callback.onSuccess(projects);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void createProject(String userId, String name, String description, ApiCallback<Project> callback) {
        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("user_id", userId);
                json.addProperty("name", name);
                json.addProperty("description", description);

                String response = supabaseClient.insertIntoTable(Constants.TABLE_PROJECTS, json.toString());

                if (response != null) {
                    com.google.gson.JsonElement jsonElement = JsonParser.parseString(response);
                    if (jsonElement.isJsonArray()) {
                        JsonArray resultArray = jsonElement.getAsJsonArray();
                        if (resultArray.size() > 0) {
                            JsonObject obj = resultArray.get(0).getAsJsonObject();
                            Project project = new Project();
                            project.setId(obj.get("id").getAsString());
                            project.setUserId(obj.get("user_id").getAsString());
                            project.setName(obj.get("name").getAsString());
                            project.setDescription(obj.has("description") && !obj.get("description").isJsonNull() ? obj.get("description").getAsString() : "");
                            callback.onSuccess(project);
                        } else {
                            callback.onError("Failed to create project: Empty response");
                        }
                    } else if (jsonElement.isJsonObject()) {
                        JsonObject errorObj = jsonElement.getAsJsonObject();
                        String msg = errorObj.has("message") ? errorObj.get("message").getAsString() : "Database error";
                        callback.onError(msg);
                    }
                } else {
                    callback.onError("Create project failed: No response from server");
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void updateProject(Project project, ApiCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("name", project.getName());
                json.addProperty("description", project.getDescription());

                String response = supabaseClient.updateInTable(Constants.TABLE_PROJECTS, project.getId(), json.toString());
                
                // SupabaseClient.updateInTable calls patchRequest which returns response.body().string()
                // A successful update with 'Prefer: return=representation' returns a JSON array "[{...}]"
                if (response != null && response.startsWith("[")) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("Update failed: " + (response != null ? response : "No response"));
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void deleteProject(String projectId, ApiCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                String response = supabaseClient.deleteFromTable(Constants.TABLE_PROJECTS, projectId);
                // SupabaseClient.deleteFromTable calls deleteRequest which returns "success" on response.isSuccessful()
                if ("success".equals(response)) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("Delete failed: " + (response != null ? response : "No response"));
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
}
