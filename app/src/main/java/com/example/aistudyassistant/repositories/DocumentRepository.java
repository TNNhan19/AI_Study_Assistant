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
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

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
                String query = "user_id=eq." + userId + "&order=created_at.desc";
                String response = supabaseClient.getFromTable(Constants.TABLE_DOCUMENTS, query);

                if (response == null) {
                    callback.onError("Failed to fetch documents");
                    return;
                }

                JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
                List<Document> documents = new ArrayList<>();
                for (JsonElement element : jsonArray) {
                    documents.add(mapDocument(element.getAsJsonObject()));
                }
                callback.onSuccess(documents);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void getDocumentById(String documentId, ApiCallback<Document> callback) {
        new Thread(() -> {
            try {
                List<Document> documents = getDocumentsBlocking(
                        "id=eq." + documentId + "&limit=1");
                if (documents.isEmpty()) {
                    callback.onError("Không tìm thấy tài liệu");
                    return;
                }
                callback.onSuccess(documents.get(0));
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public List<Document> getDocumentsByTopicBlocking(String topicId) {
        return getDocumentsBlocking(
                "topic_id=eq." + topicId + "&order=created_at.desc");
    }

    public List<Document> getDocumentsByProjectBlocking(String projectId) {
        return getDocumentsBlocking(
                "project_id=eq." + projectId + "&order=created_at.desc");
    }

    private List<Document> getDocumentsBlocking(String query) {
        String response = supabaseClient.getFromTable(Constants.TABLE_DOCUMENTS, query);
        if (response == null) {
            throw new IllegalStateException("Không thể tải danh sách tài liệu");
        }

        JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
        List<Document> documents = new ArrayList<>();
        for (JsonElement element : jsonArray) {
            documents.add(mapDocument(element.getAsJsonObject()));
        }
        return documents;
    }

    /**
     * Downloads a document from the private Storage bucket without changing its bytes,
     * then stores it in the app cache so it can be shared through FileProvider.
     */
    public void downloadDocument(Document document, File cacheDir, ApiCallback<File> callback) {
        new Thread(() -> {
            try {
                byte[] fileBytes = downloadDocumentBytes(document);
                if (fileBytes == null) {
                    callback.onError("Could not download document");
                    return;
                }

                File documentCache = new File(cacheDir, "documents");
                if (!documentCache.exists() && !documentCache.mkdirs()) {
                    callback.onError("Could not create document cache");
                    return;
                }

                String extension = sanitizeExtension(document.getFileType());
                String fileId = document.getId() != null
                        ? document.getId().replaceAll("[^a-zA-Z0-9_-]", "_")
                        : String.valueOf(document.getFilePath().hashCode());
                File cachedFile = new File(documentCache,
                        fileId + (extension.isEmpty() ? "" : "." + extension));

                try (FileOutputStream output = new FileOutputStream(cachedFile)) {
                    output.write(fileBytes);
                }
                callback.onSuccess(cachedFile);
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Could not cache document");
            }
        }).start();
    }

    /**
     * Tải byte gốc của tài liệu. Hàm blocking nên chỉ gọi từ background thread.
     */
    public byte[] downloadDocumentBytes(Document document) {
        if (document == null || document.getFilePath() == null
                || document.getFilePath().trim().isEmpty()) {
            return null;
        }
        return supabaseClient.downloadFile(
                Constants.STORAGE_BUCKET,
                document.getFilePath()
        );
    }

    private String sanitizeExtension(String fileType) {
        if (fileType == null) return "";
        return fileType.toLowerCase().replaceAll("[^a-z0-9]", "");
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
                    callback.onSuccess(mapDocument(insertedObj));
                } else {
                    callback.onError("Failed to retrieve saved document info");
                }

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private Document mapDocument(JsonObject obj) {
        Document document = new Document();
        document.setId(obj.get("id").getAsString());
        document.setUserId(obj.get("user_id").getAsString());
        document.setName(obj.get("name").getAsString());
        document.setFilePath(obj.get("file_path").getAsString());
        document.setFileType(getOptionalString(obj, "file_type", "pdf"));
        document.setFileSize(obj.has("file_size") && !obj.get("file_size").isJsonNull()
                ? obj.get("file_size").getAsLong() : 0);
        document.setStatus(getOptionalString(obj, "status", Constants.STATUS_UPLOADED));
        document.setFavorite(obj.has("is_favorite") && !obj.get("is_favorite").isJsonNull()
                && obj.get("is_favorite").getAsBoolean());

        if (obj.has("project_id") && !obj.get("project_id").isJsonNull()) {
            document.setProjectId(obj.get("project_id").getAsString());
        }
        if (obj.has("topic_id") && !obj.get("topic_id").isJsonNull()) {
            document.setTopicId(obj.get("topic_id").getAsString());
        }

        long createdAt = parseSupabaseTimestamp(obj, "created_at");
        document.setCreatedAt(createdAt);
        document.setUpdatedAt(parseSupabaseTimestamp(obj, "updated_at", createdAt));
        return document;
    }

    private String getOptionalString(JsonObject obj, String field, String defaultValue) {
        return obj.has(field) && !obj.get(field).isJsonNull()
                ? obj.get(field).getAsString() : defaultValue;
    }

    private long parseSupabaseTimestamp(JsonObject obj, String field) {
        return parseSupabaseTimestamp(obj, field, System.currentTimeMillis());
    }

    private long parseSupabaseTimestamp(JsonObject obj, String field, long fallback) {
        if (!obj.has(field) || obj.get(field).isJsonNull()) {
            return fallback;
        }

        String value = obj.get(field).getAsString();
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        value = normalizeTimestampFraction(value.trim());
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                return format.parse(value).getTime();
            } catch (Exception ignored) {
                // Try the next supported Supabase timestamp shape.
            }
        }
        return fallback;
    }

    private String normalizeTimestampFraction(String value) {
        int dotIndex = value.indexOf('.');
        if (dotIndex < 0) return value;

        int fractionStart = dotIndex + 1;
        int fractionEnd = fractionStart;
        while (fractionEnd < value.length() && Character.isDigit(value.charAt(fractionEnd))) {
            fractionEnd++;
        }

        String fraction = value.substring(fractionStart, fractionEnd);
        if (fraction.length() > 3) {
            fraction = fraction.substring(0, 3);
        } else {
            while (fraction.length() < 3) {
                fraction += "0";
            }
        }

        return value.substring(0, fractionStart) + fraction + value.substring(fractionEnd);
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
