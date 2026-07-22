import "@supabase/functions-js/edge-runtime.d.ts";
import { withSupabase } from "@supabase/server";

const GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";
const DEFAULT_MODEL = "gemini-3.5-flash";
const MAX_PROMPT_LENGTH = 200_000;

// Chỉ cho phép các tác vụ AI của ứng dụng.
const ALLOWED_TASKS = new Set(["chat", "summary", "flashcards", "quiz"]);

type GeminiRequest = {
  task?: unknown;
  prompt?: unknown;
};

type GeminiResponse = {
  candidates?: Array<{
    content?: {
      parts?: Array<{ text?: string }>;
    };
  }>;
  usageMetadata?: Record<string, unknown>;
};

function jsonResponse(body: unknown, status = 200): Response {
  // Khai báo UTF-8 để client hiển thị đúng tiếng Việt.
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });
}

function errorResponse(status: number, code: string, message: string): Response {
  return jsonResponse({ error: { code, message } }, status);
}

function readGeneratedText(response: GeminiResponse): string {
  // Ghép toàn bộ phần văn bản Gemini trả về.
  return response.candidates?.[0]?.content?.parts
    ?.map((part) => part.text ?? "")
    .join("")
    .trim() ?? "";
}

export default {
  fetch: withSupabase({ auth: "user" }, async (req) => {
    if (req.method !== "POST") {
      return errorResponse(405, "METHOD_NOT_ALLOWED", "Only POST is allowed.");
    }

    const apiKey = Deno.env.get("GEMINI_API_KEY");
    const model = Deno.env.get("GEMINI_MODEL") ?? DEFAULT_MODEL;

    // API key chỉ được đọc từ Supabase Secret.
    if (!apiKey) {
      console.error("GEMINI_API_KEY is not configured.");
      return errorResponse(500, "SERVER_MISCONFIGURED", "AI service is not configured.");
    }

    let body: GeminiRequest;
    try {
      body = await req.json();
    } catch {
      return errorResponse(400, "INVALID_JSON", "Request body must be valid JSON.");
    }

    if (typeof body.task !== "string" || !ALLOWED_TASKS.has(body.task)) {
      return errorResponse(400, "INVALID_TASK", "Unsupported AI task.");
    }

    if (typeof body.prompt !== "string" || body.prompt.trim().length === 0) {
      return errorResponse(400, "INVALID_PROMPT", "Prompt must not be empty.");
    }

    // Giới hạn kích thước để tránh request quá lớn.
    if (body.prompt.length > MAX_PROMPT_LENGTH) {
      return errorResponse(413, "PROMPT_TOO_LARGE", "Prompt is too large.");
    }

    let geminiResponse: Response;
    try {
      geminiResponse = await fetch(
        `${GEMINI_BASE_URL}/${encodeURIComponent(model)}:generateContent`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "x-goog-api-key": apiKey,
          },
          body: JSON.stringify({
            contents: [{ role: "user", parts: [{ text: body.prompt.trim() }] }],
          }),
        },
      );
    } catch (error) {
      console.error("Gemini network error:", error);
      return errorResponse(502, "GEMINI_UNREACHABLE", "AI service is unavailable.");
    }

    let responseBody: GeminiResponse & { error?: unknown };
    try {
      responseBody = await geminiResponse.json();
    } catch {
      console.error("Gemini returned a non-JSON response.");
      return errorResponse(502, "INVALID_GEMINI_RESPONSE", "AI service returned an invalid response.");
    }

    if (!geminiResponse.ok) {
      console.error(`Gemini API error ${geminiResponse.status}:`, responseBody.error);
      const status = geminiResponse.status === 429 ? 429 : 502;
      return errorResponse(status, "GEMINI_API_ERROR", "AI service could not process the request.");
    }

    const text = readGeneratedText(responseBody);
    if (!text) {
      console.error("Gemini response does not contain generated text.");
      return errorResponse(502, "EMPTY_GEMINI_RESPONSE", "AI service returned no content.");
    }

    return jsonResponse({
      data: {
        task: body.task,
        text,
        model,
        usage: responseBody.usageMetadata ?? null,
      },
    });
  }),
};
