package com.example.tca_app;

public class Comment {
    private String id;
    private String author;
    private String text;
    private long timestamp;
    private String reactionType;
    private int reactionCount;

    public Comment() {}

    public Comment(String id, String author, String text, long timestamp) {
        this.id = id;
        this.author = author;
        this.text = text;
        this.timestamp = timestamp;
        this.reactionType = "";
        this.reactionCount = 0;
    }

    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getAuthor() { return author != null ? author : ""; }
    public void setAuthor(String author) { this.author = author; }

    public String getText() { return text != null ? text : ""; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getReactionType() { return reactionType != null ? reactionType : ""; }
    public void setReactionType(String reactionType) { this.reactionType = reactionType; }

    public int getReactionCount() { return reactionCount; }
    public void setReactionCount(int reactionCount) { this.reactionCount = reactionCount; }
}
