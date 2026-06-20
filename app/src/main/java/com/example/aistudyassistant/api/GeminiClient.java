package com.example.aistudyassistant.api;

import android.util.Log;

import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * GeminiClient handles all communication with the Google Gemini API.
 *
 * USAGE:
 * 1. Replace Constants.GEMINI_API_KEY with your actual API key.
 * 2. Call methods on a background thread (they are blocking).
 *
 * Flow for document processing:
 *   - Upload PDF to Supabase Storage first
 *   - Pass file URL to the generation methods
 *   - Gemini reads the document and generates content
 */
public class GeminiClient {

    private static final String TAG = "GeminiClient";
    private static GeminiClient instance;

    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;

    private GeminiClient() {
        this.apiKey = Constants.GEMINI_API_KEY;
        this.baseUrl = Constants.GEMINI_BASE_URL;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized GeminiClient getInstance() {
        if (instance == null) {
            instance = new GeminiClient();
        }
        return instance;
    }

    // ======================== Core Request Method ========================

    /**
     * Sends a text prompt to Gemini and returns the response text.
     * Call this on a background thread.
     *
     * @param prompt The text prompt to send
     * @return The AI response as a String, or null on error
     */
    public String generateText(String prompt) {
        try {
            String url = baseUrl + "models/" + Constants.GEMINI_MODEL
                    + ":generateContent?key=" + apiKey;

            JsonObject requestBody = buildTextRequest(prompt);

            RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Gemini API error: " + response.code()
                            + " " + response.message());
                    return null;
                }
                String responseBody = response.body() != null ? response.body().string() : null;
                return parseGeminiResponse(responseBody);
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error calling Gemini: " + e.getMessage());
            return null;
        }
    }

    // ======================== AI Feature Methods ========================

    /**
     * Generates a summary for a document.
     *
     * @param documentText The text content of the document
     * @return JSON string with summary, keyPoints, keywords, conclusion
     */
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

        return generateText(prompt);
    }

    /**
     * Generates quiz questions from document text.
     *
     * @param documentText The text content of the document
     * @param questionCount Number of questions to generate
     * @return JSON array of quiz questions
     */
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

        return generateText(prompt);
    }

    /**
     * Generates flashcards from document text.
     *
     * @param documentText The text content of the document
     * @param cardCount    Number of flashcards to generate
     * @return JSON array of flashcards
     */
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

        return generateText(prompt);
    }

    /**
     * Answers a question based on document context.
     *
     * @param documentText The text content of the document
     * @param userQuestion The user's question
     * @return AI answer as a String
     */
    public String chatWithDocument(String documentText, String userQuestion) {
        String prompt = "You are a helpful study assistant. Answer the following question "
                + "based ONLY on the provided document content. "
                + "If the answer is not in the document, say so.\n\n"
                + "DOCUMENT CONTENT:\n" + documentText + "\n\n"
                + "USER QUESTION: " + userQuestion + "\n\n"
                + "Provide a clear, helpful answer in a conversational tone.";

        return generateText(prompt);
    }

    /**
     * General AI chat (no document context).
     *
     * @param userQuestion The user's question
     * @return AI answer as a String
     */
    public String generalChat(String userQuestion) {
        String prompt = "You are a helpful study assistant for students. "
                + "Answer the following question clearly and helpfully.\n\n"
                + "QUESTION: " + userQuestion;

        return generateText(prompt);
    }

    // ======================== Private Helpers ========================

    private JsonObject buildTextRequest(String prompt) {
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(textPart);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject requestBody = new JsonObject();
        requestBody.add("contents", contents);

        // Generation config for quality output
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        generationConfig.addProperty("topK", 40);
        generationConfig.addProperty("topP", 0.95);
        generationConfig.addProperty("maxOutputTokens", 8192);
        requestBody.add("generationConfig", generationConfig);

        return requestBody;
    }

    private String parseGeminiResponse(String responseJson) {
        if (responseJson == null || responseJson.isEmpty()) return null;
        try {
            JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) return null;

            JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
            JsonObject content = firstCandidate.getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");
            if (parts == null || parts.size() == 0) return null;

            return parts.get(0).getAsJsonObject().get("text").getAsString();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Gemini response: " + e.getMessage());
            return null;
        }
    }
}
