package com.example.aistudyassistant.models;

public class LearningContext {
    private final ChatContextScope scope;
    private final String content;
    private final int sourceCount;

    public LearningContext(ChatContextScope scope, String content, int sourceCount) {
        this.scope = scope;
        this.content = content;
        this.sourceCount = sourceCount;
    }

    public ChatContextScope getScope() {
        return scope;
    }

    public String getContent() {
        return content;
    }

    public int getSourceCount() {
        return sourceCount;
    }
}
