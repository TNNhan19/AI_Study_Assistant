package com.example.aistudyassistant.repositories;

import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class DocumentRepository {

    private static DocumentRepository instance;
    private final SupabaseClient supabaseClient;
    private final Gson gson;

    private DocumentRepository() {
        this.supabaseClient = SupabaseClient.getInstance();
        this.gson = new Gson();
    }

    public static synchronized DocumentRepository getInstance() {
        if (instance == null) {
            instance = new DocumentRepository();
        }
        return instance;
    }

    public void getAllDocuments(String userId, ApiCallback<List<Document>> callback) {
        new Thread(() -> {
            try {
                // Query: filter by user_id, order by created_at descending
                String query = "user_id=eq." + userId + "&order=created_at.desc";
                String response = supabaseClient.getFromTable(Constants.TABLE_DOCUMENTS, query);

                if (response == null) {
                    callback.onError("Failed to fetch documents");
                    return;
                }

                JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
                List<Document> documents = new ArrayList<>();
                for (JsonElement element : jsonArray) {
                    // Manual mapping because model field names might differ slightly from DB columns
                    JsonObject obj = element.getAsJsonObject();
                    Document doc = new Document();
                    doc.setId(obj.get("id").getAsString());
                    doc.setUserId(obj.get("user_id").getAsString());
                    doc.setName(obj.get("name").getAsString());
                    
                    String filePath = obj.get("file_path").getAsString();
                    doc.setFilePath(filePath);
                    
                    // Tạo Public URL thật sự từ Supabase Storage
                    String publicUrl = Constants.SUPABASE_URL + "/storage/v1/object/public/" + Constants.STORAGE_BUCKET + "/" + filePath;
                    doc.setFileUrl(publicUrl);

                    doc.setFileType(obj.has("file_type") && !obj.get("file_type").isJsonNull() ? obj.get("file_type").getAsString() : "pdf");
                    doc.setFileSize(obj.has("file_size") && !obj.get("file_size").isJsonNull() ? obj.get("file_size").getAsLong() : 0);
                    doc.setStatus(obj.has("status") && !obj.get("status").isJsonNull() ? obj.get("status").getAsString() : "UPLOADED");
                    doc.setFavorite(obj.has("is_favorite") && !obj.get("is_favorite").isJsonNull() && obj.get("is_favorite").getAsBoolean());
                    
                    if (obj.has("project_id") && !obj.get("project_id").isJsonNull()) 
                        doc.setProjectId(obj.get("project_id").getAsString());
                    if (obj.has("topic_id") && !obj.get("topic_id").isJsonNull()) 
                        doc.setTopicId(obj.get("topic_id").getAsString());

                    documents.add(doc);
                }
                callback.onSuccess(documents);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void uploadDocument(Document document, byte[] fileBytes, ApiCallback<Document> callback) {
        new Thread(() -> {
            try {
                // 1. Upload file to Storage
                String contentType = "application/pdf";
                if ("txt".equalsIgnoreCase(document.getFileType())) contentType = "text/plain";
                else if ("docx".equalsIgnoreCase(document.getFileType())) contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

                String uploadResult = supabaseClient.uploadFile(
                        Constants.STORAGE_BUCKET,
                        document.getFilePath(),
                        fileBytes,
                        contentType
                );

                if (uploadResult == null) {
                    callback.onError("Failed to upload file to storage");
                    return;
                }

                // 2. Insert record into database
                JsonObject json = new JsonObject();
                json.addProperty("user_id", document.getUserId());
                json.addProperty("name", document.getName());
                json.addProperty("file_path", document.getFilePath());
                json.addProperty("file_type", document.getFileType());
                json.addProperty("file_size", document.getFileSize());
                json.addProperty("status", Constants.STATUS_UPLOADED);
                if (document.getProjectId() != null) json.addProperty("project_id", document.getProjectId());
                if (document.getTopicId() != null) json.addProperty("topic_id", document.getTopicId());

                String dbResponse = supabaseClient.insertIntoTable(Constants.TABLE_DOCUMENTS, json.toString());
                if (dbResponse == null) {
                    callback.onError("Failed to save document metadata");
                    return;
                }

                // Parse response to get the inserted document (including generated ID)
                JsonArray resultArray = JsonParser.parseString(dbResponse).getAsJsonArray();
                if (resultArray.size() > 0) {
                    JsonObject insertedObj = resultArray.get(0).getAsJsonObject();
                    document.setId(insertedObj.get("id").getAsString());
                    callback.onSuccess(document);
                } else {
                    callback.onError("Failed to retrieve saved document info");
                }

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void deleteDocument(Document document, ApiCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                // 1. Delete record from DB
                String response = supabaseClient.deleteFromTable(Constants.TABLE_DOCUMENTS, document.getId());
                
                // Note: We should also delete from Storage, but Supabase Client deleteFromTable is generic.
                // In a real app, you might want a separate Storage delete.
                
                if ("success".equals(response)) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("Failed to delete document: " + response);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void toggleFavorite(String documentId, boolean isFavorite, ApiCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("is_favorite", isFavorite);
                
                String response = supabaseClient.updateInTable(Constants.TABLE_DOCUMENTS, documentId, json.toString());
                if (response != null && !response.contains("error")) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("Update favorite failed: " + response);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
}
