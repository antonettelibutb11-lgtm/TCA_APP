package com.example.tca_app;

public class ChatConversation {
    private String chatId;
    private String studentUid;
    private String studentName;
    private String studentEmail;
    private String adminUid;
    private String lastMessage;
    private long lastMessageTimestamp;
    private String lastSenderId;
    private String lastSenderName;
    private String lastSenderRole;
    private boolean unread;

    public ChatConversation() {}

    public ChatConversation(String chatId, String studentUid, String studentName, String studentEmail,
                            String adminUid, String lastMessage, long lastMessageTimestamp,
                            String lastSenderId, String lastSenderName, String lastSenderRole) {
        this.chatId = chatId;
        this.studentUid = studentUid;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.adminUid = adminUid;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.lastSenderId = lastSenderId;
        this.lastSenderName = lastSenderName;
        this.lastSenderRole = lastSenderRole;
        this.unread = !"ADMIN".equalsIgnoreCase(lastSenderRole);
    }

    public String getChatId() {
        return chatId != null ? chatId : "";
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getStudentUid() {
        return studentUid != null ? studentUid : "";
    }

    public void setStudentUid(String studentUid) {
        this.studentUid = studentUid;
    }

    public String getStudentName() {
        return (studentName != null && !studentName.trim().isEmpty()) ? studentName : "BISU Student";
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail != null ? studentEmail : "";
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getAdminUid() {
        return adminUid != null ? adminUid : "";
    }

    public void setAdminUid(String adminUid) {
        this.adminUid = adminUid;
    }

    public String getLastMessage() {
        return lastMessage != null ? lastMessage : "No messages yet";
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getLastSenderId() {
        return lastSenderId != null ? lastSenderId : "";
    }

    public void setLastSenderId(String lastSenderId) {
        this.lastSenderId = lastSenderId;
    }

    public String getLastSenderName() {
        return lastSenderName != null ? lastSenderName : "";
    }

    public void setLastSenderName(String lastSenderName) {
        this.lastSenderName = lastSenderName;
    }

    public String getLastSenderRole() {
        return lastSenderRole != null ? lastSenderRole : "STUDENT";
    }

    public void setLastSenderRole(String lastSenderRole) {
        this.lastSenderRole = lastSenderRole;
    }

    public boolean isUnread() {
        return unread;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }
}
