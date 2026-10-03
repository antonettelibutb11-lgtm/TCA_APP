package com.example.tca_app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Comment {
    private String id;
    private String author;
    private String text;
    private long timestamp;
    private String reactionType;
    private int reactionCount;
    private Map<String, Long> reactionsSummary = new HashMap<>();
    private Map<String, String> userReactions = new HashMap<>();
    private Map<String, String> userReactionNames = new HashMap<>();

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

    public Map<String, Long> getReactionsSummary() {
        return reactionsSummary != null ? reactionsSummary : new HashMap<>();
    }

    public void setReactionsSummary(Map<String, Long> reactionsSummary) {
        this.reactionsSummary = reactionsSummary;
    }

    public Map<String, String> getUserReactions() {
        return userReactions != null ? userReactions : new HashMap<>();
    }

    public void setUserReactions(Map<String, String> userReactions) {
        this.userReactions = userReactions;
    }

    public Map<String, String> getUserReactionNames() {
        return userReactionNames != null ? userReactionNames : new HashMap<>();
    }

    public void setUserReactionNames(Map<String, String> userReactionNames) {
        this.userReactionNames = userReactionNames;
    }

    public String getCurrentUserReaction(String uid) {
        if (uid == null || userReactions == null) return "";
        String rx = userReactions.get(uid);
        return rx != null ? rx : "";
    }

    public String getTopReactionEmojis() {
        if (reactionsSummary != null && !reactionsSummary.isEmpty()) {
            List<Map.Entry<String, Long>> sorted = new ArrayList<>(reactionsSummary.entrySet());
            Collections.sort(sorted, (a, b) -> Long.compare(b.getValue() != null ? b.getValue() : 0, a.getValue() != null ? a.getValue() : 0));
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (Map.Entry<String, Long> entry : sorted) {
                if (entry.getValue() != null && entry.getValue() > 0) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(entry.getKey());
                    count++;
                    if (count >= 3) break;
                }
            }
            if (sb.length() > 0) return sb.toString();
        }
        return reactionType != null && !reactionType.isEmpty() ? reactionType : "❤️";
    }
}
