package com.example.aistudyassistant.activities;

import android.os.Bundle;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.adapters.ChatMessageAdapter;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.models.ChatContextScope;
import com.example.aistudyassistant.models.ChatMessage;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.models.LearningContext;
import com.example.aistudyassistant.repositories.ChatRepository;
import com.example.aistudyassistant.repositories.DocumentRepository;
import com.example.aistudyassistant.services.AIProcessingService;
import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DocumentChatActivity extends AppCompatActivity {

    private static final String TAG = "DocumentChatActivity";

    private RecyclerView rvMessages;
    private TextInputEditText etMessage;
    private FloatingActionButton btnSend;
    private LinearLayout layoutTyping;
    private TextView tvDocName;
    private TextView tvContextStatus;
    private ImageButton btnBack;
    private MaterialButtonToggleGroup contextToggleGroup;
    private MaterialButton btnContextDocument;
    private MaterialButton btnContextTopic;
    private MaterialButton btnContextProject;

    private ChatMessageAdapter adapter;
    private AIProcessingService aiProcessingService;
    private ChatRepository chatRepository;
    private DocumentRepository documentRepository;
    private KeyListener messageKeyListener;

    private final List<ChatMessage> messages = new ArrayList<>();
    private final List<ChatMessage> conversationHistory = new ArrayList<>();
    private final Map<ChatContextScope, LearningContext> contextCache =
            new EnumMap<>(ChatContextScope.class);

    private Document currentDocument;
    private LearningContext activeContext;
    private ChatContextScope selectedScope = ChatContextScope.DOCUMENT;
    private String userId;
    private long contextGeneration;
    private long requestGeneration;
    private boolean requestInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_chat);

        userId = SharedPrefManager.getInstance(this).getUserId();
        initViews();
        setupRecyclerView();
        setupSendButton();
        setupContextSelector();
        loadCurrentDocument();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        layoutTyping = findViewById(R.id.layout_typing);
        tvDocName = findViewById(R.id.tv_doc_name);
        tvContextStatus = findViewById(R.id.tv_context_status);
        btnBack = findViewById(R.id.btn_back);
        contextToggleGroup = findViewById(R.id.context_toggle_group);
        btnContextDocument = findViewById(R.id.btn_context_document);
        btnContextTopic = findViewById(R.id.btn_context_topic);
        btnContextProject = findViewById(R.id.btn_context_project);

        aiProcessingService = AIProcessingService.getInstance(this);
        chatRepository = ChatRepository.getInstance();
        documentRepository = DocumentRepository.getInstance();
        messageKeyListener = etMessage.getKeyListener();

        String documentName = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_NAME);
        tvDocName.setText(hasValue(documentName) ? documentName : "Tài liệu");
        btnBack.setOnClickListener(v -> finish());
        setQuestionInputEnabled(false);
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter(this, messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
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

    private void setupContextSelector() {
        contextToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || currentDocument == null || requestInProgress) return;
            ChatContextScope scope = scopeFromButton(checkedId);
            if (scope != null && scope != selectedScope) {
                loadScope(scope);
            } else if (activeContext == null && scope != null) {
                loadScope(scope);
            }
        });
    }

    private void loadCurrentDocument() {
        String documentId = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_ID);
        if (!hasValue(documentId)) {
            showFatalError("Thiếu mã tài liệu");
            return;
        }

        tvContextStatus.setText("Đang tải thông tin tài liệu...");
        setScopeButtonsEnabled(false);
        documentRepository.getDocumentById(documentId, new ApiCallback<Document>() {
            @Override
            public void onSuccess(Document document) {
                runOnUiThread(() -> initializeDocument(document));
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Document fallback = createDocumentFromIntent(documentId);
                    if (hasValue(fallback.getFilePath())) {
                        Toast.makeText(DocumentChatActivity.this,
                                "Không tải được metadata mới nhất, đang dùng dữ liệu màn hình trước",
                                Toast.LENGTH_LONG).show();
                        initializeDocument(fallback);
                    } else {
                        showFatalError(errorMessage);
                    }
                });
            }
        });
    }

    private Document createDocumentFromIntent(String documentId) {
        Document document = new Document();
        document.setId(documentId);
        document.setUserId(userId);
        document.setName(getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_NAME));
        document.setFilePath(getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_URL));
        document.setFileType(getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_TYPE));
        document.setProjectId(getIntent().getStringExtra(Constants.EXTRA_PROJECT_ID));
        document.setTopicId(getIntent().getStringExtra(Constants.EXTRA_TOPIC_ID));
        return document;
    }

    private void initializeDocument(Document document) {
        currentDocument = document;
        tvDocName.setText(hasValue(document.getName()) ? document.getName() : "Tài liệu");
        btnContextTopic.setVisibility(hasValue(document.getTopicId())
                ? View.VISIBLE : View.GONE);
        btnContextProject.setVisibility(hasValue(document.getProjectId())
                ? View.VISIBLE : View.GONE);
        setScopeButtonsEnabled(true);

        // Document là phạm vi mặc định và luôn khả dụng.
        selectedScope = ChatContextScope.DOCUMENT;
        contextToggleGroup.check(R.id.btn_context_document);
    }

    private void loadScope(ChatContextScope scope) {
        selectedScope = scope;
        activeContext = null;
        conversationHistory.clear();
        adapter.clearMessages();
        showTypingIndicator(false);
        setInteractionEnabled(false);

        long generation = ++contextGeneration;
        LearningContext cached = contextCache.get(scope);
        if (cached != null) {
            applyContext(cached, generation);
            return;
        }

        tvContextStatus.setText("Đang đọc ngữ cảnh "
                + scope.getDisplayName().toLowerCase(Locale.getDefault()) + "...");
        aiProcessingService.prepareLearningContext(currentDocument, scope,
                new ApiCallback<LearningContext>() {
                    @Override
                    public void onSuccess(LearningContext context) {
                        if (!isActivityActive() || generation != contextGeneration) return;
                        contextCache.put(scope, context);
                        applyContext(context, generation);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (!isActivityActive() || generation != contextGeneration) return;
                        showContextError(errorMessage);
                    }
                });
    }

    private void applyContext(LearningContext context, long generation) {
        activeContext = context;
        updateContextStatus(context);
        loadHistory(generation);
    }

    private void loadHistory(long generation) {
        tvContextStatus.setText("Đang tải lịch sử "
                + selectedScope.getDisplayName().toLowerCase(Locale.getDefault()) + "...");
        chatRepository.getHistory(userId, currentDocument, selectedScope,
                new ApiCallback<List<ChatMessage>>() {
                    @Override
                    public void onSuccess(List<ChatMessage> history) {
                        runOnUiThread(() -> {
                            if (!isActivityActive() || generation != contextGeneration) return;
                            displayConversation(history);
                            updateContextStatus(activeContext);
                            setInteractionEnabled(true);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            if (!isActivityActive() || generation != contextGeneration) return;
                            displayConversation(new ArrayList<>());
                            updateContextStatus(activeContext);
                            setInteractionEnabled(true);
                            Toast.makeText(DocumentChatActivity.this,
                                    "Không tải được lịch sử: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void displayConversation(List<ChatMessage> history) {
        adapter.clearMessages();
        conversationHistory.clear();
        showWelcomeMessage();
        for (ChatMessage message : history) {
            conversationHistory.add(message);
            adapter.addMessage(message);
        }
        scrollToLatestMessage();
    }

    private void showWelcomeMessage() {
        String scopeName = selectedScope.getDisplayName().toLowerCase(Locale.getDefault());
        int sourceCount = activeContext != null ? activeContext.getSourceCount() : 0;
        addDisplayMessage(new ChatMessage(
                "Tôi đã đọc " + sourceCount + " tài liệu trong phạm vi " + scopeName
                        + ". Bạn có thể hỏi nội dung, yêu cầu giải thích hoặc đặt câu hỏi tiếp nối.",
                Constants.MSG_TYPE_AI
        ));
    }

    private void sendMessage() {
        String question = etMessage.getText() != null
                ? etMessage.getText().toString().trim() : "";
        if (question.isEmpty() || requestInProgress || activeContext == null) return;

        List<ChatMessage> previousTurns = new ArrayList<>(conversationHistory);
        ChatMessage userMessage = createMessage(question, Constants.MSG_TYPE_USER);
        conversationHistory.add(userMessage);
        addDisplayMessage(userMessage);
        persistMessage(userMessage);
        etMessage.setText("");

        requestInProgress = true;
        long requestId = ++requestGeneration;
        setInteractionEnabled(false);
        showTypingIndicator(true);

        aiProcessingService.answerWithLearningContext(
                activeContext, previousTurns, question, new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String response) {
                        if (!isActivityActive() || requestId != requestGeneration) return;
                        requestInProgress = false;
                        showTypingIndicator(false);
                        setInteractionEnabled(true);

                        ChatMessage assistantMessage = createMessage(
                                response, Constants.MSG_TYPE_AI);
                        conversationHistory.add(assistantMessage);
                        addDisplayMessage(assistantMessage);
                        persistMessage(assistantMessage);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (!isActivityActive() || requestId != requestGeneration) return;
                        requestInProgress = false;
                        showTypingIndicator(false);
                        setInteractionEnabled(true);
                        addDisplayMessage(new ChatMessage(
                                "Không thể nhận phản hồi từ AI. " + errorMessage,
                                Constants.MSG_TYPE_AI));
                    }
                });
    }

    private ChatMessage createMessage(String content, int type) {
        String documentId = selectedScope == ChatContextScope.DOCUMENT
                ? currentDocument.getId() : null;
        ChatMessage message = new ChatMessage(content, type, documentId);
        message.setUserId(userId);
        return message;
    }

    private void persistMessage(ChatMessage message) {
        chatRepository.saveMessage(userId, currentDocument, selectedScope, message,
                new ApiCallback<ChatMessage>() {
                    @Override
                    public void onSuccess(ChatMessage savedMessage) {
                        message.setId(savedMessage.getId());
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.w(TAG, "Không thể lưu lịch sử chat: " + errorMessage);
                    }
                });
    }

    private void addDisplayMessage(ChatMessage message) {
        adapter.addMessage(message);
        scrollToLatestMessage();
    }

    private void scrollToLatestMessage() {
        if (!messages.isEmpty()) {
            rvMessages.smoothScrollToPosition(messages.size() - 1);
        }
    }

    private void updateContextStatus(LearningContext context) {
        if (context == null) return;
        tvContextStatus.setText(context.getScope().getDisplayName()
                + " • " + context.getSourceCount() + " tài liệu nguồn");
    }

    private void showContextError(String errorMessage) {
        tvContextStatus.setText("Không thể chuẩn bị ngữ cảnh");
        adapter.clearMessages();
        conversationHistory.clear();
        addDisplayMessage(new ChatMessage(
                "Không thể đọc phạm vi đã chọn: " + errorMessage,
                Constants.MSG_TYPE_AI));
        setQuestionInputEnabled(false);
        setScopeButtonsEnabled(true);
    }

    private void showFatalError(String errorMessage) {
        tvContextStatus.setText("Không thể mở trò chuyện");
        adapter.clearMessages();
        addDisplayMessage(new ChatMessage(errorMessage, Constants.MSG_TYPE_AI));
        setQuestionInputEnabled(false);
        setScopeButtonsEnabled(false);
    }

    private void setInteractionEnabled(boolean enabled) {
        setQuestionInputEnabled(enabled);
        setScopeButtonsEnabled(enabled);
    }

    private void setQuestionInputEnabled(boolean enabled) {
        // KeyListener kiểm soát cả bàn phím ảo và bàn phím laptop.
        etMessage.setKeyListener(enabled ? messageKeyListener : null);
        etMessage.setEnabled(enabled);
        etMessage.setFocusable(enabled);
        etMessage.setFocusableInTouchMode(enabled);
        etMessage.setCursorVisible(enabled);
        btnSend.setEnabled(enabled);
        btnSend.setClickable(enabled);
        btnSend.setAlpha(enabled ? 1f : 0.45f);

        if (!enabled) {
            etMessage.clearFocus();
            InputMethodManager keyboard = (InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            if (keyboard != null) {
                keyboard.hideSoftInputFromWindow(etMessage.getWindowToken(), 0);
            }
        }
    }

    private void setScopeButtonsEnabled(boolean enabled) {
        btnContextDocument.setEnabled(enabled);
        btnContextTopic.setEnabled(enabled);
        btnContextProject.setEnabled(enabled);
    }

    private void showTypingIndicator(boolean show) {
        layoutTyping.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private ChatContextScope scopeFromButton(int checkedId) {
        if (checkedId == R.id.btn_context_document) return ChatContextScope.DOCUMENT;
        if (checkedId == R.id.btn_context_topic) return ChatContextScope.TOPIC;
        if (checkedId == R.id.btn_context_project) return ChatContextScope.PROJECT;
        return null;
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isActivityActive() {
        return !isFinishing() && !isDestroyed();
    }
}
