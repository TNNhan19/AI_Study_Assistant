package com.example.aistudyassistant.repositories;

import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.Note;
import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class NoteRepository {
    private static NoteRepository instance;
    private final SupabaseClient supabaseClient;

    private NoteRepository() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public static synchronized NoteRepository getInstance() {
        if (instance == null) {
            instance = new NoteRepository();
        }
        return instance;
    }

    public void getAllNotes(String userId, ApiCallback<List<Note>> callback) {
        new Thread(() -> {
            try {
                String query = "user_id=eq." + userId + "&order=is_pinned.desc,created_at.desc";
                String response = supabaseClient.getFromTable(Constants.TABLE_NOTES, query);

                if (response == null) {
                    callback.onError("Failed to fetch notes");
                    return;
                }

                callback.onSuccess(parseNotes(response));
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void getNotesByDocument(String documentId, ApiCallback<List<Note>> callback) {
        new Thread(() -> {
            try {
                String query = "document_id=eq." + documentId + "&order=created_at.desc";
                String response = supabaseClient.getFromTable(Constants.TABLE_NOTES, query);
                if (response == null) {
                    callback.onError("Failed to fetch notes for this document");
                    return;
                }
                callback.onSuccess(parseNotes(response));
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void createNote(Note note, ApiCallback<Note> callback) {
        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("user_id", note.getUserId());
                if (note.getDocumentId() != null) json.addProperty("document_id", note.getDocumentId());
                if (note.getTopicId() != null) json.addProperty("topic_id", note.getTopicId());
                json.addProperty("title", note.getTitle());
                json.addProperty("content", note.getContent());
                json.addProperty("is_pinned", note.isPinned());

                String response = supabaseClient.insertIntoTable(Constants.TABLE_NOTES, json.toString());
                if (response != null) {
                    JsonArray resultArray = JsonParser.parseString(response).getAsJsonArray();
                    if (resultArray.size() > 0) {
                        callback.onSuccess(parseNote(resultArray.get(0).getAsJsonObject()));
                    } else {
                        callback.onError("Failed to create note");
                    }
                } else {
                    callback.onError("Create note failed");
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void updateNote(Note note, ApiCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("title", note.getTitle());
                json.addProperty("content", note.getContent());
                json.addProperty("is_pinned", note.isPinned());
                json.addProperty("updated_at", "now()");

                String response = supabaseClient.updateInTable(Constants.TABLE_NOTES, note.getId(), json.toString());
                if (response != null && !response.contains("error")) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("Update note failed: " + response);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void deleteNote(String noteId, ApiCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                String response = supabaseClient.deleteFromTable(Constants.TABLE_NOTES, noteId);
                if ("success".equals(response)) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("Delete note failed: " + response);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private List<Note> parseNotes(String response) {
        JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
        List<Note> notes = new ArrayList<>();
        for (JsonElement element : jsonArray) {
            notes.add(parseNote(element.getAsJsonObject()));
        }
        return notes;
    }

    private Note parseNote(JsonObject obj) {
        Note note = new Note();
        note.setId(obj.get("id").getAsString());
        note.setUserId(obj.get("user_id").getAsString());
        if (obj.has("document_id") && !obj.get("document_id").isJsonNull())
            note.setDocumentId(obj.get("document_id").getAsString());
        if (obj.has("topic_id") && !obj.get("topic_id").isJsonNull())
            note.setTopicId(obj.get("topic_id").getAsString());
        note.setTitle(obj.get("title").getAsString());
        note.setContent(obj.has("content") && !obj.get("content").isJsonNull() ? obj.get("content").getAsString() : "");
        note.setPinned(obj.has("is_pinned") && !obj.get("is_pinned").isJsonNull() && obj.get("is_pinned").getAsBoolean());
        note.setCreatedAt(obj.get("created_at").getAsString());
        if (obj.has("updated_at") && !obj.get("updated_at").isJsonNull())
            note.setUpdatedAt(obj.get("updated_at").getAsString());
        return note;
    }
}
