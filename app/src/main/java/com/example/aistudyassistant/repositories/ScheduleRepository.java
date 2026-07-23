package com.example.aistudyassistant.repositories;

import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.api.SupabaseClient;
import com.example.aistudyassistant.models.Schedule;
import com.example.aistudyassistant.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ScheduleRepository {

    private static ScheduleRepository instance;
    private final SupabaseClient supabaseClient;

    private ScheduleRepository() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public static synchronized ScheduleRepository getInstance() {
        if (instance == null) {
            instance = new ScheduleRepository();
        }
        return instance;
    }

    public void createSchedule(Schedule schedule, ApiCallback<Schedule> callback) {
        new Thread(() -> {
            try {
                validateSchedule(schedule, true);
                JsonObject json = buildScheduleJson(schedule, true);
                String response = supabaseClient.insertIntoTable(
                        Constants.TABLE_SCHEDULES,
                        json.toString()
                );
                callback.onSuccess(parseOne(response, "Could not read saved schedule"));
            } catch (Exception error) {
                callback.onError(readError(error, "Could not save schedule"));
            }
        }).start();
    }

    public void getSchedulesByUser(String userId, ApiCallback<List<Schedule>> callback) {
        new Thread(() -> {
            try {
                if (isBlank(userId)) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }

                String query = "user_id=eq." + userId + "&order=reminder_at.asc";
                String response = supabaseClient.getFromTable(Constants.TABLE_SCHEDULES, query);
                callback.onSuccess(parseList(response));
            } catch (Exception error) {
                callback.onError(readError(error, "Could not load schedules"));
            }
        }).start();
    }

    public void getUpcomingSchedules(String userId, ApiCallback<List<Schedule>> callback) {
        getSchedulesByUser(userId, new ApiCallback<List<Schedule>>() {
            @Override
            public void onSuccess(List<Schedule> result) {
                long now = System.currentTimeMillis();
                List<Schedule> upcoming = new ArrayList<>();
                for (Schedule schedule : result) {
                    if (!schedule.isCompleted() && schedule.getDateTimeMillis() >= now) {
                        upcoming.add(schedule);
                    }
                }
                callback.onSuccess(upcoming);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void getTodaySchedules(String userId, ApiCallback<List<Schedule>> callback) {
        new Thread(() -> {
            try {
                if (isBlank(userId)) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }

                Calendar startOfToday = Calendar.getInstance();
                startOfToday.set(Calendar.HOUR_OF_DAY, 0);
                startOfToday.set(Calendar.MINUTE, 0);
                startOfToday.set(Calendar.SECOND, 0);
                startOfToday.set(Calendar.MILLISECOND, 0);

                Calendar startOfTomorrow = (Calendar) startOfToday.clone();
                startOfTomorrow.add(Calendar.DAY_OF_MONTH, 1);

                String query = "user_id=eq." + userId
                        + "&reminder_at=gte." + formatTimestamp(startOfToday.getTimeInMillis())
                        + "&reminder_at=lt." + formatTimestamp(startOfTomorrow.getTimeInMillis())
                        + "&is_completed=eq.false"
                        + "&order=reminder_at.asc";
                String response = supabaseClient.getFromTable(Constants.TABLE_SCHEDULES, query);
                callback.onSuccess(parseList(response));
            } catch (Exception error) {
                callback.onError(readError(error, "Could not load today's schedules"));
            }
        }).start();
    }

    public void getScheduleById(String scheduleId, ApiCallback<Schedule> callback) {
        new Thread(() -> {
            try {
                if (isBlank(scheduleId)) {
                    callback.onError("Schedule id is required");
                    return;
                }
                String response = supabaseClient.getFromTable(
                        Constants.TABLE_SCHEDULES,
                        "id=eq." + scheduleId + "&limit=1"
                );
                JsonArray rows = parseRows(response);
                callback.onSuccess(rows.size() == 0 ? null : mapSchedule(rows.get(0).getAsJsonObject()));
            } catch (Exception error) {
                callback.onError(readError(error, "Could not load schedule"));
            }
        }).start();
    }

    public void updateSchedule(Schedule schedule, ApiCallback<Schedule> callback) {
        new Thread(() -> {
            try {
                validateSchedule(schedule, false);
                if (isBlank(schedule.getId())) {
                    throw new IllegalArgumentException("Schedule id is required");
                }

                JsonObject json = buildScheduleJson(schedule, false);
                String response = supabaseClient.updateInTable(
                        Constants.TABLE_SCHEDULES,
                        schedule.getId(),
                        json.toString()
                );
                callback.onSuccess(parseOne(response, "Could not read updated schedule"));
            } catch (Exception error) {
                callback.onError(readError(error, "Could not update schedule"));
            }
        }).start();
    }

    public void deleteSchedule(String scheduleId, ApiCallback<Void> callback) {
        new Thread(() -> {
            try {
                if (isBlank(scheduleId)) {
                    callback.onError("Schedule id is required");
                    return;
                }

                String response = supabaseClient.deleteFromTable(Constants.TABLE_SCHEDULES, scheduleId);
                if ("success".equals(response)) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(readSupabaseMessage(response, "Could not delete schedule"));
                }
            } catch (Exception error) {
                callback.onError(readError(error, "Could not delete schedule"));
            }
        }).start();
    }

    public void markScheduleCompleted(String scheduleId, boolean completed,
                                      ApiCallback<Schedule> callback) {
        new Thread(() -> {
            try {
                if (isBlank(scheduleId)) {
                    callback.onError("Schedule id is required");
                    return;
                }

                JsonObject json = new JsonObject();
                json.addProperty("is_completed", completed);
                String response = supabaseClient.updateInTable(
                        Constants.TABLE_SCHEDULES,
                        scheduleId,
                        json.toString()
                );
                callback.onSuccess(parseOne(response, "Could not read updated schedule"));
            } catch (Exception error) {
                callback.onError(readError(error, "Could not update schedule status"));
            }
        }).start();
    }

    private JsonObject buildScheduleJson(Schedule schedule, boolean includeUserId) {
        JsonObject json = new JsonObject();
        if (includeUserId) {
            json.addProperty("user_id", schedule.getUserId());
        }
        json.addProperty("title", schedule.getTitle());
        json.addProperty("description", schedule.getDescription());
        json.addProperty("reminder_at", formatTimestamp(schedule.getDateTimeMillis()));
        json.addProperty("is_completed", schedule.isCompleted());
        return json;
    }

    private Schedule parseOne(String response, String emptyMessage) {
        JsonArray rows = parseRows(response);
        if (rows.size() == 0) {
            throw new IllegalStateException(emptyMessage);
        }
        return mapSchedule(rows.get(0).getAsJsonObject());
    }

    private List<Schedule> parseList(String response) {
        JsonArray rows = parseRows(response);
        List<Schedule> schedules = new ArrayList<>();
        for (JsonElement row : rows) {
            if (row != null && row.isJsonObject()) {
                schedules.add(mapSchedule(row.getAsJsonObject()));
            }
        }
        return schedules;
    }

    private JsonArray parseRows(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new IllegalStateException("Empty Supabase response");
        }

        JsonElement root = JsonParser.parseString(response);
        if (root.isJsonArray()) return root.getAsJsonArray();
        if (root.isJsonObject()) {
            throw new IllegalStateException(readSupabaseMessage(response, "Invalid Supabase response"));
        }
        throw new IllegalStateException("Invalid Supabase response");
    }

    private Schedule mapSchedule(JsonObject obj) {
        Schedule schedule = new Schedule();
        schedule.setId(getString(obj, "id"));
        schedule.setUserId(getString(obj, "user_id"));
        schedule.setTitle(getString(obj, "title"));
        schedule.setDescription(getString(obj, "description"));
        schedule.setDateTimeMillis(parseTimestamp(getString(obj, "reminder_at")));
        schedule.setCompleted(getBoolean(obj, "is_completed"));
        schedule.setReminderEnabled(true);
        schedule.setCreatedAt(parseTimestamp(getString(obj, "created_at")));
        return schedule;
    }

    private void validateSchedule(Schedule schedule, boolean requireUserId) {
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule is required");
        }
        if (requireUserId && isBlank(schedule.getUserId())) {
            throw new IllegalArgumentException("User id is required");
        }
        if (isBlank(schedule.getTitle())) {
            throw new IllegalArgumentException("Title is required");
        }
        if (schedule.getDateTimeMillis() <= 0) {
            throw new IllegalArgumentException("Reminder time is required");
        }
    }

    private String getString(JsonObject obj, String column) {
        if (!obj.has(column) || obj.get(column).isJsonNull()) return "";
        return obj.get(column).getAsString();
    }

    private boolean getBoolean(JsonObject obj, String column) {
        return obj.has(column) && !obj.get(column).isJsonNull() && obj.get(column).getAsBoolean();
    }

    private String formatTimestamp(long millis) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(millis);
    }

    private long parseTimestamp(String value) {
        if (value == null || value.trim().isEmpty()) {
            return System.currentTimeMillis();
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
        return System.currentTimeMillis();
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

    private String readSupabaseMessage(String response, String fallback) {
        if (response == null || response.trim().isEmpty()) return fallback;
        try {
            JsonElement root = JsonParser.parseString(response);
            if (root.isJsonObject()) {
                JsonObject object = root.getAsJsonObject();
                String message = getString(object, "message");
                if (!isBlank(message)) return message;
                String error = getString(object, "error");
                if (!isBlank(error)) return error;
            }
        } catch (Exception ignored) {
            // Return raw response below if it is useful.
        }
        return response.startsWith("error:") ? response : fallback;
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
