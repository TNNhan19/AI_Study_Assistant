package com.example.aistudyassistant.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etEmail;
    private MaterialButton btnSave, btnChangeAvatar;
    private ProgressBar progressBar;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        initViews();
        loadCurrentProfile();
        setupClickListeners();
    }

    private void initViews() {
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        btnSave = findViewById(R.id.btn_save);
        btnChangeAvatar = findViewById(R.id.btn_change_avatar);
        progressBar = findViewById(R.id.progress_bar);
        btnBack = findViewById(R.id.btn_back);
    }

    private void loadCurrentProfile() {
        SharedPrefManager prefs = SharedPrefManager.getInstance(this);
        etFullName.setText(prefs.getUserName());
        etEmail.setText(prefs.getUserEmail());
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveProfile());
        btnChangeAvatar.setOnClickListener(v ->
                Toast.makeText(this, "Avatar upload coming soon!", Toast.LENGTH_SHORT).show());
    }

    private void saveProfile() {
        String newName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        if (TextUtils.isEmpty(newName)) {
            etFullName.setError("Name cannot be empty");
            return;
        }

        setLoading(true);

        // TODO: Update profile in Supabase
        // new Thread(() -> {
        //     String userId = SharedPrefManager.getInstance(this).getUserId();
        //     String json = "{\"full_name\":\"" + newName + "\"}";
        //     SupabaseClient.getInstance().updateInTable(Constants.TABLE_USERS, userId, json);
        //     runOnUiThread(() -> {
        //         SharedPrefManager.getInstance(this).updateUserName(newName);
        //         setLoading(false);
        //         Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
        //         finish();
        //     });
        // }).start();

        // Without API: just save locally
        SharedPrefManager.getInstance(this).updateUserName(newName);
        setLoading(false);
        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }
}
