package com.example.aistudyassistant.models;

public enum ChatContextScope {
    DOCUMENT("Tài liệu"),
    TOPIC("Chủ đề"),
    PROJECT("Dự án");

    private final String displayName;

    ChatContextScope(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
