package com.example.tca_app;

public class ChatMessage {
    private String messageId = "";
    private String senderId;
    private String senderName;
    private String text;
    private String imageUrl;
    private String messageType;
    private long timestamp;
    private String senderRole = "STUDENT";
    private boolean isEdited = false;
    private boolean isUnsent = false;
    private long editedAt = 0;

    public ChatMessage() {}

    public ChatMessage(String senderId, String senderName, String text, long timestamp) {
        this(senderId, senderName, text, "", "TEXT", timestamp);
    }

    public ChatMessage(String senderId, String senderName, String text, String imageUrl, String messageType, long timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.imageUrl = imageUrl;
        this.messageType = messageType != null ? messageType : "TEXT";
        this.timestamp = timestamp;
    }

    public String getMessageId() { return messageId != null ? messageId : ""; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public boolean isEdited() { return isEdited; }
    public void setEdited(boolean edited) { isEdited = edited; }

    public boolean isUnsent() { return isUnsent; }
    public void setUnsent(boolean unsent) { isUnsent = unsent; }

    public long getEditedAt() { return editedAt; }
    public void setEditedAt(long editedAt) { this.editedAt = editedAt; }

    public String getSenderRole() { return senderRole != null ? senderRole : "STUDENT"; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getSenderId() { return senderId != null ? senderId : ""; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName != null ? senderName : ""; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getText() { return text != null ? text : ""; }
    public void setText(String text) { this.text = text; }

    public String getImageUrl() { return imageUrl != null ? imageUrl : ""; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getMessageType() { return messageType != null ? messageType : "TEXT"; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
