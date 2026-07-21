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
    private static final int MAX_RECENT_DOCS = 10;

    public void addRecentDocument(String documentId) {
        if (documentId == null || documentId.isEmpty()) return;
        
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
        
        // Join and save
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recentList.size(); i++) {
            sb.append(recentList.get(i));
            if (i < recentList.size() - 1) sb.append(",");
        }
        prefs.edit().putString(KEY_RECENT_DOCS, sb.toString()).apply();
    }

    public java.util.List<String> getRecentDocumentIds() {
        String recentStr = prefs.getString(KEY_RECENT_DOCS, "");
        if (recentStr.isEmpty()) return new java.util.ArrayList<>();
        return new java.util.ArrayList<>(java.util.Arrays.asList(recentStr.split(",")));
    }
}
