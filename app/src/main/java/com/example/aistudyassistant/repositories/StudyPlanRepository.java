package com.example.aistudyassistant.repositories;

import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.StudyPlan;
import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class StudyPlanRepository {

    private static StudyPlanRepository instance;
    private final SupabaseClient supabaseClient;

    private StudyPlanRepository() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public static synchronized StudyPlanRepository getInstance() {
        if (instance == null) {
            instance = new StudyPlanRepository();
        }
        return instance;
    }

    public void saveStudyPlan(StudyPlan plan, ApiCallback<StudyPlan> callback) {
        new Thread(() -> {
            try {
                validatePlan(plan);

                JsonObject json = new JsonObject();
                json.addProperty("user_id", plan.getUserId());
                addOptionalString(json, "project_id", plan.getProjectId());
                json.addProperty("title", plan.getTitle());
                addOptionalString(json, "exam_date", plan.getExamDate());
                json.add("plan_data", JsonParser.parseString(plan.getPlanData()));

                String response = supabaseClient.insertIntoTable(
                        Constants.TABLE_STUDY_PLANS,
                        json.toString()
                );
                if (response == null) {
                    callback.onError("Could not save study plan");
                    return;
                }

                JsonArray rows = parseRows(response);
                if (rows.size() == 0) {
                    callback.onError("Could not read saved study plan");
                    return;
                }
                callback.onSuccess(parseStudyPlan(rows.get(0).getAsJsonObject()));
            } catch (Exception error) {
                callback.onError(readError(error, "Could not save study plan"));
            }
        }).start();
    }

    public void getStudyPlansByUser(String userId, ApiCallback<List<StudyPlan>> callback) {
        new Thread(() -> {
            try {
                if (isBlank(userId)) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }

                String query = "user_id=eq." + userId + "&order=created_at.desc";
                String response = supabaseClient.getFromTable(Constants.TABLE_STUDY_PLANS, query);
                if (response == null) {
                    callback.onError("Could not load study plans");
                    return;
                }

                JsonArray rows = parseRows(response);
                List<StudyPlan> plans = new ArrayList<>();
                for (JsonElement row : rows) {
                    plans.add(parseStudyPlan(row.getAsJsonObject()));
                }
                callback.onSuccess(plans);
            } catch (Exception error) {
                callback.onError(readError(error, "Could not load study plans"));
            }
        }).start();
    }

    private StudyPlan parseStudyPlan(JsonObject json) {
        StudyPlan plan = new StudyPlan();
        plan.setId(readString(json, "id"));
        plan.setUserId(readString(json, "user_id"));
        plan.setProjectId(readString(json, "project_id"));
        plan.setTitle(readString(json, "title"));
        plan.setExamDate(readString(json, "exam_date"));
        plan.setCreatedAt(readString(json, "created_at"));
        if (json.has("plan_data") && !json.get("plan_data").isJsonNull()) {
            plan.setPlanData(json.get("plan_data").toString());
        } else {
            plan.setPlanData("{}");
        }
        return plan;
    }

    private JsonArray parseRows(String response) {
        JsonElement root = JsonParser.parseString(response);
        if (root.isJsonArray()) return root.getAsJsonArray();
        if (root.isJsonObject()) {
            JsonObject error = root.getAsJsonObject();
            String message = readString(error, "message");
            if (!isBlank(message)) throw new IllegalStateException(message);
        }
        throw new IllegalStateException("Invalid Supabase response");
    }

    private void validatePlan(StudyPlan plan) {
        if (plan == null || isBlank(plan.getUserId())
                || isBlank(plan.getTitle())
                || isBlank(plan.getPlanData())) {
            throw new IllegalArgumentException("Study plan is missing required data");
        }
        JsonElement planData = JsonParser.parseString(plan.getPlanData());
        if (!planData.isJsonObject()) {
            throw new IllegalArgumentException("Study plan data must be a JSON object");
        }
    }

    private void addOptionalString(JsonObject json, String key, String value) {
        if (!isBlank(value)) {
            json.addProperty(key, value);
        }
    }

    private String readString(JsonObject json, String key) {
        return !json.has(key) || json.get(key).isJsonNull()
                ? ""
                : json.get(key).getAsString();
    }

    private String readError(Exception error, String fallback) {
        return error.getMessage() == null || error.getMessage().trim().isEmpty()
                ? fallback
                : error.getMessage();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
