package com.example.aistudyassistant.models;

public class User {
    private String id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private long createdAt;

    public User() {}

    public User(String id, String email, String fullName) {
        // id này trùng với auth.users.id để liên kết profile app với Supabase Auth.
        this.id = id;
        this.email = email;
        this.fullName = fullName;
    }

    public User(String id, String email, String fullName, String avatarUrl, long createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
    }

    // Getters
    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public long getCreatedAt() { return createdAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
