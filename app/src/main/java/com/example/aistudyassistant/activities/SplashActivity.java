package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.utils.SharedPrefManager;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DELAY_MS = 800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::routeUser,
                SPLASH_DELAY_MS);
    }

    private void routeUser() {
        SharedPrefManager prefs = SharedPrefManager.getInstance(this);

        if (prefs.isLoggedIn()) {
            // Khôi phục access token để request Supabase sau khi mở app vẫn qua RLS.
                    SupabaseClient.getInstance().setAccessToken(prefs.getAccessToken());
            navigateTo(MainActivity.class);
        } else {
            navigateTo(LoginActivity.class);
        }
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}