package com.example.aistudyassistant.models;

public class SearchResult {
    public enum Type { DOCUMENT, PROJECT, TOPIC, FLASHCARD, QUIZ, NOTE }
    
    private String id;
    private String title;
    private String subtitle;
    private Type type;
    private Object originalObject;

    public SearchResult(String id, String title, String subtitle, Type type, Object originalObject) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.type = type;
        this.originalObject = originalObject;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public Type getType() { return type; }
    public Object getOriginalObject() { return originalObject; }

    public String getIcon() {
        switch (type) {
            case DOCUMENT: return "📄";
            case PROJECT: return "📁";
            case TOPIC: return "🔖";
            case FLASHCARD: return "🃏";
            case QUIZ: return "📝";
            case NOTE: return "📓";
            default: return "🔍";
        }
    }

    public String getTypeName() {
        return type.name().substring(0, 1) + type.name().substring(1).toLowerCase();
    }
}
