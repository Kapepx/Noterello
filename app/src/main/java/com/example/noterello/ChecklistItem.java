package com.example.noterello;

public class ChecklistItem {
    private String text;
    private boolean checked;

    public ChecklistItem() { } // Wymagany dla Firebase

    public ChecklistItem(String text, boolean checked) {
        this.text = text;
        this.checked = checked;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }
}