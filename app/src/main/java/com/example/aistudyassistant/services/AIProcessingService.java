package com.example.aistudyassistant.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.aistudyassistant.api.AIClient;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.NetworkRequestException;
import com.example.aistudyassistant.models.AIProcessingResult;
import com.example.aistudyassistant.models.ChatContextScope;
import com.example.aistudyassistant.models.ChatMessage;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.models.DocumentAnalysis;
import com.example.aistudyassistant.models.Flashcard;
import com.example.aistudyassistant.models.LearningContext;
import com.example.aistudyassistant.models.QuizQuestion;
import com.example.aistudyassistant.models.Summary;
import com.example.aistudyassistant.receivers.ConnectivityReceiver;
import com.example.aistudyassistant.repositories.DocumentRepository;
import com.example.aistudyassistant.utils.DocumentTextExtractor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Điều phối việc đọc tài liệu và sinh nội dung AI trên background thread.
 */
public class AIProcessingService {

    private static final int MAX_FILE_BYTES = 25 * 1024 * 1024;
    private static final int MAX_DOCUMENT_CHARS = 160_000;
    private static final int MAX_CHAT_CONTEXT_CHARS = 80_000;
    private static final int MAX_CHAT_CONTEXT_DOCUMENTS = 5;
    private static final int MAX_CHAT_HISTORY_MESSAGES = 10;
    private static final int MAX_CHAT_HISTORY_CHARS = 12_000;
    private static final int DEFAULT_QUIZ_COUNT = 10;
    private static final int DEFAULT_FLASHCARD_COUNT = 15;
    private static final int MAX_PENDING_REQUESTS = 10;

    private static AIProcessingService instance;

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final DocumentRepository documentRepository;
    private final AIClient aiClient;
    private final ConnectivityReceiver connectivityReceiver;
    private final Object pendingLock = new Object();
    private final Deque<PendingOperation<?>> pendingOperations = new ArrayDeque<>();

    private AIProcessingService(Context context) {
        PDFBoxResourceLoader.init(context.getApplicationContext());
        documentRepository = DocumentRepository.getInstance();
        aiClient = AIClient.getInstance();
        connectivityReceiver = ConnectivityReceiver.getInstance(context);
        connectivityReceiver.addListener(this::retryPendingOperations);
    }

    public static synchronized AIProcessingService getInstance(Context context) {
        if (instance == null) {
            instance = new AIProcessingService(context);
        }
        return instance;
    }

    public void analyzeDocument(Document document, ApiCallback<DocumentAnalysis> callback) {
        submit(() -> analyzeBlocking(document), callback);
    }

    public void createSummary(Document document, ApiCallback<Summary> callback) {
        submit(() -> {
            DocumentAnalysis analysis = analyzeBlocking(document);
            return createSummaryBlocking(document, analysis);
        }, callback);
    }

    public void generateQuiz(Document document, int questionCount,
                             ApiCallback<List<QuizQuestion>> callback) {
        generateQuiz(document, questionCount, "MEDIUM", callback);
    }

    public void generateQuiz(Document document, int questionCount, String difficulty,
                             ApiCallback<List<QuizQuestion>> callback) {
        submit(() -> {
            validateCount(questionCount, 1, 30, "Số câu hỏi");
            DocumentAnalysis analysis = analyzeBlocking(document);
            return generateQuizBlocking(document, analysis, questionCount, difficulty);
        }, callback);
    }

    public void generateFlashcards(Document document, int cardCount,
                                   ApiCallback<List<Flashcard>> callback) {
        submit(() -> {
            validateCount(cardCount, 1, 50, "Số flashcard");
            DocumentAnalysis analysis = analyzeBlocking(document);
            return generateFlashcardsBlocking(document, analysis, cardCount);
        }, callback);
    }

    public void answerStudyQuestion(String question, ApiCallback<String> callback) {
        submit(() -> {
            if (question == null || question.trim().isEmpty()) {
                throw new IllegalArgumentException("Câu hỏi không được để trống");
            }

            // Gọi AI ở background thread để không chặn giao diện.
            String response = aiClient.generalChat(question.trim());
            if (response == null || response.trim().isEmpty()) {
                throw new IllegalStateException("Dịch vụ AI chưa trả về nội dung");
            }
            return response.trim();
        }, callback);
    }

    public void prepareLearningContext(Document currentDocument, ChatContextScope scope,
                                       ApiCallback<LearningContext> callback) {
        submit(() -> buildLearningContext(currentDocument, scope), callback);
    }

