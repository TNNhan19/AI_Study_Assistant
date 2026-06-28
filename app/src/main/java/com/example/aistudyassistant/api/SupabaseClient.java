package com.example.aistudyassistant.api;

import android.util.Log;

import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * SupabaseClient handles all communication with the Supabase backend.
 *
 * SETUP:
 * 1. Replace Constants.SUPABASE_URL with your project URL
 * 2. Replace Constants.SUPABASE_ANON_KEY with your anon key
 * 3. Create the following tables in Supabase:
 *    - users (id, email, full_name, avatar_url, created_at)
 *    - documents (id, user_id, name, file_url, file_type, file_size, status, created_at)
 *    - summaries (id, document_id, summary_text, key_points, keywords, conclusion, created_at)
 *    - quiz_questions (id, document_id, question, option_a, option_b, option_c, option_d, correct_answer, explanation, order_index)
 *    - quiz_results (id, document_id, user_id, total_questions, correct_answers, created_at)
 *    - flashcards (id, document_id, front, back, order_index, is_known)
 *    - chat_messages (id, user_id, document_id, content, type, timestamp)
 *    - schedules (id, user_id, title, description, date_time_millis, reminder_enabled, created_at)
 */
public class SupabaseClient {

    private static final String TAG = "SupabaseClient";
    private static SupabaseClient instance;

    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final String anonKey;
    private String accessToken;  // Set after login

    private SupabaseClient() {
        this.baseUrl = Constants.SUPABASE_URL;
        this.anonKey = Constants.SUPABASE_ANON_KEY;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }

    public void setAccessToken(String token) {
        this.accessToken = token;
    }

    // ======================== Auth Endpoints ========================

    /**
     * Register a new user with email and password.
     * Returns the raw JSON response from Supabase Auth.
     */
    public String signUp(String email, String password) {
        return signUp(email, password, null);
    }

    /**
     * Register a new user with email, password, and optional display name metadata.
     */
    public String signUp(String email, String password, String fullName) {
        String url = baseUrl + "/auth/v1/signup";
        // Gửi full_name vào user_metadata để app lấy lại tên sau khi đăng nhập.
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        json.addProperty("password", password);
        if (fullName != null && !fullName.trim().isEmpty()) {
            JsonObject data = new JsonObject();
            data.addProperty("full_name", fullName.trim());
            json.add("data", data);
        }
        String jsonBody = json.toString();
        return postRequest(url, jsonBody, false);
    }

    /**
     * Login with email and password.
     * Returns JSON with access_token, refresh_token, and user info.
     */
    public String signIn(String email, String password) {
        String url = baseUrl + "/auth/v1/token?grant_type=password";
        // Dùng JsonObject để tránh lỗi khi email/password có ký tự đặc biệt.
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        json.addProperty("password", password);
        String jsonBody = json.toString();
        return postRequest(url, jsonBody, false);
    }

    /**
     * Send password reset email.
     */
    public String resetPassword(String email) {
        String url = baseUrl + "/auth/v1/recover";
        String jsonBody = "{\"email\":\"" + email + "\"}";
        return postRequest(url, jsonBody, false);
    }

    /**
     * Sign out (invalidate token).
     */
    public String signOut() {
        String url = baseUrl + "/auth/v1/logout";
        return postRequest(url, "{}", true);
    }

    // ======================== Database Endpoints ========================

    /**
     * Generic GET request to a Supabase table.
     * @param table Table name
     * @param query Query parameters (e.g. "user_id=eq.abc&order=created_at.desc")
     */
    public String getFromTable(String table, String query) {
        String url = baseUrl + "/rest/v1/" + table + "?" + query;
        return getRequest(url);
    }

    /**
     * Generic POST (insert) request to a Supabase table.
     * @param table Table name
     * @param jsonBody JSON body with the data to insert
     */
    public String insertIntoTable(String table, String jsonBody) {
        String url = baseUrl + "/rest/v1/" + table;
        return postRequest(url, jsonBody, true);
    }

    /**
     * Generic PATCH (update) request to a Supabase table row.
     * @param table Table name
     * @param id Row ID to update
     * @param jsonBody JSON body with the updated fields
     */
    public String updateInTable(String table, String id, String jsonBody) {
        String url = baseUrl + "/rest/v1/" + table + "?id=eq." + id;
        return patchRequest(url, jsonBody);
    }

