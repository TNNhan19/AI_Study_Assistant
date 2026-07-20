package com.example.aistudyassistant.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.example.aistudyassistant.activities.LoginActivity;
import com.example.aistudyassistant.api.SupabaseClient;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SessionManager implements SupabaseClient.SessionListener {
    private static SessionManager instance;

    private final Context appContext;
    private final SharedPrefManager preferences;
    private final SupabaseClient supabaseClient;
    private final AtomicBoolean redirectingToLogin = new AtomicBoolean(false);

    private SessionManager(Context context) {
        appContext = context.getApplicationContext();
        preferences = SharedPrefManager.getInstance(appContext);
        supabaseClient = SupabaseClient.getInstance();
        supabaseClient.setSessionListener(this);
        restoreSession();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public boolean isLoggedIn() {
        return preferences.isLoggedIn();
    }

    public void saveSession(String userId, String email, String name,
                            String accessToken, String refreshToken) {
        preferences.saveUserSession(
                userId, email, name, accessToken, refreshToken);
        supabaseClient.setSession(accessToken, refreshToken);
        redirectingToLogin.set(false);
    }

    public boolean refreshSession() {
        if (!preferences.isLoggedIn()) return false;

        // Khôi phục cặp token từ máy trước khi gọi API refresh.
        restoreSession();
        return supabaseClient.refreshSession();
    }

    public void clearSession() {
        preferences.clearSession();
        supabaseClient.clearSession();
        redirectingToLogin.set(false);
    }

    @Override
    public void onSessionRefreshed(String accessToken, String refreshToken) {
        preferences.updateSessionTokens(accessToken, refreshToken);
        redirectingToLogin.set(false);
    }

    @Override
    public void onSessionExpired() {
        // Chỉ mở một LoginActivity nếu nhiều request cùng báo hết phiên.
        if (!redirectingToLogin.compareAndSet(false, true)) return;

        preferences.clearSession();
        supabaseClient.clearSession();
        new Handler(Looper.getMainLooper()).post(() -> {
            Intent intent = new Intent(appContext, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            appContext.startActivity(intent);
        });
    }

    private void restoreSession() {
        if (preferences.isLoggedIn()) {
            supabaseClient.setSession(
                    preferences.getAccessToken(),
                    preferences.getRefreshToken()
            );
        } else {
            supabaseClient.clearSession();
        }
    }
}