    public void answerWithLearningContext(LearningContext context,
                                          List<ChatMessage> conversationHistory,
                                          String question,
                                          ApiCallback<String> callback) {
        submit(() -> {
            if (context == null || context.getContent() == null
                    || context.getContent().trim().isEmpty()) {
                throw new IllegalArgumentException("Ngữ cảnh học tập chưa sẵn sàng");
            }
            if (question == null || question.trim().isEmpty()) {
                throw new IllegalArgumentException("Câu hỏi không được để trống");
            }

            String history = formatConversationHistory(conversationHistory);
            String response = aiClient.chatWithLearningContext(
                    context.getScope().getDisplayName(),
                    context.getContent(),
                    history,
                    question.trim()
            );
            if (response == null || response.trim().isEmpty()) {
                throw new IllegalStateException("Dịch vụ AI chưa trả về nội dung");
            }
            return response.trim();
        }, callback);
    }

    /**
     * Đọc file một lần rồi tạo đủ summary, quiz và flashcard.
     */
    public void processDocument(Document document, ApiCallback<AIProcessingResult> callback) {
        submit(() -> {
            DocumentAnalysis analysis = analyzeBlocking(document);
            Summary summary = createSummaryBlocking(document, analysis);
            List<QuizQuestion> quiz = generateQuizBlocking(
                    document, analysis, DEFAULT_QUIZ_COUNT, "MEDIUM");
            List<Flashcard> flashcards = generateFlashcardsBlocking(
                    document, analysis, DEFAULT_FLASHCARD_COUNT);
            return new AIProcessingResult(analysis, summary, quiz, flashcards);
        }, callback);
    }

    private DocumentAnalysis analyzeBlocking(Document document) throws Exception {
        validateDocument(document);
        byte[] fileBytes = documentRepository.downloadDocumentBytes(document);
        if (fileBytes == null) {
            throw new IllegalStateException("Không thể tải tài liệu từ Storage");
        }
        if (fileBytes.length > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Tài liệu vượt quá giới hạn 25 MB");
        }

        String fileType = resolveFileType(document);
        String extracted = normalizeText(DocumentTextExtractor.extract(fileBytes, fileType));
        if (extracted.isEmpty()) {
            throw new IllegalArgumentException(
                    "Không tìm thấy văn bản; PDF dạng ảnh cần OCR ở bước phát triển sau");
        }

        boolean truncated = extracted.length() > MAX_DOCUMENT_CHARS;
        String textForAi = truncated
                ? extracted.substring(0, MAX_DOCUMENT_CHARS)
                : extracted;
        int wordCount = textForAi.split("\\s+").length;
        return new DocumentAnalysis(
                document.getId(), fileType, textForAi,
                textForAi.length(), wordCount, truncated);
    }

    private LearningContext buildLearningContext(Document currentDocument,
                                                  ChatContextScope scope) throws Exception {
        if (currentDocument == null || scope == null) {
            throw new IllegalArgumentException("Thiếu thông tin ngữ cảnh học tập");
        }

        List<Document> documents = resolveContextDocuments(currentDocument, scope);
        moveCurrentDocumentFirst(documents, currentDocument.getId());

        StringBuilder context = new StringBuilder();
        int sourceCount = 0;
        Exception firstError = null;
        for (Document document : documents) {
            if (sourceCount >= MAX_CHAT_CONTEXT_DOCUMENTS
                    || context.length() >= MAX_CHAT_CONTEXT_CHARS) {
                break;
            }

            try {
                DocumentAnalysis analysis = analyzeBlocking(document);
                if (appendDocumentContext(context, document, analysis.getText())) {
                    sourceCount++;
                }
            } catch (Exception error) {
                if (scope == ChatContextScope.DOCUMENT) throw error;
                if (firstError == null) firstError = error;
            }
        }

        if (sourceCount == 0) {
            if (firstError != null) throw firstError;
            throw new IllegalStateException("Không có tài liệu phù hợp trong phạm vi đã chọn");
        }
        return new LearningContext(scope, context.toString(), sourceCount);
    }

    private List<Document> resolveContextDocuments(Document currentDocument,
                                                    ChatContextScope scope) {
        if (scope == ChatContextScope.DOCUMENT) {
            return new ArrayList<>(Collections.singletonList(currentDocument));
        }
        if (scope == ChatContextScope.TOPIC) {
            if (currentDocument.getTopicId() == null
                    || currentDocument.getTopicId().trim().isEmpty()) {
                throw new IllegalArgumentException("Tài liệu chưa thuộc chủ đề nào");
            }
            return new ArrayList<>(documentRepository.getDocumentsByTopicBlocking(
                    currentDocument.getTopicId()));
        }
        if (currentDocument.getProjectId() == null
                || currentDocument.getProjectId().trim().isEmpty()) {
            throw new IllegalArgumentException("Tài liệu chưa thuộc dự án nào");
        }
        return new ArrayList<>(documentRepository.getDocumentsByProjectBlocking(
                currentDocument.getProjectId()));
    }

