package com.example.tca_app;

public class ModerationItem {
    private String id;
    private String postId;
    private String reason;
    private String aiScore;
    private String previewText;

    public ModerationItem(String reason, String aiScore, String previewText) {
        this.reason = reason;
        this.aiScore = aiScore;
        this.previewText = previewText;
    }

    public ModerationItem(String id, String postId, String reason, String aiScore, String previewText) {
        this.id = id;
        this.postId = postId;
        this.reason = reason;
        this.aiScore = aiScore;
        this.previewText = previewText;
    }

    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getPostId() { return postId != null ? postId : ""; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getReason() { return reason; }
    public String getAiScore() { return aiScore; }
    public String getPreviewText() { return previewText; }
}
