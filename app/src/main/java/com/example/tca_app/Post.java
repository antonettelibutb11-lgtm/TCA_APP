package com.example.tca_app;

public class Post {
    private String authorName;
    private String authorUid;
    private String postMeta;
    private String content;
    private String badgeText;
    private String category;
    private boolean isPinned;
    private boolean isAiPick;
    private int likeCount;
    private int loveCount;
    private int commentCount;
    private long timestamp;
    private String id;
    private String photoUri;
    private String videoUri;
    private String docUri;
    private java.util.List<String> mediaUris = new java.util.ArrayList<>();
    private String imageHash = "";
    private String videoHash = "";
    private java.util.List<String> mediaHashes = new java.util.ArrayList<>();
    private String folderName = "";
    private String moderationStatus = "APPROVED";
    // SCALABILITY FIX: likedByUsers array removed from the document model.
    // Likes are now stored in the subcollection posts/{postId}/likes/{userId}.
    // isLikedByCurrentUser is a transient in-memory flag set by the adapter.
    private transient boolean isLikedByCurrentUser = false;

    // Required empty constructor for Firebase Firestore deserialization
    public Post() {}

    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public Post(String authorName, String postMeta, String content, String badgeText, String category, boolean isPinned, boolean isAiPick, int likeCount, int loveCount, int commentCount) {
        this.authorName = authorName;
        this.postMeta = postMeta;
        this.content = content;
        this.badgeText = badgeText;
        this.category = category;
        this.isPinned = isPinned;
        this.isAiPick = isAiPick;
        this.likeCount = likeCount;
        this.loveCount = loveCount;
        this.commentCount = commentCount;
        this.timestamp = System.currentTimeMillis();
    }

    public String getAuthorName() { return authorName != null ? authorName : ""; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorUid() { return authorUid != null ? authorUid : ""; }
    public void setAuthorUid(String authorUid) { this.authorUid = authorUid; }

    public String getPostMeta() { return postMeta != null ? postMeta : ""; }
    public void setPostMeta(String postMeta) { this.postMeta = postMeta; }

    public String getContent() { return content != null ? content : ""; }
    public void setContent(String content) { this.content = content; }

    public String getBadgeText() { return badgeText != null ? badgeText : ""; }
    public void setBadgeText(String badgeText) { this.badgeText = badgeText; }

    public String getCategory() { return category != null ? category : ""; }
    public void setCategory(String category) { this.category = category; }

    public String getFolderName() { return folderName != null ? folderName : ""; }
    public void setFolderName(String folderName) { this.folderName = folderName; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public boolean isAiPick() { return isAiPick; }
    public void setAiPick(boolean aiPick) { isAiPick = aiPick; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getLoveCount() { return loveCount; }
    public void setLoveCount(int loveCount) { this.loveCount = loveCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getPhotoUri() { return photoUri != null ? photoUri : ""; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }

    public String getVideoUri() { return videoUri != null ? videoUri : ""; }
    public void setVideoUri(String videoUri) { this.videoUri = videoUri; }

    public String getDocUri() { return docUri != null ? docUri : ""; }
    public void setDocUri(String docUri) { this.docUri = docUri; }

    public java.util.List<String> getMediaUris() { return mediaUris != null ? mediaUris : new java.util.ArrayList<>(); }
    public void setMediaUris(java.util.List<String> mediaUris) { this.mediaUris = mediaUris; }

    public String getImageHash() { return imageHash != null ? imageHash : ""; }
    public void setImageHash(String imageHash) { this.imageHash = imageHash; }

    public java.util.List<String> getMediaHashes() { return mediaHashes != null ? mediaHashes : new java.util.ArrayList<>(); }
    public void setMediaHashes(java.util.List<String> mediaHashes) { this.mediaHashes = mediaHashes; }

    public String getVideoHash() { return videoHash != null ? videoHash : ""; }
    public void setVideoHash(String videoHash) { this.videoHash = videoHash; }

    public String getModerationStatus() { return moderationStatus != null ? moderationStatus : "APPROVED"; }
    public void setModerationStatus(String moderationStatus) { this.moderationStatus = moderationStatus; }

    /** Transient in-memory flag — NOT persisted to Firestore. Set by PostAdapter after a subcollection check. */
    public boolean isLikedByCurrentUser() { return isLikedByCurrentUser; }
    public void setLikedByCurrentUser(boolean liked) { this.isLikedByCurrentUser = liked; }
}