    /**
     * Generic DELETE request to a Supabase table row.
     * @param table Table name
     * @param id Row ID to delete
     */
    public String deleteFromTable(String table, String id) {
        String url = baseUrl + "/rest/v1/" + table + "?id=eq." + id;
        return deleteRequest(url);
    }

    // ======================== Storage ========================

    /**
     * Upload a file to Supabase Storage.
     * @param bucket Storage bucket name
     * @param path File path within the bucket (e.g. "userId/filename.pdf")
     * @param fileBytes Raw file bytes
     * @param contentType MIME type (e.g. "application/pdf")
     */
    public String uploadFile(String bucket, String path, byte[] fileBytes, String contentType) {
        String url = baseUrl + "/storage/v1/object/" + bucket + "/" + path;
        try {
            RequestBody body = RequestBody.create(fileBytes, MediaType.parse(contentType));
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("apikey", anonKey)
                    .addHeader("Content-Type", contentType);

            // Storage cần bearer token; sau login dùng access token, chưa login dùng anon key.
            builder.addHeader("Authorization", "Bearer " + getBearerToken());

            Request request = builder.build();
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Storage upload failed: " + response.code() + " " + responseBody);
                    return null;
                }
                return responseBody;
            }
        } catch (IOException e) {
            Log.e(TAG, "Storage upload error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the public URL for a stored file.
     */
    public String getFilePublicUrl(String bucket, String path) {
        return baseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }

    // ======================== Private HTTP Methods ========================

    private String getRequest(String url) {
        try {
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("apikey", anonKey)
                    // Supabase REST cần cả apikey và Authorization header.
                    .addHeader("Authorization", "Bearer " + getBearerToken())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");

            try (Response response = httpClient.newCall(builder.build()).execute()) {
                if (response.body() != null) return response.body().string();
            }
        } catch (IOException e) {
            Log.e(TAG, "GET error: " + e.getMessage());
        }
        return null;
    }

    private String postRequest(String url, String jsonBody, boolean useAuth) {
        try {
            RequestBody body = RequestBody.create(
                    jsonBody, MediaType.parse("application/json; charset=utf-8"));

            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("apikey", anonKey)
                    // Auth public request dùng anon key; thao tác DB dùng access token nếu đã login.
                    .addHeader("Authorization", "Bearer " + (useAuth ? getBearerToken() : anonKey))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");

            try (Response response = httpClient.newCall(builder.build()).execute()) {
                if (response.body() != null) return response.body().string();
            }
        } catch (IOException e) {
            Log.e(TAG, "POST error: " + e.getMessage());
        }
        return null;
    }

    private String patchRequest(String url, String jsonBody) {
        try {
            RequestBody body = RequestBody.create(
                    jsonBody, MediaType.parse("application/json; charset=utf-8"));

            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .patch(body)
                    .addHeader("apikey", anonKey)
                    // PATCH cập nhật dữ liệu nên ưu tiên access token của user hiện tại.
                    .addHeader("Authorization", "Bearer " + getBearerToken())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");

            try (Response response = httpClient.newCall(builder.build()).execute()) {
                if (response.body() != null) return response.body().string();
            }
        } catch (IOException e) {
            Log.e(TAG, "PATCH error: " + e.getMessage());
        }
        return null;
    }

    private String deleteRequest(String url) {
        try {
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("apikey", anonKey)
                    // DELETE cũng cần token để Supabase kiểm tra policy/RLS.
                    .addHeader("Authorization", "Bearer " + getBearerToken())
                    .addHeader("Content-Type", "application/json");

            try (Response response = httpClient.newCall(builder.build()).execute()) {
                return response.isSuccessful() ? "success" : "error:" + response.code();
            }
        } catch (IOException e) {
            Log.e(TAG, "DELETE error: " + e.getMessage());
        }
        return null;
    }

    private String getBearerToken() {
        // Nếu user chưa login thì dùng anon key cho request public như signup/login.
        return accessToken != null && !accessToken.isEmpty() ? accessToken : anonKey;
    }
}
