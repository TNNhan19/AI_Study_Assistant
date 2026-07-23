import "@supabase/functions-js/edge-runtime.d.ts";
import { withSupabase } from "@supabase/server";

const OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses";
const DEFAULT_MODEL = "gpt-5.6-luna";
const MAX_PROMPT_LENGTH = 200_000;
const MAX_ATTEMPTS = 3;
const RETRYABLE_STATUSES = new Set([500, 502, 503, 504]);

// Chỉ cho phép các tác vụ AI của ứng dụng.
const ALLOWED_TASKS = new Set(["chat", "summary", "flashcards", "quiz"]);

type AIRequest = {
  task?: unknown;
  prompt?: unknown;
};

type OpenAIResponse = {
  output?: Array<{
    type?: string;
    content?: Array<{
      type?: string;
      text?: string;
    }>;
  }>;
  usage?: Record<string, unknown>;
  error?: {
    code?: string;
    message?: string;
    type?: string;
  };
};

type ProviderResult = {
  response: Response;
  body: OpenAIResponse;
};

function jsonResponse(body: unknown, status = 200): Response {
  // Khai báo UTF-8 để client hiển thị đúng tiếng Việt.
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });
}

function errorResponse(
  status: number,
  code: string,
  message: string,
  details?: Record<string, unknown>,
): Response {
  return jsonResponse({ error: { code, message, ...details } }, status);
}

function readGeneratedText(response: OpenAIResponse): string {
  // Ghép các output_text trong phản hồi OpenAI.
  return response.output
    ?.flatMap((item) => item.content ?? [])
    .filter((part) => part.type === "output_text" && typeof part.text === "string")
    .map((part) => part.text ?? "")
    .join("")
    .trim() ?? "";
}

function delay(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

async function callOpenAI(
  apiKey: string,
  model: string,
  prompt: string,
): Promise<ProviderResult> {
  let lastError: unknown;

  for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
    try {
      const response = await fetch(OPENAI_RESPONSES_URL, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${apiKey}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          model,
          input: prompt,
          max_output_tokens: 8192,
        }),
      });

      const body = await response.json() as OpenAIResponse;
      if (response.ok || !RETRYABLE_STATUSES.has(response.status)
          || attempt === MAX_ATTEMPTS) {
        return { response, body };
      }

      // Retry ngắn cho lỗi tạm thời từ nhà cung cấp.
      console.warn(`OpenAI temporary error ${response.status}, retry ${attempt}.`);
      await delay(750 * Math.pow(2, attempt - 1));
    } catch (error) {
      lastError = error;
      if (attempt === MAX_ATTEMPTS) break;
      console.warn(`OpenAI network error, retry ${attempt}.`);
      await delay(750 * Math.pow(2, attempt - 1));
    }
  }

  throw lastError ?? new Error("OpenAI request failed.");
}

export default {
  fetch: withSupabase({ auth: "user" }, async (req) => {
    if (req.method !== "POST") {
      return errorResponse(405, "METHOD_NOT_ALLOWED", "Only POST is allowed.");
    }

    const apiKey = Deno.env.get("OPENAI_API_KEY");
    const model = Deno.env.get("OPENAI_MODEL") ?? DEFAULT_MODEL;

    // API key chỉ được đọc từ Supabase Secret.
    if (!apiKey) {
      console.error("OPENAI_API_KEY is not configured.");
      return errorResponse(500, "SERVER_MISCONFIGURED", "AI service is not configured.");
    }

    let body: AIRequest;
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

    if (body.prompt.length > MAX_PROMPT_LENGTH) {
      return errorResponse(413, "PROMPT_TOO_LARGE", "Prompt is too large.");
    }

    let providerResult: ProviderResult;
    try {
      providerResult = await callOpenAI(apiKey, model, body.prompt.trim());
    } catch (error) {
      console.error("OpenAI network error:", error);
      return errorResponse(503, "AI_PROVIDER_UNREACHABLE", "AI service is unavailable.");
    }

    const { response, body: responseBody } = providerResult;
    if (!response.ok) {
      console.error(`OpenAI API error ${response.status}:`, responseBody.error);
      const status = response.status === 429
        ? 429
        : response.status >= 500 ? 503 : 502;
      return errorResponse(
        status,
        "AI_PROVIDER_ERROR",
        "AI service could not process the request.",
        {
          providerStatus: response.status,
          providerCode: responseBody.error?.code ?? responseBody.error?.type ?? null,
        },
      );
    }

    const text = readGeneratedText(responseBody);
    if (!text) {
      console.error("OpenAI response does not contain output_text.");
      return errorResponse(502, "EMPTY_AI_RESPONSE", "AI service returned no content.");
    }

    return jsonResponse({
      data: {
        task: body.task,
        text,
        provider: "openai",
        model,
        usage: responseBody.usage ?? null,
      },
    });
  }),
};
