package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DELAY_MS = 800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(
                this::routeUser, SPLASH_DELAY_MS);
    }

    private void routeUser() {
        SessionManager sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            navigateTo(LoginActivity.class);
            return;
        }

        // Refresh trên luồng nền trước khi cho người dùng vào ứng dụng.
        new Thread(() -> {
            if (sessionManager.refreshSession()) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        navigateTo(MainActivity.class);
                    }
                });
            }
            // Refresh lỗi thì SessionManager tự xóa phiên và mở Login.
        }).start();
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
