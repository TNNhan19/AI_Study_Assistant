package com.example.aistudyassistant.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {

    private static SharedPrefManager instance;
    private final SharedPreferences prefs;

    private SharedPrefManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    // ===================== Session Management =====================

    public void saveUserSession(String userId, String email, String name,
                                String accessToken, String refreshToken) {
        prefs.edit()
                .putString(Constants.PREF_USER_ID, userId)
                .putString(Constants.PREF_USER_EMAIL, email)
                .putString(Constants.PREF_USER_NAME, name)
                .putString(Constants.PREF_ACCESS_TOKEN, accessToken)
                .putString(Constants.PREF_REFRESH_TOKEN, refreshToken)
                .putBoolean(Constants.PREF_IS_LOGGED_IN, true)
                .commit();
    }

    public void clearSession() {
        prefs.edit()
                .remove(Constants.PREF_USER_ID)
                .remove(Constants.PREF_USER_EMAIL)
                .remove(Constants.PREF_USER_NAME)
                .remove(Constants.PREF_ACCESS_TOKEN)
                .remove(Constants.PREF_REFRESH_TOKEN)
                .putBoolean(Constants.PREF_IS_LOGGED_IN, false)
                .commit();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
    }

    // ===================== Getters =====================

    public String getUserId() {
        return prefs.getString(Constants.PREF_USER_ID, "");
    }

    public String getUserEmail() {
        return prefs.getString(Constants.PREF_USER_EMAIL, "");
    }

    public String getUserName() {
        return prefs.getString(Constants.PREF_USER_NAME, "Student");
    }

    public String getAccessToken() {
        return prefs.getString(Constants.PREF_ACCESS_TOKEN, "");
    }

    public String getRefreshToken() {
        return prefs.getString(Constants.PREF_REFRESH_TOKEN, "");
    }

    // ===================== Setters =====================

    public void updateUserName(String name) {
        prefs.edit().putString(Constants.PREF_USER_NAME, name).apply();
    }

    public void updateAccessToken(String token) {
        prefs.edit().putString(Constants.PREF_ACCESS_TOKEN, token).apply();
    }

    public void updateSessionTokens(String accessToken, String refreshToken) {
        // Lưu cả hai token vì refresh token có thể được Supabase thay mới.
        prefs.edit()
                .putString(Constants.PREF_ACCESS_TOKEN, accessToken)
                .putString(Constants.PREF_REFRESH_TOKEN, refreshToken)
                .putBoolean(Constants.PREF_IS_LOGGED_IN, true)
                .commit();
    }

    // ===================== Generic helpers =====================

    public void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    public void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    // ===================== Recent Documents =====================

    private static final String KEY_RECENT_DOCS = "recent_documents";
    private static final String KEY_RECENT_DOC_OPENED_AT_PREFIX =
            "recent_document_opened_at_";
    private static final int MAX_RECENT_DOCS = 10;

    public void addRecentDocument(String documentId) {
        if (documentId == null || documentId.trim().isEmpty()) return;
        documentId = documentId.trim();
        
        String recentStr = prefs.getString(KEY_RECENT_DOCS, "");
        java.util.List<String> recentList = new java.util.ArrayList<>();
        if (!recentStr.isEmpty()) {
            recentList.addAll(java.util.Arrays.asList(recentStr.split(",")));
        }
        
        // Remove if already exists to move to top
        recentList.remove(documentId);
        
        // Add to front
        recentList.add(0, documentId);
        
        // Trim if too many
        if (recentList.size() > MAX_RECENT_DOCS) {
            recentList = recentList.subList(0, MAX_RECENT_DOCS);
        }
        
        prefs.edit()
                .putString(KEY_RECENT_DOCS, joinDocumentIds(recentList))
                .putLong(KEY_RECENT_DOC_OPENED_AT_PREFIX + documentId,
                        System.currentTimeMillis())
                .apply();
    }

    public java.util.List<String> getRecentDocumentIds() {
        String recentStr = prefs.getString(KEY_RECENT_DOCS, "");
        if (recentStr.isEmpty()) return new java.util.ArrayList<>();
        return new java.util.ArrayList<>(java.util.Arrays.asList(recentStr.split(",")));
    }

    public long getRecentDocumentOpenedAt(String documentId) {
        if (documentId == null || documentId.isEmpty()) return 0L;
        return prefs.getLong(KEY_RECENT_DOC_OPENED_AT_PREFIX + documentId, 0L);
    }

    public void retainRecentDocumentIds(java.util.List<String> existingDocumentIds) {
        java.util.Set<String> existingIds =
                new java.util.HashSet<>(existingDocumentIds);
        java.util.List<String> recentIds = getRecentDocumentIds();
        java.util.List<String> retainedIds = new java.util.ArrayList<>();
        SharedPreferences.Editor editor = prefs.edit();

        for (String documentId : recentIds) {
            if (existingIds.contains(documentId)) {
                retainedIds.add(documentId);
            } else {
                editor.remove(KEY_RECENT_DOC_OPENED_AT_PREFIX + documentId);
            }
        }

        editor.putString(KEY_RECENT_DOCS, joinDocumentIds(retainedIds)).apply();
    }

    private String joinDocumentIds(java.util.List<String> documentIds) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < documentIds.size(); i++) {
            if (i > 0) result.append(",");
            result.append(documentIds.get(i));
        }
        return result.toString();
    }
}
