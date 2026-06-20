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
                .apply();
    }

    public void clearSession() {
        prefs.edit()
                .remove(Constants.PREF_USER_ID)
                .remove(Constants.PREF_USER_EMAIL)
                .remove(Constants.PREF_USER_NAME)
                .remove(Constants.PREF_ACCESS_TOKEN)
                .remove(Constants.PREF_REFRESH_TOKEN)
                .putBoolean(Constants.PREF_IS_LOGGED_IN, false)
                .apply();
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
}
