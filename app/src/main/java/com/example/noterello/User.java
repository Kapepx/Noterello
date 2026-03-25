package com.example.noterello;

public class User {
    private String id;
    private String username;
    private String email;
    private String avatarColor;
    private String role; // "Admin", "Editor", "Viewer"

    public User(String id, String username, String email, String avatarColor, String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.avatarColor = avatarColor;
        this.role = role;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getAvatarColor() { return avatarColor; }
    public String getRole() { return role; }

    public void setRole(String role) { this.role = role; }
    
    public String getInitials() {
        if (username == null || username.isEmpty()) return "??";
        String[] parts = username.split(" ");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return username.substring(0, Math.min(2, username.length())).toUpperCase();
    }
}