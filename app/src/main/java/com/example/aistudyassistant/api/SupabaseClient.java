package com.example.aistudyassistant.api;

import android.util.Log;

import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseClient {

    private static final String TAG = "SupabaseClient";
    private static SupabaseClient instance;

    private final OkHttpClient httpClient;
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

    public String signOut() {
        // Logout không cần refresh một session đã hết hạn.
        return postRequest(baseUrl + "/auth/v1/logout", "{}", true, false);
    }

    // ======================== Database Endpoints ========================

    public String getFromTable(String table, String query) {
        return getRequest(baseUrl + "/rest/v1/" + table + "?" + query);
    }

    public String insertIntoTable(String table, String jsonBody) {
        return postRequest(baseUrl + "/rest/v1/" + table, jsonBody, true);
    }

    public String updateInTable(String table, String id, String jsonBody) {
        return patchRequest(
                baseUrl + "/rest/v1/" + table + "?id=eq." + id,
                jsonBody
        );
    }

    public String deleteFromTable(String table, String id) {
        return deleteRequest(baseUrl + "/rest/v1/" + table + "?id=eq." + id);
    }

    // ======================== Storage ========================

    public String uploadFile(String bucket, String path, byte[] fileBytes,
                             String contentType) {
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

    public String getFilePublicUrl(String bucket, String path) {
        return baseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
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
        HttpResult result = executeOnce(request);
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
        HttpResult retryResult = executeOnce(retryRequest);
        if (isSessionExpired(retryResult)) {
            notifySessionExpired();
        }
        return retryResult;
    }

    private HttpResult executeOnce(Request request) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            return new HttpResult(response.code(), response.isSuccessful(), body);
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
}