    private void moveCurrentDocumentFirst(List<Document> documents, String documentId) {
        if (documentId == null) return;
        for (int index = 0; index < documents.size(); index++) {
            if (documentId.equals(documents.get(index).getId())) {
                Document current = documents.remove(index);
                documents.add(0, current);
                return;
            }
        }
    }

    private boolean appendDocumentContext(StringBuilder context, Document document,
                                          String documentText) {
        String documentName = document.getName() == null ? "Không tên" : document.getName();
        String header = "\n\n--- TÀI LIỆU: " + documentName + " ---\n";
        int remaining = MAX_CHAT_CONTEXT_CHARS - context.length();
        if (remaining <= header.length()) return false;
        context.append(header);

        int textLimit = Math.min(documentText.length(),
                MAX_CHAT_CONTEXT_CHARS - context.length());
        context.append(documentText, 0, textLimit);
        return textLimit > 0;
    }

    private String formatConversationHistory(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) return "Không có hội thoại trước đó.";

        int start = Math.max(0, history.size() - MAX_CHAT_HISTORY_MESSAGES);
        StringBuilder formatted = new StringBuilder();
        for (int index = start; index < history.size(); index++) {
            ChatMessage message = history.get(index);
            if (message == null || message.getContent() == null) continue;

            String line = (message.isUserMessage() ? "HỌC VIÊN: " : "TRỢ LÝ: ")
                    + message.getContent().trim() + "\n";
            int remaining = MAX_CHAT_HISTORY_CHARS - formatted.length();
            if (remaining <= 0) break;
            formatted.append(line, 0, Math.min(line.length(), remaining));
        }
        return formatted.length() == 0
                ? "Không có hội thoại trước đó." : formatted.toString();
    }

    private Summary createSummaryBlocking(Document document,
                                          DocumentAnalysis analysis) {
        String rawResponse = aiClient.generateSummary(analysis.getText());
        JsonObject json = parseObject(rawResponse, "tóm tắt");

        Summary summary = new Summary();
        summary.setUserId(document.getUserId());
        summary.setDocumentId(document.getId());
        summary.setSummaryText(requireString(json, "summary"));
        summary.setKeyPoints(readStringList(json, "keyPoints"));
        summary.setKeywords(readStringList(json, "keywords"));
        summary.setConclusion(optionalString(json, "conclusion"));
        return summary;
    }

    private List<QuizQuestion> generateQuizBlocking(Document document,
                                                     DocumentAnalysis analysis,
                                                     int questionCount,
                                                     String difficulty) {
        String normalizedDifficulty = difficulty == null
                ? "MEDIUM"
                : difficulty.trim().toUpperCase(Locale.US);
        String rawResponse = aiClient.generateQuiz(
                analysis.getText(), questionCount, normalizedDifficulty);
        JsonArray array = parseArray(rawResponse, "câu hỏi");
        List<QuizQuestion> questions = new ArrayList<>();

        for (JsonElement element : array) {
            JsonObject json = element.getAsJsonObject();
            String correctAnswer = requireString(json, "correctAnswer")
                    .trim().toUpperCase(Locale.US);
            if (!correctAnswer.matches("[ABCD]")) {
                throw new IllegalStateException("AI trả về đáp án trắc nghiệm không hợp lệ");
            }

            QuizQuestion question = new QuizQuestion(
                    requireString(json, "question"),
                    requireString(json, "optionA"),
                    requireString(json, "optionB"),
                    requireString(json, "optionC"),
                    requireString(json, "optionD"),
                    correctAnswer,
                    optionalString(json, "explanation")
            );
            question.setDocumentId(document.getId());
            question.setDifficulty(normalizedDifficulty);
            question.setOrderIndex(questions.size());
            questions.add(question);
        }

        if (questions.isEmpty()) {
            throw new IllegalStateException("AI không tạo được câu hỏi nào");
        }
        return questions;
    }

    private List<Flashcard> generateFlashcardsBlocking(Document document,
                                                        DocumentAnalysis analysis,
                                                        int cardCount) {
        String rawResponse = aiClient.generateFlashcards(analysis.getText(), cardCount);
        JsonArray array = parseArray(rawResponse, "flashcard");
        List<Flashcard> flashcards = new ArrayList<>();

        for (JsonElement element : array) {
            JsonObject json = element.getAsJsonObject();
            Flashcard flashcard = new Flashcard(
                    requireString(json, "front"),
                    requireString(json, "back")
            );
            flashcard.setUserId(document.getUserId());
            flashcard.setDocumentId(document.getId());
            flashcard.setTopicId(document.getTopicId());
            flashcards.add(flashcard);
        }

        if (flashcards.isEmpty()) {
            throw new IllegalStateException("AI không tạo được flashcard nào");
        }
        return flashcards;
    }

    private <T> void submit(BackgroundOperation<T> operation, ApiCallback<T> callback) {
        if (callback == null) return;
        PendingOperation<T> request = new PendingOperation<>(operation, callback);
        if (!connectivityReceiver.isConnected()) {
            enqueueForRetry(request);
            return;
        }
        executeOperation(request);
    }

    private <T> void executeOperation(PendingOperation<T> request) {
        executor.execute(() -> {
            try {
                T result = request.operation.run();
                mainHandler.post(() -> request.callback.onSuccess(result));
            } catch (Exception error) {
                if (error instanceof NetworkRequestException) {
                    // Emulator có thể vẫn báo VALIDATED dù DNS/Internet đã mất.
                    connectivityReceiver.reportTransportFailure();
                }
                if (!request.retryAttempted && !connectivityReceiver.isConnected()) {
                    enqueueForRetry(request);
                    return;
                }
                String message = error.getMessage();
                if (message == null || message.trim().isEmpty()) {
                    message = "Không thể xử lý tài liệu";
                }
                String finalMessage = message;
                mainHandler.post(() -> request.callback.onError(finalMessage));
            }
        });
    }

    private void enqueueForRetry(PendingOperation<?> request) {
        boolean queued;
        synchronized (pendingLock) {
            queued = pendingOperations.size() < MAX_PENDING_REQUESTS;
            if (queued) pendingOperations.addLast(request);
        }

        if (queued) {
            mainHandler.post(request.callback::onWaitingForNetwork);
        } else {
            mainHandler.post(() -> request.callback.onError(
                    "Hàng đợi AI đã đầy, vui lòng thử lại sau"));
        }
    }

    private void retryPendingOperations() {
        List<PendingOperation<?>> requests = new ArrayList<>();
        synchronized (pendingLock) {
            while (!pendingOperations.isEmpty()) {
                requests.add(pendingOperations.removeFirst());
            }
        }

        // Mỗi request chỉ được gửi lại một lần sau khi mạng trở lại.
        for (PendingOperation<?> request : requests) {
            request.retryAttempted = true;
            executeUnchecked(request);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void executeUnchecked(PendingOperation<?> request) {
        executeOperation((PendingOperation<T>) request);
    }

    private void validateDocument(Document document) {
        if (document == null || document.getFilePath() == null
                || document.getFilePath().trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu đường dẫn tài liệu");
        }
    }

    private void validateCount(int count, int min, int max, String fieldName) {
        if (count < min || count > max) {
            throw new IllegalArgumentException(
                    fieldName + " phải nằm trong khoảng " + min + "-" + max);
        }
    }

    private String resolveFileType(Document document) {
        if (document.getFileType() != null && !document.getFileType().trim().isEmpty()) {
            return document.getFileType();
        }
        String path = document.getFilePath();
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : "";
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f ]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private JsonObject parseObject(String raw, String resultName) {
        JsonElement element = parseJson(raw, '{', '}', resultName);
        if (!element.isJsonObject()) {
            throw new IllegalStateException("AI trả về " + resultName + " sai định dạng");
        }
        return element.getAsJsonObject();
    }

    private JsonArray parseArray(String raw, String resultName) {
        JsonElement element = parseJson(raw, '[', ']', resultName);
        if (!element.isJsonArray()) {
            throw new IllegalStateException("AI trả về " + resultName + " sai định dạng");
        }
        return element.getAsJsonArray();
    }

    private JsonElement parseJson(String raw, char opening, char closing,
                                  String resultName) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalStateException("Không nhận được " + resultName + " từ AI");
        }
        String cleaned = raw.trim();
        int start = cleaned.indexOf(opening);
        int end = cleaned.lastIndexOf(closing);
        if (start < 0 || end < start) {
            throw new IllegalStateException("AI trả về " + resultName + " sai định dạng");
        }
        try {
            return JsonParser.parseString(cleaned.substring(start, end + 1));
        } catch (Exception error) {
            throw new IllegalStateException("Không thể đọc " + resultName + " do AI trả về");
        }
    }

    private String requireString(JsonObject json, String key) {
        String value = optionalString(json, key);
        if (value.isEmpty()) {
            throw new IllegalStateException("Phản hồi AI thiếu trường " + key);
        }
        return value;
    }

    private String optionalString(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) return "";
        return json.get(key).getAsString().trim();
    }

    private List<String> readStringList(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray(key)) {
            if (element.isJsonPrimitive()) {
                String value = element.getAsString().trim();
                if (!value.isEmpty()) values.add(value);
            }
        }
        return values;
    }

    private interface BackgroundOperation<T> {
        T run() throws Exception;
    }

    private static class PendingOperation<T> {
        private final BackgroundOperation<T> operation;
        private final ApiCallback<T> callback;
        private boolean retryAttempted;

        private PendingOperation(BackgroundOperation<T> operation, ApiCallback<T> callback) {
            this.operation = operation;
            this.callback = callback;
        }
    }
}
