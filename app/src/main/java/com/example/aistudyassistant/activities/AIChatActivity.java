package com.example.aistudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.KeyListener;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.adapters.ChatMessageAdapter;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.models.ChatMessage;
import com.example.aistudyassistant.services.AIProcessingService;
import com.example.aistudyassistant.utils.Constants;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AIChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private TextInputEditText etMessage;
    private FloatingActionButton btnSend;
    private ImageButton btnNewChat;
    private LinearLayout layoutTyping;
    private BottomNavigationView bottomNavigation;

    private ChatMessageAdapter adapter;
    private AIProcessingService aiProcessingService;
    private KeyListener messageKeyListener;
    private final List<ChatMessage> messages = new ArrayList<>();
    private boolean requestInProgress;
    private long activeRequestId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initViews();
        setupRecyclerView();
        setupSendButton();
        setupNewChatButton();
        setupBottomNavigation();
        showWelcomeMessage();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnNewChat = findViewById(R.id.btn_new_chat);
        layoutTyping = findViewById(R.id.layout_typing);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        aiProcessingService = AIProcessingService.getInstance(this);
        messageKeyListener = etMessage.getKeyListener();
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter(this, messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
    }

    private void showWelcomeMessage() {
        addAiMessage("Xin chào! Tôi là trợ lý học tập AI. Bạn có thể hỏi kiến thức "
                + "hoặc yêu cầu giải thích một khái niệm bất kỳ.\n\n"
                + "Ví dụ:\n- TCP/IP là gì?\n- Giải thích chuẩn hóa cơ sở dữ liệu\n"
                + "- RecyclerView hoạt động như thế nào?");
    }

    private void setupSendButton() {
        btnSend.setOnClickListener(v -> sendMessage());
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND
                    || actionId == EditorInfo.IME_ACTION_DONE) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void setupNewChatButton() {
        btnNewChat.setOnClickListener(v -> {
            // Tăng mã phiên để bỏ qua phản hồi cũ nếu AI vẫn đang xử lý.
            activeRequestId++;
            setRequestInProgress(false);
            adapter.clearMessages();
            etMessage.setText("");
            showWelcomeMessage();
        });
    }

    private void sendMessage() {
        String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (text.isEmpty() || requestInProgress) return;

        etMessage.setText("");
        addUserMessage(text);
        setRequestInProgress(true);
        long requestId = ++activeRequestId;

        // Service xử lý mạng ở background thread và trả kết quả về UI thread.
        aiProcessingService.answerStudyQuestion(text, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                if (!isActivityActive() || requestId != activeRequestId) return;
                setRequestInProgress(false);
                addAiMessage(response);
            }

            @Override
            public void onError(String errorMessage) {
                if (!isActivityActive() || requestId != activeRequestId) return;
                setRequestInProgress(false);
                addAiMessage("Không thể nhận phản hồi từ AI. " + errorMessage);
            }

            @Override
            public void onWaitingForNetwork() {
                if (!isActivityActive() || requestId != activeRequestId) return;
                addAiMessage("Đã mất kết nối. Câu hỏi sẽ tự động được gửi lại khi có mạng.");
            }
        });
    }

    private void addUserMessage(String text) {
        ChatMessage message = new ChatMessage(text, Constants.MSG_TYPE_USER);
        adapter.addMessage(message);
        rvMessages.smoothScrollToPosition(messages.size() - 1);
    }

    private void addAiMessage(String text) {
        ChatMessage message = new ChatMessage(text, Constants.MSG_TYPE_AI);
        adapter.addMessage(message);
        rvMessages.smoothScrollToPosition(messages.size() - 1);
    }

    private void setRequestInProgress(boolean inProgress) {
        requestInProgress = inProgress;
        layoutTyping.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        // KeyListener null chặn cả bàn phím ảo lẫn bàn phím laptop.
        etMessage.setKeyListener(inProgress ? null : messageKeyListener);
        etMessage.setEnabled(!inProgress);
        etMessage.setFocusable(!inProgress);
        etMessage.setFocusableInTouchMode(!inProgress);
        etMessage.setCursorVisible(!inProgress);
        btnSend.setEnabled(!inProgress);
        btnSend.setClickable(!inProgress);
        btnSend.setAlpha(inProgress ? 0.45f : 1f);

        if (inProgress) {
            // Ngắt kết nối bàn phím để không nhận thêm ký tự khi đang chờ AI.
            etMessage.clearFocus();
            InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(etMessage.getWindowToken(), 0);
        }
    }

    private boolean isActivityActive() {
        return !isFinishing() && !isDestroyed();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_chat);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_chat) return true;
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            if (id == R.id.nav_documents) {
                startActivity(new Intent(this, DocumentsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            if (id == R.id.nav_schedule) {
                startActivity(new Intent(this, ScheduleActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }
}
