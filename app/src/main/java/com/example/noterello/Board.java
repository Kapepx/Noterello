package com.example.noterello;

public class Board {
    private String id;
    private String name;
    private String color;
    private int noteCount;
    private long timestamp; // NOWA ZMIENNA

    private String ownerId; // nowe pole do przechowywania id właściciela tablicy


    // Wymagany przez Firebase pusty konstruktor
    public Board() {
    }

    public Board(String id, String name, String color, int noteCount, long timestamp, String ownerId) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.noteCount = noteCount;
        this.timestamp = timestamp;
        this.ownerId = ownerId; // Zaktualizowany konstruktor
    }

    // Gettery i Settery
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }


    public int getNoteCount() {
        return noteCount;
    }

    public void setNoteCount(int noteCount) {
        this.noteCount = noteCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
}