package com.example.aistudyassistant.api;

import android.util.Log;

import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Gửi các tác vụ AI từ Android qua Supabase Edge Function.
 * Các hàm là blocking nên phải được gọi trên background thread.
 */
public class AIClient {

    private static final String TAG = "AIClient";
    private static final String TASK_CHAT = "chat";
    private static final String TASK_SUMMARY = "summary";
    private static final String TASK_FLASHCARDS = "flashcards";
    private static final String TASK_QUIZ = "quiz";

    private static AIClient instance;

    private AIClient() {
    }

    public static synchronized AIClient getInstance() {
        if (instance == null) {
            instance = new AIClient();
        }
        return instance;
    }

    /**
     * Gửi prompt chat thông thường qua Edge Function.
     */
    public String generateText(String prompt) {
        return generateText(TASK_CHAT, prompt);
    }

    public String generateSummary(String documentText) {
        String prompt = "You are an expert study assistant. Analyze the following document and create a comprehensive study summary.\n\n"
                + "Return your response in this exact JSON format:\n"
                + "{\n"
                + "  \"summary\": \"A concise 2-3 paragraph summary of the document\",\n"
                + "  \"keyPoints\": [\"Key point 1\", \"Key point 2\", \"Key point 3\", ...],\n"
                + "  \"keywords\": [\"keyword1\", \"keyword2\", \"keyword3\", ...],\n"
                + "  \"conclusion\": \"A brief conclusion about the main takeaways\"\n"
                + "}\n\n"
                + "DOCUMENT CONTENT:\n" + documentText;

        return generateText(TASK_SUMMARY, prompt);
    }

    public String generateQuiz(String documentText, int questionCount) {
        String prompt = "You are an expert educator. Create " + questionCount
                + " multiple-choice quiz questions based on the following document.\n\n"
                + "Return your response as a JSON array with this exact format:\n"
                + "[\n"
                + "  {\n"
                + "    \"question\": \"The question text\",\n"
                + "    \"optionA\": \"First option\",\n"
                + "    \"optionB\": \"Second option\",\n"
                + "    \"optionC\": \"Third option\",\n"
                + "    \"optionD\": \"Fourth option\",\n"
                + "    \"correctAnswer\": \"A\",\n"
                + "    \"explanation\": \"Why this answer is correct\"\n"
                + "  }\n"
                + "]\n\n"
                + "Make questions that test understanding, not just memorization.\n\n"
                + "DOCUMENT CONTENT:\n" + documentText;

        return generateText(TASK_QUIZ, prompt);
    }

    public String generateFlashcards(String documentText, int cardCount) {
        String prompt = "You are an expert educator. Create " + cardCount
                + " flashcards for studying the following document.\n\n"
                + "Return your response as a JSON array with this exact format:\n"
                + "[\n"
                + "  {\n"
                + "    \"front\": \"Question or term (keep it brief)\",\n"
                + "    \"back\": \"Answer or explanation (clear and concise)\"\n"
                + "  }\n"
                + "]\n\n"
                + "Focus on key concepts, definitions, and important facts.\n\n"
                + "DOCUMENT CONTENT:\n" + documentText;

        return generateText(TASK_FLASHCARDS, prompt);
    }

    public String chatWithDocument(String documentText, String userQuestion) {
        String prompt = "You are a helpful study assistant. Answer the following question "
                + "based ONLY on the provided document content. "
                + "If the answer is not in the document, say so.\n\n"
                + "DOCUMENT CONTENT:\n" + documentText + "\n\n"
                + "USER QUESTION: " + userQuestion + "\n\n"
                + "Provide a clear, helpful answer in a conversational tone.";

        return generateText(TASK_CHAT, prompt);
    }

    public String generalChat(String userQuestion) {
        String prompt = "You are an accurate and supportive AI learning assistant. "
                + "Answer in the same language as the student's question. "
                + "For concept explanations, define key terms, explain step by step, "
                + "and include a simple example when useful. "
                + "Keep the answer clear and focused. If uncertain, state the limitation "
                + "instead of inventing facts.\n\n"
                + "QUESTION: " + userQuestion;

        return generateText(TASK_CHAT, prompt);
    }

    private String generateText(String task, String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            Log.e(TAG, "AI prompt is empty.");
            return null;
        }

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("task", task);
        requestBody.addProperty("prompt", prompt.trim());

        // SupabaseClient tự gắn JWT, refresh và retry một lần khi cần.
        String responseJson = SupabaseClient.getInstance().invokeEdgeFunction(
                Constants.FUNCTION_AI_GATEWAY,
                requestBody.toString()
        );
        return parseGatewayResponse(responseJson);
    }

    private String parseGatewayResponse(String responseJson) {
        if (responseJson == null || responseJson.trim().isEmpty()) {
            return null;
        }

        try {
            JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
            if (root.has("error")) {
                JsonObject error = root.getAsJsonObject("error");
                Log.e(TAG, "AI gateway error: " + error);
                return null;
            }

            JsonObject data = root.getAsJsonObject("data");
            if (data == null || !data.has("text") || data.get("text").isJsonNull()) {
                Log.e(TAG, "AI gateway response has no text.");
                return null;
            }

            String text = data.get("text").getAsString().trim();
            return text.isEmpty() ? null : text;
        } catch (Exception e) {
            Log.e(TAG, "Cannot parse AI gateway response: " + e.getMessage());
            return null;
        }
    }
}
