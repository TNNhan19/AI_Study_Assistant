package com.example.aistudyassistant.models;

public class Schedule {
    private int id;
    private String userId;
    private String title;
    private String description;
    private long dateTimeMillis;   // Combined date + time in milliseconds
    private boolean reminderEnabled;
    private long createdAt;

    public Schedule() {}

    public Schedule(String title, String description, long dateTimeMillis, boolean reminderEnabled) {
        this.title = title;
        this.description = description;
        this.dateTimeMillis = dateTimeMillis;
        this.reminderEnabled = reminderEnabled;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters
    public int getId() { return id; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getDateTimeMillis() { return dateTimeMillis; }
    public boolean isReminderEnabled() { return reminderEnabled; }
    public long getCreatedAt() { return createdAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDateTimeMillis(long dateTimeMillis) { this.dateTimeMillis = dateTimeMillis; }
    public void setReminderEnabled(boolean reminderEnabled) { this.reminderEnabled = reminderEnabled; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    /** Check if schedule is in the future */
    public boolean isUpcoming() {
        return dateTimeMillis > System.currentTimeMillis();
    }
}
