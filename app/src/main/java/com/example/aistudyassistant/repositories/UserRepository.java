package com.example.aistudyassistant.repositories;

import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.User;
import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UserRepository {
    private static UserRepository instance;
    private final SupabaseClient supabaseClient;

    private UserRepository() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    public void getProfile(String userId, ApiCallback<User> callback) {
        new Thread(() -> {
            try {
                if (userId == null || userId.trim().isEmpty()) {
                    callback.onError("Invalid user session");
                    return;
                }

                String response = supabaseClient.getFromTable(
                        Constants.TABLE_USERS,
                        "id=eq." + userId + "&select=id,email,full_name,avatar_url"
                );
                JsonArray rows = parseRows(response);
                if (rows.size() == 0) {
                    callback.onError("Profile not found");
                    return;
                }
                callback.onSuccess(parseUser(rows.get(0).getAsJsonObject()));
            } catch (Exception e) {
                callback.onError(errorMessage(e));
            }
        }).start();
    }

    public void updateFullName(String userId, String fullName,
                               ApiCallback<User> callback) {
        new Thread(() -> {
            try {
                if (userId == null || userId.trim().isEmpty()) {
                    callback.onError("Invalid user session");
                    return;
                }
                if (fullName == null || fullName.trim().isEmpty()) {
                    callback.onError("Name cannot be empty");
                    return;
                }

                JsonObject update = new JsonObject();
                update.addProperty("full_name", fullName.trim());
                String response = supabaseClient.updateInTable(
                        Constants.TABLE_USERS, userId, update.toString());

                JsonArray rows = parseRows(response);
                if (rows.size() == 0) {
                    callback.onError("Profile was not updated");
                    return;
                }
                callback.onSuccess(parseUser(rows.get(0).getAsJsonObject()));
            } catch (Exception e) {
                callback.onError(errorMessage(e));
            }
        }).start();
    }

    private static JsonArray parseRows(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new IllegalStateException("No response from server");
        }

        JsonElement element = JsonParser.parseString(response);
        if (element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        if (element.isJsonObject()) {
            JsonObject error = element.getAsJsonObject();
            if (error.has("message") && !error.get("message").isJsonNull()) {
                throw new IllegalStateException(error.get("message").getAsString());
            }
        }
        throw new IllegalStateException("Invalid server response");
    }

    private static User parseUser(JsonObject object) {
        User user = new User();
        user.setId(getString(object, "id", ""));
        user.setEmail(getString(object, "email", ""));
        user.setFullName(getString(object, "full_name", "Student"));
        user.setAvatarUrl(getString(object, "avatar_url", ""));
        return user;
    }

    private static String getString(JsonObject object, String key,
                                    String defaultValue) {
        return object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString()
                : defaultValue;
    }

    private static String errorMessage(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().trim().isEmpty()
                ? "Unable to update profile"
                : exception.getMessage();
    }
}
