package com.example.aistudyassistant.api;

import android.util.Log;

import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.HttpUrl;
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
    private final OkHttpClient edgeFunctionClient;
    private final String baseUrl;
    private final String anonKey;
    private final Object refreshLock = new Object();

    private volatile String accessToken;
    private volatile String refreshToken;
    private volatile SessionListener sessionListener;

    public interface SessionListener {
        void onSessionRefreshed(String accessToken, String refreshToken);
        void onSessionExpired();
    }

    private SupabaseClient() {
        this.baseUrl = Constants.SUPABASE_URL;
        this.anonKey = Constants.SUPABASE_ANON_KEY;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.edgeFunctionClient = httpClient.newBuilder()
                // Tác vụ AI có thể cần nhiều thời gian hơn request CRUD.
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
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

    public void setSession(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public void clearSession() {
        this.accessToken = null;
        this.refreshToken = null;
    }

    public void setSessionListener(SessionListener listener) {
        this.sessionListener = listener;
    }

    // ======================== Auth Endpoints ========================

    /**
     * Register a new user with email and password.
     * Returns the raw JSON response from Supabase Auth.
     */
    public String signUp(String email, String password) {
        return signUp(email, password, null);
    }

    public String signUp(String email, String password, String fullName) {
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        json.addProperty("password", password);
        if (fullName != null && !fullName.trim().isEmpty()) {
            JsonObject data = new JsonObject();
            data.addProperty("full_name", fullName.trim());
            json.add("data", data);
        }
        return postRequest(baseUrl + "/auth/v1/signup", json.toString(), false);
    }

    public String signIn(String email, String password) {
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        json.addProperty("password", password);
        return postRequest(
                baseUrl + "/auth/v1/token?grant_type=password",
                json.toString(),
                false
        );
    }

    /**
     * Đổi refresh token lấy cặp access/refresh token mới.
     * Hàm blocking nên phải chạy trên background thread.
     */
    public boolean refreshSession() {
        synchronized (refreshLock) {
            return refreshSessionLocked();
        }
    }

    public String resetPassword(String email) {
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        return postRequest(baseUrl + "/auth/v1/recover", json.toString(), false);
    }

    /**
     * Sign out (invalidate token).
     */
    public String signOut() {
        // Logout không cần refresh một session đã hết hạn.
        return postRequest(baseUrl + "/auth/v1/logout", "{}", true, false);
    }

    // ======================== Database Endpoints ========================

    /**
     * Generic GET request to a Supabase table.
     * @param table Table name
     * @param query Query parameters (e.g. "user_id=eq.abc&order=created_at.desc")
     */
    public String getFromTable(String table, String query) {
        return getRequest(baseUrl + "/rest/v1/" + table + "?" + query);
    }

    /**
     * Generic POST (insert) request to a Supabase table.
     * @param table Table name
     * @param jsonBody JSON body with the data to insert
     */
    public String insertIntoTable(String table, String jsonBody) {
        return postRequest(baseUrl + "/rest/v1/" + table, jsonBody, true);
    }

    /**
     * Generic PATCH (update) request to a Supabase table row.
     * @param table Table name
     * @param id Row ID to update
     * @param jsonBody JSON body with the updated fields
     */
    public String updateInTable(String table, String id, String jsonBody) {
        return patchRequest(
                baseUrl + "/rest/v1/" + table + "?id=eq." + id,
                jsonBody
        );
    }

    /**
     * Generic DELETE request to a Supabase table row.
     * @param table Table name
     * @param id Row ID to delete
     */
    public String deleteFromTable(String table, String id) {
        return deleteRequest(baseUrl + "/rest/v1/" + table + "?id=eq." + id);
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
            String requestToken = getBearerToken();
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer " + requestToken)
                    .addHeader("Content-Type", contentType)
                    .build();

            HttpResult result = executeWithRefresh(request, true, requestToken);
            if (!result.successful) {
                Log.e(TAG, "Storage upload failed: " + result.code + " " + result.body);
                return null;
            }
            return result.body;
        } catch (IOException e) {
            Log.e(TAG, "Storage upload error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Downloads the original bytes from a private Storage bucket. The authenticated
     * endpoint is required because the documents bucket is intentionally private.
     */
    public byte[] downloadFile(String bucket, String path) {
        if (bucket == null || bucket.trim().isEmpty()
                || path == null || path.trim().isEmpty()) {
            return null;
        }

        HttpUrl.Builder urlBuilder = HttpUrl.get(baseUrl).newBuilder()
                .addPathSegments("storage/v1/object/authenticated")
                .addPathSegment(bucket);
        for (String pathSegment : path.split("/")) {
            if (!pathSegment.isEmpty()) {
                urlBuilder.addPathSegment(pathSegment);
            }
        }

        String requestToken = getBearerToken();
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + requestToken)
                .build();

        try {
            StorageDownloadResult result = executeDownloadOnce(request);
            if (result.successful) return result.body;

            HttpResult error = new HttpResult(result.code, false, result.errorBody);
            if (isSessionExpired(error) && refreshAfterFailure(requestToken)) {
                Request retryRequest = request.newBuilder()
                        .header("Authorization", "Bearer " + getBearerToken())
                        .build();
                StorageDownloadResult retryResult = executeDownloadOnce(retryRequest);
                if (retryResult.successful) return retryResult.body;
                if (isSessionExpired(new HttpResult(
                        retryResult.code, false, retryResult.errorBody))) {
                    notifySessionExpired();
                }
                Log.e(TAG, "Storage download failed: " + retryResult.code);
                return null;
            }

            Log.e(TAG, "Storage download failed: " + result.code);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Storage download error: " + e.getMessage());
            throw new NetworkRequestException("Mất kết nối khi tải tài liệu", e);
        }
    }

    /**
     * Returns the public URL for a stored file.
     */
    public String getFilePublicUrl(String bucket, String path) {
        return baseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }

    // ======================== Edge Functions ========================

    /**
     * Gọi Edge Function bằng session hiện tại và retry một lần sau khi refresh JWT.
     */
    public String invokeEdgeFunction(String functionName, String jsonBody) {
        if (functionName == null || functionName.trim().isEmpty()
                || jsonBody == null || jsonBody.trim().isEmpty()) {
            return null;
        }

        try {
            RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json; charset=utf-8")
            );
            String requestToken = getBearerToken();
            Request request = new Request.Builder()
                    .url(baseUrl + "/functions/v1/" + functionName.trim())
                    .post(body)
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer " + requestToken)
                    .addHeader("Content-Type", "application/json; charset=utf-8")
                    .build();

            HttpResult result = executeWithRefresh(
                    edgeFunctionClient, request, true, requestToken);
            if (!result.successful) {
                Log.e(TAG, "Edge Function failed: " + result.code + " " + result.body);
                return null;
            }
            return result.body;
        } catch (IOException e) {
            Log.e(TAG, "Edge Function error: " + e.getMessage());
            throw new NetworkRequestException("Không thể kết nối dịch vụ AI", e);
        }
    }

    // ======================== HTTP Helpers ========================

    private String getRequest(String url) {
        try {
            String requestToken = getBearerToken();
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer " + requestToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();
            return executeWithRefresh(request, true, requestToken).body;
        } catch (IOException e) {
            Log.e(TAG, "GET error: " + e.getMessage());
            return null;
        }
    }

    private String postRequest(String url, String jsonBody, boolean useAuth) {
        return postRequest(url, jsonBody, useAuth, useAuth);
    }

    private String postRequest(String url, String jsonBody, boolean useAuth,
                               boolean allowRefresh) {
        try {
            RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json; charset=utf-8")
            );
            String requestToken = useAuth ? getBearerToken() : anonKey;
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("apikey", anonKey)
                    // Auth public request dùng anon key; thao tác DB dùng access token nếu đã login.
                    .addHeader("Authorization", "Bearer " + requestToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();
            return executeWithRefresh(request, allowRefresh, requestToken).body;
        } catch (IOException e) {
            Log.e(TAG, "POST error: " + e.getMessage());
            return null;
        }
    }

    private String patchRequest(String url, String jsonBody) {
        try {
            RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json; charset=utf-8")
            );
            String requestToken = getBearerToken();
            Request request = new Request.Builder()
                    .url(url)
                    .patch(body)
                    .addHeader("apikey", anonKey)
                    // PATCH cập nhật dữ liệu nên ưu tiên access token của user hiện tại.
                    .addHeader("Authorization", "Bearer " + requestToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();
            return executeWithRefresh(request, true, requestToken).body;
        } catch (IOException e) {
            Log.e(TAG, "PATCH error: " + e.getMessage());
            return null;
        }
    }

    private String deleteRequest(String url) {
        try {
            String requestToken = getBearerToken();
            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("apikey", anonKey)
                    // DELETE cần token để Supabase kiểm tra RLS.
                    .addHeader("Authorization", "Bearer " + requestToken)
                    .addHeader("Content-Type", "application/json")
                    .build();

            HttpResult result = executeWithRefresh(request, true, requestToken);
            return result.successful ? "success" : "error:" + result.code;
        } catch (IOException e) {
            Log.e(TAG, "DELETE error: " + e.getMessage());
            return null;
        }
    }

    private HttpResult executeWithRefresh(Request request, boolean allowRefresh,
                                          String requestToken) throws IOException {
        return executeWithRefresh(httpClient, request, allowRefresh, requestToken);
    }

    private HttpResult executeWithRefresh(OkHttpClient client, Request request,
                                          boolean allowRefresh,
                                          String requestToken) throws IOException {
        HttpResult result = executeOnce(client, request);
        if (!allowRefresh || !isSessionExpired(result)) {
            return result;
        }

        // Mỗi request chỉ refresh và retry đúng một lần.
        if (!refreshAfterFailure(requestToken)) {
            return result;
        }

        Request retryRequest = request.newBuilder()
                .header("Authorization", "Bearer " + getBearerToken())
                .build();
        HttpResult retryResult = executeOnce(client, retryRequest);
        if (isSessionExpired(retryResult)) {
            notifySessionExpired();
        }
        return retryResult;
    }

    private HttpResult executeOnce(Request request) throws IOException {
        return executeOnce(httpClient, request);
    }

    private HttpResult executeOnce(OkHttpClient client, Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            return new HttpResult(response.code(), response.isSuccessful(), body);
        }
    }

    private StorageDownloadResult executeDownloadOnce(Request request) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                byte[] body = response.body() != null ? response.body().bytes() : new byte[0];
                return new StorageDownloadResult(response.code(), true, body, "");
            }
            String errorBody = response.body() != null ? response.body().string() : "";
            return new StorageDownloadResult(response.code(), false, null, errorBody);
        }
    }

    private boolean refreshAfterFailure(String failedAccessToken) {
        synchronized (refreshLock) {
            // Request khác đã refresh xong thì chỉ cần retry bằng token mới.
            if (accessToken != null && !accessToken.equals(failedAccessToken)) {
                return true;
            }
            return refreshSessionLocked();
        }
    }

    private boolean refreshSessionLocked() {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            notifySessionExpired();
            return false;
        }

        try {
            JsonObject jsonBody = new JsonObject();
            jsonBody.addProperty("refresh_token", refreshToken);
            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );
            Request request = new Request.Builder()
                    .url(baseUrl + "/auth/v1/token?grant_type=refresh_token")
                    .post(body)
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer " + anonKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            HttpResult result = executeOnce(request);
            if (!result.successful || result.body == null || result.body.isEmpty()) {
                notifySessionExpired();
                return false;
            }

            JsonObject json = JsonParser.parseString(result.body).getAsJsonObject();
            if (!json.has("access_token") || !json.has("refresh_token")) {
                notifySessionExpired();
                return false;
            }

            String newAccessToken = json.get("access_token").getAsString();
            String newRefreshToken = json.get("refresh_token").getAsString();
            setSession(newAccessToken, newRefreshToken);

            SessionListener listener = sessionListener;
            if (listener != null) {
                listener.onSessionRefreshed(newAccessToken, newRefreshToken);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Session refresh failed: " + e.getMessage());
            notifySessionExpired();
            return false;
        }
    }

    private boolean isSessionExpired(HttpResult result) {
        if (result.code == 401) return true;
        String body = result.body == null ? "" : result.body.toLowerCase();
        return body.contains("jwt expired")
                || body.contains("token has expired")
                || body.contains("invalid jwt")
                || body.contains("pgrst303");
    }

    private void notifySessionExpired() {
        SessionListener listener = sessionListener;
        if (listener != null) {
            listener.onSessionExpired();
        }
    }

    private String getBearerToken() {
        return accessToken != null && !accessToken.isEmpty() ? accessToken : anonKey;
    }

    private static class HttpResult {
        final int code;
        final boolean successful;
        final String body;

        HttpResult(int code, boolean successful, String body) {
            this.code = code;
            this.successful = successful;
            this.body = body;
        }
    }

    private static class StorageDownloadResult {
        final int code;
        final boolean successful;
        final byte[] body;
        final String errorBody;

        StorageDownloadResult(int code, boolean successful, byte[] body, String errorBody) {
            this.code = code;
            this.successful = successful;
            this.body = body;
            this.errorBody = errorBody;
        }
    }
}
