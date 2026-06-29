package com.example.aistudyassistant.repositories;

import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.Project;
import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonObject;

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
    public void createProject(String userId, String name, String description,
                              ApiCallback<String> callback) {
        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("user_id", userId);
                json.addProperty("name", name);
                json.addProperty("description", description);

                // Repository gom logic insert project để Activity không gọi Supabase trực tiếp.
                        String response = supabaseClient.insertIntoTable(
                        Constants.TABLE_PROJECTS,
                        json.toString()
                );

                if (response != null) {
                    callback.onSuccess(response);
                } else {
                    callback.onError("Create project failed");
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
}