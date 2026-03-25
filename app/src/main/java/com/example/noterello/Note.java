package com.example.noterello;

public class Note {
    private String id;
    private String title;
    private String content;
    private String color; // Np. "#FFF9C4"
    private boolean isFullWidth; // true = prostokąt (cała szerokość), false = kwadrat (połowa)
    private boolean isPinned;

    public Note(String id, String title, String content, String color, boolean isFullWidth, boolean isPinned) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.color = color;
        this.isFullWidth = isFullWidth;
        this.isPinned = isPinned;
    }

    // Metody pomocnicze
    public void toggleFullWidth() {
        this.isFullWidth = !this.isFullWidth;
    }

    public void togglePinned() {
        this.isPinned = !this.isPinned;
    }

    // Gettery i Settery
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isFullWidth() { return isFullWidth; }
    public void setFullWidth(boolean fullWidth) { isFullWidth = fullWidth; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
}