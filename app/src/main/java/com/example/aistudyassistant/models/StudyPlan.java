package com.example.aistudyassistant.models;

public class StudyPlan {
    private String id;
    private String userId;
    private String projectId;
    private String title;
    private String examDate;
    private String planData;
    private String createdAt;

    public StudyPlan() {}

    public StudyPlan(String userId, String title, String examDate) {
        this.userId = userId;
        this.title = title;
        this.examDate = examDate;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getProjectId() { return projectId; }
    public String getTitle() { return title; }
    public String getExamDate() { return examDate; }
    public String getPlanData() { return planData; }
    public String getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public void setTitle(String title) { this.title = title; }
    public void setExamDate(String examDate) { this.examDate = examDate; }
    public void setPlanData(String planData) { this.planData = planData; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
