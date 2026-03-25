package com.example.noterello;

public class Board {
    private String id;
    private String name;
    private String color;
    private int noteCount;

    public Board(String id, String name, String color, int noteCount) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.noteCount = noteCount;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public int getNoteCount() { return noteCount; }

    public void setName(String name) { this.name = name; }
    public void setColor(String color) { this.color = color; }
    public void setNoteCount(int noteCount) { this.noteCount = noteCount; }
}