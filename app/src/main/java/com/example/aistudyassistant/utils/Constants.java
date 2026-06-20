package com.example.aistudyassistant.utils;

public class Constants {

    // ===================== Supabase Configuration =====================
    // TODO: Replace these with your actual Supabase credentials
    public static final String SUPABASE_URL = "YOUR_SUPABASE_URL";  // e.g. https://xxxx.supabase.co
    public static final String SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY";

    // Supabase Table Names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_DOCUMENTS = "documents";
    public static final String TABLE_SUMMARIES = "summaries";
    public static final String TABLE_QUIZ_QUESTIONS = "quiz_questions";
    public static final String TABLE_QUIZ_RESULTS = "quiz_results";
    public static final String TABLE_FLASHCARDS = "flashcards";
    public static final String TABLE_CHAT_MESSAGES = "chat_messages";
    public static final String TABLE_SCHEDULES = "schedules";

    // Supabase Storage Bucket
    public static final String STORAGE_BUCKET = "documents";

    // ===================== Gemini API Configuration =====================
    // TODO: Replace this with your actual Gemini API Key
    public static final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY";
    public static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/";
    public static final String GEMINI_MODEL = "gemini-1.5-flash";
    public static final String GEMINI_MODEL_PRO = "gemini-1.5-pro";

    // ===================== SharedPreferences Keys =====================
    public static final String PREF_NAME = "AIStudyAssistantPrefs";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_ACCESS_TOKEN = "access_token";
    public static final String PREF_REFRESH_TOKEN = "refresh_token";
    public static final String PREF_IS_LOGGED_IN = "is_logged_in";

    // ===================== Intent Extra Keys =====================
    public static final String EXTRA_DOCUMENT_ID = "document_id";
    public static final String EXTRA_DOCUMENT_NAME = "document_name";
    public static final String EXTRA_DOCUMENT_URL = "document_url";
    public static final String EXTRA_SCHEDULE_ID = "schedule_id";
    public static final String EXTRA_QUIZ_ID = "quiz_id";

    // ===================== Document Status =====================
    public static final String STATUS_UPLOADED = "UPLOADED";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    // ===================== Notification =====================
    public static final String NOTIFICATION_CHANNEL_ID = "study_reminder_channel";
    public static final String ALARM_SCHEDULE_ID = "schedule_id";
    public static final String ALARM_SCHEDULE_TITLE = "schedule_title";

    // ===================== Message Types =====================
    public static final int MSG_TYPE_USER = 0;
    public static final int MSG_TYPE_AI = 1;

    // ===================== Request Codes =====================
    public static final int REQUEST_CODE_FILE_PICKER = 1001;
    public static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 1002;

    private Constants() {
        // Prevent instantiation
    }
}
