package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.adapters.ChatMessageAdapter;
import com.example.aistudyassistant.api.GeminiClient;
import com.example.aistudyassistant.models.ChatMessage;
import com.example.aistudyassistant.utils.Constants;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private TextInputEditText etMessage;
    private FloatingActionButton btnSend;
    private LinearLayout layoutTyping;
    private BottomNavigationView bottomNavigation;

    private ChatMessageAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initViews();
        setupRecyclerView();
        setupSendButton();
        setupBottomNavigation();
        showWelcomeMessage();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        layoutTyping = findViewById(R.id.layout_typing);
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter(this, messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
    }

    private void showWelcomeMessage() {
        addAiMessage("Hello! 👋 I'm your AI study assistant. Ask me anything about your studies!\n\nFor example:\n• What is TCP/IP?\n• Explain database normalization\n• How does RecyclerView work in Android?");
    }

    private void setupSendButton() {
        btnSend.setOnClickListener(v -> sendMessage());
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {
        String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        etMessage.setText("");
        addUserMessage(text);
        showTypingIndicator(true);

        new Thread(() -> {
            String response = GeminiClient.getInstance().generalChat(text);
            runOnUiThread(() -> {
                showTypingIndicator(false);
                if (response != null) {
                    addAiMessage(response);
                } else {
                    addAiMessage("Sorry, I couldn't get a response. Please check your API key configuration.");
                }
            });
        }).start();
    }

    private void addUserMessage(String text) {
        ChatMessage msg = new ChatMessage(text, Constants.MSG_TYPE_USER);
        adapter.addMessage(msg);
        rvMessages.smoothScrollToPosition(messages.size() - 1);
    }

    private void addAiMessage(String text) {
        ChatMessage msg = new ChatMessage(text, Constants.MSG_TYPE_AI);
        adapter.addMessage(msg);
        rvMessages.smoothScrollToPosition(messages.size() - 1);
    }

    private void showTypingIndicator(boolean show) {
        layoutTyping.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_chat);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_chat) return true;
            else if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_documents) {
                startActivity(new Intent(this, DocumentsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_schedule) {
                startActivity(new Intent(this, ScheduleActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }
}
