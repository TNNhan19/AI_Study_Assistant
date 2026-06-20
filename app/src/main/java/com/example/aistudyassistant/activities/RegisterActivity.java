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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private ProgressBar progressBar;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        tilFullName = findViewById(R.id.til_full_name);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progress_bar);
        tvLogin = findViewById(R.id.tv_login);
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null
                ? etConfirmPassword.getText().toString().trim() : "";

        boolean isValid = true;

        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError("Name is required");
            isValid = false;
        } else {
            tilFullName.setError(null);
        }

        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email");
            isValid = false;
        } else {
            tilEmail.setError(null);
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            isValid = false;
        } else {
            tilConfirmPassword.setError(null);
        }

        if (!isValid) return;

        setLoading(true);

        final String finalFullName = fullName;
        new Thread(() -> {
            // Build JSON with user metadata
            String jsonBody = "{\"email\":\"" + email + "\","
                    + "\"password\":\"" + password + "\","
                    + "\"data\":{\"full_name\":\"" + finalFullName + "\"}}";

            // Use Supabase signUp endpoint directly
            String response = SupabaseClient.getInstance().signUp(email, password);

            runOnUiThread(() -> {
                setLoading(false);
                handleRegisterResponse(response);
            });
        }).start();
    }

    private void handleRegisterResponse(String response) {
        if (response == null) {
            Toast.makeText(this, "Registration failed. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            if (json.has("error")) {
                String error = json.has("msg") ? json.get("msg").getAsString() : "Registration failed";
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this,
                    "Account created! Please check your email to verify your account.",
                    Toast.LENGTH_LONG).show();
            finish(); // Go back to login
        } catch (Exception e) {
            Toast.makeText(this, "Registration error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? "Creating account..." : "Create Account");
    }
}
