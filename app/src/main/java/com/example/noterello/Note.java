package com.example.noterello;

import java.util.ArrayList;
import java.util.List;

public class Note {
    private String id;
    private String title;
    private String content; // Dla typu "plain"
    private String color;
    private boolean fullWidth;
    private boolean pinned;
    private double timestamp;

    private long deadline;
    private boolean completed;

    // NOWE POLA
    private String type; // "plain", "checklist", "image"
    private List<ChecklistItem> checklist;

    // POLE NA ZDJĘCIE (Base64)
    private String imageUrl;

    public Note() {
        this.checklist = new ArrayList<>();
        this.imageUrl = "";
    }

    public Note(String id, String title, String content, String color, boolean fullWidth, boolean pinned, double timestamp, String type) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.color = color;
        this.fullWidth = fullWidth;
        this.pinned = pinned;
        this.timestamp = timestamp;
        this.type = type;
        this.checklist = new ArrayList<>();
        this.imageUrl = "";
    }

    // Gettery i Settery
    public long getDeadline() { return deadline; }
    public void setDeadline(long deadline) { this.deadline = deadline; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isFullWidth() { return fullWidth; }
    public void setFullWidth(boolean fullWidth) { this.fullWidth = fullWidth; }

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }

    public double getTimestamp() { return timestamp; }
    public void setTimestamp(double timestamp) { this.timestamp = timestamp; }

    public String getType() { return type != null ? type : "plain"; }
    public void setType(String type) { this.type = type; }

    public List<ChecklistItem> getChecklist() { return checklist; }
    public void setChecklist(List<ChecklistItem> checklist) { this.checklist = checklist; }

    // GETTER I SETTER DLA OBRAZKA
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}