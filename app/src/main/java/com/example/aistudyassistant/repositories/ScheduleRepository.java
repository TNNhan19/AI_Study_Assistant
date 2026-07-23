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

    public void getUpcomingSchedules(String userId, ApiCallback<List<Schedule>> callback) {
        new Thread(() -> {
            try {
                String query = "user_id=eq." + userId
                        + "&is_completed=eq.false"
                        + "&order=reminder_at.asc";
                String response = supabaseClient.getFromTable(Constants.TABLE_SCHEDULES, query);
                if (response == null) {
                    callback.onError("Failed to fetch schedules");
                    return;
                }

                JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
                List<Schedule> schedules = new ArrayList<>();
                long now = System.currentTimeMillis();
                for (JsonElement element : jsonArray) {
                    Schedule schedule = mapReminder(element.getAsJsonObject());
                    if (schedule.getDateTimeMillis() >= now) {
                        schedules.add(schedule);
                    }
                }
                callback.onSuccess(schedules);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void getTodaySchedules(String userId, ApiCallback<List<Schedule>> callback) {
        new Thread(() -> {
            try {
                Calendar startOfToday = Calendar.getInstance();
                startOfToday.set(Calendar.HOUR_OF_DAY, 0);
                startOfToday.set(Calendar.MINUTE, 0);
                startOfToday.set(Calendar.SECOND, 0);
                startOfToday.set(Calendar.MILLISECOND, 0);

                Calendar startOfTomorrow = (Calendar) startOfToday.clone();
                startOfTomorrow.add(Calendar.DAY_OF_MONTH, 1);

                String query = "user_id=eq." + userId
                        + "&is_completed=eq.false"
                        + "&reminder_at=gte." + formatTimestamp(startOfToday.getTimeInMillis())
                        + "&reminder_at=lt." + formatTimestamp(startOfTomorrow.getTimeInMillis())
                        + "&order=reminder_at.asc"
                        + "&limit=3";
                String response = supabaseClient.getFromTable(Constants.TABLE_SCHEDULES, query);
                if (response == null) {
                    callback.onError("Failed to fetch today's schedules");
                    return;
                }

                JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
                List<Schedule> schedules = new ArrayList<>();
                for (JsonElement element : jsonArray) {
                    schedules.add(mapReminder(element.getAsJsonObject()));
                    if (schedules.size() >= 3) break;
                }
                callback.onSuccess(schedules);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void createSchedule(Schedule schedule, ApiCallback<Schedule> callback) {
        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("user_id", schedule.getUserId());
                json.addProperty("title", schedule.getTitle());
                json.addProperty("description", schedule.getDescription());
                json.addProperty("reminder_at", formatTimestamp(schedule.getDateTimeMillis()));
                json.addProperty("is_completed", false);

                String response = supabaseClient.insertIntoTable(Constants.TABLE_SCHEDULES, json.toString());
                if (response == null) {
                    callback.onError("Failed to save schedule");
                    return;
                }

                JsonArray resultArray = JsonParser.parseString(response).getAsJsonArray();
                if (resultArray.size() == 0) {
                    callback.onError("Failed to read saved schedule");
                    return;
                }

                Schedule savedSchedule = mapReminder(resultArray.get(0).getAsJsonObject());
                savedSchedule.setReminderEnabled(schedule.isReminderEnabled());
                callback.onSuccess(savedSchedule);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void deleteSchedule(String scheduleId, ApiCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                String response = supabaseClient.deleteFromTable(Constants.TABLE_SCHEDULES, scheduleId);
                if ("success".equals(response)) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("Failed to delete schedule");
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private Schedule mapReminder(JsonObject obj) {
        Schedule schedule = new Schedule();
        schedule.setId(getString(obj, "id"));
        schedule.setUserId(getString(obj, "user_id"));
        schedule.setTitle(getString(obj, "title"));
        schedule.setDescription(getString(obj, "description"));
        schedule.setDateTimeMillis(parseTimestamp(getString(obj, "reminder_at")));
        schedule.setReminderEnabled(true);
        schedule.setCreatedAt(parseTimestamp(getString(obj, "created_at")));
        return schedule;
    }

    private String getString(JsonObject obj, String column) {
        if (!obj.has(column) || obj.get(column).isJsonNull()) return "";
        return obj.get(column).getAsString();
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
}
