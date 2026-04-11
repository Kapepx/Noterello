package com.example.noterello;

public class Invitation {
    private String id;
    private String boardId;
    private String boardName;
    private String fromUserId;
    private String fromUserEmail;
    private String toUserEmail;
    private String status;
    private String role; // NOWE POLE: "Admin", "Editor", "Viewer"
    private long timestamp;

    public Invitation() { }

    public Invitation(String id, String boardId, String boardName, String fromUserId, String fromUserEmail, String toUserEmail, String status, String role, long timestamp) {
        this.id = id;
        this.boardId = boardId;
        this.boardName = boardName;
        this.fromUserId = fromUserId;
        this.fromUserEmail = fromUserEmail;
        this.toUserEmail = toUserEmail;
        this.status = status;
        this.role = role;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBoardId() { return boardId; }
    public void setBoardId(String boardId) { this.boardId = boardId; }

    public String getBoardName() { return boardName; }
    public void setBoardName(String boardName) { this.boardName = boardName; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getFromUserEmail() { return fromUserEmail; }
    public void setFromUserEmail(String fromUserEmail) { this.fromUserEmail = fromUserEmail; }

    public String getToUserEmail() { return toUserEmail; }
    public void setToUserEmail(String toUserEmail) { this.toUserEmail = toUserEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}