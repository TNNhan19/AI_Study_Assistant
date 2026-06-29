package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private TextView tvForgotPassword, tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SharedPrefManager.getInstance(this).isLoggedIn()) {
            // Khôi phục access token để các request Supabase sau khi mở lại app vẫn qua RLS.
            String accessToken = SharedPrefManager.getInstance(this).getAccessToken();
            SupabaseClient.getInstance().setAccessToken(accessToken);

            navigateToHome();
            return;
        }

        setContentView(R.layout.activity_login);
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvRegister = findViewById(R.id.tv_register);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        // Validation
        boolean isValid = true;
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email");
            isValid = false;
        } else {
            tilEmail.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        if (!isValid) return;

        setLoading(true);

        // API call on background thread
        new Thread(() -> {
            String response = SupabaseClient.getInstance().signIn(email, password);
            runOnUiThread(() -> {
                setLoading(false);
                handleLoginResponse(response);
            });
        }).start();
    }

    private void handleLoginResponse(String response) {
        if (response == null) {
            Toast.makeText(this, "Login failed. Check your credentials.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();

            // Supabase có thể trả lỗi bằng error/code/message, không phải lúc nào cũng có access_token.
            if (json.has("error") || json.has("code") || json.has("message")) {
                String error = getErrorMessage(json);
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!json.has("access_token") || !json.has("refresh_token") || !json.has("user")) {
                Toast.makeText(this, "Login failed: invalid Supabase response", Toast.LENGTH_SHORT).show();
                return;
            }

            String accessToken = json.get("access_token").getAsString();
            String refreshToken = json.get("refresh_token").getAsString();
            JsonObject user = json.getAsJsonObject("user");
            String userId = user.get("id").getAsString();
            String userEmail = user.get("email").getAsString();

            // Get display name from user_metadata
            String fullName = "Student";
            if (user.has("user_metadata") && !user.get("user_metadata").isJsonNull()) {
                JsonObject metadata = user.getAsJsonObject("user_metadata");
                if (metadata.has("full_name")) {
                    fullName = metadata.get("full_name").getAsString();
                }
            }

            // Save session
            SharedPrefManager.getInstance(this)
                    .saveUserSession(userId, userEmail, fullName, accessToken, refreshToken);
            SupabaseClient.getInstance().setAccessToken(accessToken);

            navigateToHome();

        } catch (Exception e) {
            Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getErrorMessage(JsonObject json) {
        // Ưu tiên message cụ thể để biết lỗi do email chưa xác nhận, sai mật khẩu hay key.
        if (json.has("msg") && !json.get("msg").isJsonNull()) {
            return json.get("msg").getAsString();
        }
        if (json.has("error_description") && !json.get("error_description").isJsonNull()) {
            return json.get("error_description").getAsString();
        }
        if (json.has("message") && !json.get("message").isJsonNull()) {
            return json.get("message").getAsString();
        }
        if (json.has("error") && !json.get("error").isJsonNull()) {
            return json.get("error").getAsString();
        }
        return "Login failed";
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Signing in..." : "Log In");
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
