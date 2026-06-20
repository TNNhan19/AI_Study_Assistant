package com.example.aistudyassistant.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class DocumentChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private TextInputEditText etMessage;
    private FloatingActionButton btnSend;
    private LinearLayout layoutTyping;
    private TextView tvDocName;
    private ImageButton btnBack;

    private ChatMessageAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();

    private String documentId;
    private String documentName;
    private String documentUrl;
    private String documentContext; // Cached document text for Gemini

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_chat);

        documentId = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_ID);
        documentName = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_NAME);
        documentUrl = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_URL);

        initViews();
        setupRecyclerView();
        setupSendButton();
        showWelcomeMessage();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        layoutTyping = findViewById(R.id.layout_typing);
        tvDocName = findViewById(R.id.tv_doc_name);
        btnBack = findViewById(R.id.btn_back);

        tvDocName.setText(documentName != null ? documentName : "Document");
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter(this, messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
    }

    private void showWelcomeMessage() {
        addAiMessage("Hello! 📄 I'm ready to help you understand **" + documentName + "**.\n\n"
                + "You can ask me:\n"
                + "• Explain this document in simple terms\n"
                + "• What are the key concepts?\n"
                + "• Give me examples from this lesson\n"
                + "• What should I remember most?");
    }

    private void setupSendButton() {
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        etMessage.setText("");
        addUserMessage(text);
        showTypingIndicator(true);

        final String question = text;
        new Thread(() -> {
            // TODO: Load actual document text if not cached
            if (documentContext == null) {
                documentContext = "Sample document content. TODO: Load actual document from " + documentUrl;
            }

            String response = GeminiClient.getInstance().chatWithDocument(documentContext, question);
            runOnUiThread(() -> {
                showTypingIndicator(false);
                addAiMessage(response != null ? response
                        : "Sorry, I couldn't get a response. Check your API key.");
            });
        }).start();
    }

    private void addUserMessage(String text) {
        messages.add(new ChatMessage(text, Constants.MSG_TYPE_USER, documentId));
        adapter.notifyItemInserted(messages.size() - 1);
        rvMessages.smoothScrollToPosition(messages.size() - 1);
    }

    private void addAiMessage(String text) {
        messages.add(new ChatMessage(text, Constants.MSG_TYPE_AI, documentId));
        adapter.notifyItemInserted(messages.size() - 1);
        rvMessages.smoothScrollToPosition(messages.size() - 1);
    }

    private void showTypingIndicator(boolean show) {
        layoutTyping.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
