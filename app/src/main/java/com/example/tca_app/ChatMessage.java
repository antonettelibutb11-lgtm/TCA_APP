package com.example.tca_app;

public class ChatMessage {
    private String senderId;
    private String senderName;
    private String text;
    private String imageUrl;
    private String messageType;
    private long timestamp;

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
