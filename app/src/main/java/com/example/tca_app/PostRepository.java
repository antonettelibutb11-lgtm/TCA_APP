package com.example.tca_app;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for all Post-related Firestore operations.
 * No Activity, Fragment, or Adapter should access FirebaseFirestore for posts directly.
 *
 * Scalability decisions:
 * - Server-side orderBy("timestamp", DESC) — no client-side sorting of entire collections.
 * - limit(50) per page — prevents loading thousands of documents into memory.
 * - Cursor pagination via startAfter(lastSnapshot) for infinite scroll.
 * - Moderation filter applied server-side where possible via a composite index fallback.
 */
public class PostRepository {

    private static final int PAGE_SIZE = 50;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration postsListenerRegistration;

    // ------------------------------------------------------------
    // LIVE FEED — real-time snapshot listener (HomeFeedFragment)
    // ------------------------------------------------------------

    /**
     * Attaches a real-time Firestore listener for the post feed.
     * Posts are ordered by timestamp DESC, limited to PAGE_SIZE.
     * Moderation filtering (DELETED/FLAGGED/ARCHIVED) is done client-side
     * to avoid requiring a composite index. Category filtering is also client-side
     * since mixing orderBy + whereEqualTo on different fields requires composite indexes.
     *
     * @param category  "All" or a specific category string
     * @param liveData  MutableLiveData to publish results to
     * @return the ListenerRegistration so the caller can remove it on destroy
     */
    public ListenerRegistration listenToFeedPosts(
            String category,
            MutableLiveData<List<Post>> liveData,
            MutableLiveData<String> errorLiveData) {

        removePostsListener(); // clean up any existing listener

        Query query = db.collection("posts");

        // Server-side category filter to prevent 'Empty Feed' bug when latest 50 posts don't match
        if (!isNullOrEmpty(category) && !"All".equalsIgnoreCase(category)) {
            query = query.whereEqualTo("category", category);
        }

        query = query.orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        postsListenerRegistration = query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                if (errorLiveData != null) {
                    errorLiveData.postValue(error.getMessage());
                }
                return;
            }
            if (snapshots == null) return;

            List<Post> result = new ArrayList<>();
            long now = System.currentTimeMillis();

            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                // Server-side moderation filter
                String moderationStatus = doc.getString("moderationStatus");
                if ("DELETED".equals(moderationStatus)
                        || "FLAGGED".equals(moderationStatus)
                        || "ARCHIVED".equals(moderationStatus)) {
                    continue;
                }

                Boolean isRepost = doc.getBoolean("isRepost");
                Boolean isStudentRepost = doc.getBoolean("isStudentRepost");
                String badgeText = doc.getString("badgeText");
                if (Boolean.TRUE.equals(isRepost) || Boolean.TRUE.equals(isStudentRepost) || "Repost".equalsIgnoreCase(badgeText)) {
                    // Student reposts belong strictly to the student's personal profile (like Facebook),
                    // not on the official Campus Feed or Admin view
                    continue;
                }

                Post post = documentToPost(doc, now);
                if (post != null) result.add(post);
            }

            liveData.postValue(result);
        });

        return postsListenerRegistration;
    }

    /**
     * One-shot paginated fetch for ProfileFragment (no real-time needed).
     * Uses orderBy + limit + optional startAfter cursor for pagination.
     */
    public void fetchUserPostsOnce(
            MutableLiveData<List<Post>> liveData,
            DocumentSnapshot startAfterCursor) {

        Query query = db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        if (startAfterCursor != null) {
            query = query.startAfter(startAfterCursor);
        }

        query.get().addOnSuccessListener(snapshots -> {
            List<Post> result = new ArrayList<>();
            long now = System.currentTimeMillis();

            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    String moderationStatus = doc.getString("moderationStatus");
                    if ("DELETED".equals(moderationStatus)
                            || "FLAGGED".equals(moderationStatus)
                            || "ARCHIVED".equals(moderationStatus)) {
                        continue;
                    }
                    Post post = documentToPost(doc, now);
                    if (post != null) result.add(post);
                }
            }
            liveData.postValue(result);
        }).addOnFailureListener(e -> liveData.postValue(new ArrayList<>()));
    }

    // ------------------------------------------------------------
    // WRITE OPERATIONS (called from ViewModel → adapter callbacks)
    // ------------------------------------------------------------

    public void toggleLike(String postId, boolean alreadyLiked, String uid,
                           Runnable onSuccess, java.util.function.Consumer<String> onFailure) {
        if (isNullOrEmpty(postId) || isNullOrEmpty(uid)) return;

        DocumentReference likeRef = db.collection("posts")
                .document(postId).collection("likes").document(uid);

        if (alreadyLiked) {
            likeRef.delete()
                    .addOnSuccessListener(v ->
                            db.collection("posts").document(postId)
                                    .update("likeCount", FieldValue.increment(-1))
                                    .addOnCompleteListener(t -> { if (onSuccess != null) onSuccess.run(); }))
                    .addOnFailureListener(e -> { if (onFailure != null) onFailure.accept(e.getMessage()); });
        } else {
            Map<String, Object> likeData = new HashMap<>();
            likeData.put("uid", uid);
            likeData.put("timestamp", FieldValue.serverTimestamp());
            likeRef.set(likeData)
                    .addOnSuccessListener(v ->
                            db.collection("posts").document(postId)
                                    .update("likeCount", FieldValue.increment(1))
                                    .addOnCompleteListener(t -> { if (onSuccess != null) onSuccess.run(); }))
                    .addOnFailureListener(e -> { if (onFailure != null) onFailure.accept(e.getMessage()); });
        }
    }

    public void updatePostContent(String postId, String newContent,
                                  Runnable onSuccess, java.util.function.Consumer<String> onFailure) {
        if (isNullOrEmpty(postId)) return;
        db.collection("posts").document(postId)
                .update("content", newContent)
                .addOnSuccessListener(v -> { if (onSuccess != null) onSuccess.run(); })
                .addOnFailureListener(e -> { if (onFailure != null) onFailure.accept(e.getMessage()); });
    }

    public void setPinStatus(String postId, boolean pinned,
                             Runnable onSuccess, java.util.function.Consumer<String> onFailure) {
        if (isNullOrEmpty(postId)) return;
        db.collection("posts").document(postId)
                .update("isPinned", pinned)
                .addOnSuccessListener(v -> { if (onSuccess != null) onSuccess.run(); })
                .addOnFailureListener(e -> { if (onFailure != null) onFailure.accept(e.getMessage()); });
    }

    public void archivePost(String postId,
                            Runnable onSuccess, java.util.function.Consumer<String> onFailure) {
        if (isNullOrEmpty(postId)) return;
        db.collection("posts").document(postId)
                .update("moderationStatus", "ARCHIVED")
                .addOnSuccessListener(v -> { if (onSuccess != null) onSuccess.run(); })
                .addOnFailureListener(e -> { if (onFailure != null) onFailure.accept(e.getMessage()); });
    }

    public void deletePost(String postId,
                           Runnable onSuccess, java.util.function.Consumer<String> onFailure) {
        if (isNullOrEmpty(postId)) return;
        db.collection("posts").document(postId)
                .delete()
                .addOnSuccessListener(v -> { if (onSuccess != null) onSuccess.run(); })
                .addOnFailureListener(e -> { if (onFailure != null) onFailure.accept(e.getMessage()); });
    }

    public void repostPost(Post originalPost, FirebaseUser currentUser,
                           Runnable onSuccess, java.util.function.Consumer<String> onFailure) {
        if (currentUser == null) return;
        String name = PostAdapter.getSafeDisplayName(currentUser);

        Map<String, Object> repostMap = new HashMap<>();
        repostMap.put("authorName", name);
        repostMap.put("authorUid", currentUser.getUid());
        repostMap.put("postMeta", "Just now • Reposted from " + originalPost.getAuthorName());
        repostMap.put("content", originalPost.getContent());
        repostMap.put("badgeText", "Repost");
        repostMap.put("isRepost", true);
        repostMap.put("isStudentRepost", true);
        repostMap.put("originalPostId", originalPost.getId());
        repostMap.put("originalAuthor", originalPost.getAuthorName());
        repostMap.put("category", isNullOrEmpty(originalPost.getCategory()) ? "General" : originalPost.getCategory());
        repostMap.put("isPinned", false);
        repostMap.put("isAiPick", false);
        repostMap.put("likeCount", 0);
        repostMap.put("loveCount", 0);
        repostMap.put("commentCount", 0);
        repostMap.put("timestamp", System.currentTimeMillis());
        repostMap.put("scheduledTimestamp", System.currentTimeMillis());
        repostMap.put("moderationStatus", "APPROVED");

        if (!isNullOrEmpty(originalPost.getPhotoUri()))  repostMap.put("photoUri", originalPost.getPhotoUri());
        if (!isNullOrEmpty(originalPost.getVideoUri()))  repostMap.put("videoUri", originalPost.getVideoUri());
        if (!isNullOrEmpty(originalPost.getDocUri()))    repostMap.put("docUri", originalPost.getDocUri());
        if (originalPost.getMediaUris() != null && !originalPost.getMediaUris().isEmpty())
            repostMap.put("mediaUris", originalPost.getMediaUris());

        db.collection("posts").add(repostMap)
                .addOnSuccessListener(d -> { if (onSuccess != null) onSuccess.run(); })
                .addOnFailureListener(e -> { if (onFailure != null) onFailure.accept(e.getMessage()); });
    }

    public void reportPost(String postId, String content, String authorName, String uid,
                           Runnable onSuccess, java.util.function.Consumer<String> onFailure) {
        Map<String, Object> flagData = new HashMap<>();
        flagData.put("postId", postId);
        flagData.put("content", content != null ? content : "");
        flagData.put("authorName", authorName != null ? authorName : "");
        flagData.put("reportedByUid", uid);
        flagData.put("reason", "Reported by User");
        flagData.put("aiScore", "User Flag");
        flagData.put("moderationStatus", "PENDING");
        flagData.put("timestamp", System.currentTimeMillis());

        db.collection("moderation_queue").add(flagData)
                .addOnSuccessListener(d -> { if (onSuccess != null) onSuccess.run(); })
                .addOnFailureListener(e -> { if (onFailure != null) onFailure.accept(e.getMessage()); });
    }

    public void addComment(String postId, String commentText, String authorName,
                           String replyToCommentId,
                           Runnable onSuccess, java.util.function.Consumer<String> onFailure) {
        Map<String, Object> commentMap = new HashMap<>();
        commentMap.put("author", authorName);
        commentMap.put("text", commentText);
        commentMap.put("timestamp", System.currentTimeMillis());

        if (!isNullOrEmpty(replyToCommentId)) {
            db.collection("posts").document(postId)
                    .collection("comments").document(replyToCommentId)
                    .collection("replies").add(commentMap)
                    .addOnSuccessListener(d -> { if (onSuccess != null) onSuccess.run(); })
                    .addOnFailureListener(e -> { if (onFailure != null) onFailure.accept(e.getMessage()); });
        } else {
            db.collection("posts").document(postId)
                    .collection("comments").add(commentMap)
                    .addOnSuccessListener(d ->
                            db.collection("posts").document(postId)
                                    .update("commentCount", FieldValue.increment(1))
                                    .addOnCompleteListener(t -> { if (onSuccess != null) onSuccess.run(); }))
                    .addOnFailureListener(e -> { if (onFailure != null) onFailure.accept(e.getMessage()); });
        }
    }

    public void addReaction(String postId, String uid, String reactionType,
                            Runnable onSuccess) {
        Map<String, Object> reactionData = new HashMap<>();
        reactionData.put("uid", uid);
        reactionData.put("type", reactionType);
        reactionData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("posts").document(postId)
                .collection("likes").document(uid)
                .set(reactionData)
                .addOnSuccessListener(v -> {
                    db.collection("posts").document(postId)
                            .update("likeCount", FieldValue.increment(1));
                    if (onSuccess != null) onSuccess.run();
                });
    }

    // ------------------------------------------------------------
    // CLEANUP
    // ------------------------------------------------------------

    public void removePostsListener() {
        if (postsListenerRegistration != null) {
            postsListenerRegistration.remove();
            postsListenerRegistration = null;
        }
    }

    // ------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------

    /**
     * Maps a Firestore DocumentSnapshot to a Post model object.
     * Centralised here so no Activity/Adapter contains mapping logic.
     */
    public static Post documentToPost(DocumentSnapshot doc, long now) {
        if (doc == null || !doc.exists()) return null;

        String authorName   = doc.getString("authorName");
        String content      = doc.getString("content");
        String badgeText    = doc.getString("badgeText");
        String category     = doc.getString("category");
        String authorUid    = doc.getString("authorUid");
        String photoUri     = doc.getString("photoUri");
        String videoUri     = doc.getString("videoUri");
        String docUri       = doc.getString("docUri");
        Boolean isPinned    = doc.getBoolean("isPinned");
        Boolean isAiPick    = doc.getBoolean("isAiPick");
        Long likeCount      = doc.getLong("likeCount");
        Long loveCount      = doc.getLong("loveCount");
        Long commentCount   = doc.getLong("commentCount");
        Long timestamp      = doc.getLong("timestamp");

        List<String> mediaUris = new ArrayList<>();
        if (doc.contains("mediaUris") && doc.get("mediaUris") instanceof List) {
            List<?> rawList = (List<?>) doc.get("mediaUris");
            if (rawList != null) {
                for (Object o : rawList) {
                    if (o instanceof String) mediaUris.add((String) o);
                }
            }
        }

        if (timestamp == null || timestamp == 0) timestamp = now;
        if (category == null || category.isEmpty()) category = "Events";

        String postMeta = TimeUtils.getRelativeTimeString(null, timestamp, category != null && !category.isEmpty() ? category : "Events");

        Post post = new Post(
                authorName != null ? authorName : "BISU Community",
                postMeta,
                content != null ? content : "",
                badgeText != null ? badgeText : "",
                category,
                isPinned != null && isPinned,
                isAiPick != null && isAiPick,
                likeCount != null ? likeCount.intValue() : 0,
                loveCount != null ? loveCount.intValue() : 0,
                commentCount != null ? commentCount.intValue() : 0
        );
        post.setTimestamp(timestamp);
        post.setId(doc.getId());
        post.setAuthorUid(authorUid);
        post.setPhotoUri(photoUri);
        post.setVideoUri(videoUri);
        post.setDocUri(docUri);
        post.setMediaUris(mediaUris);
        Boolean isRepost = doc.getBoolean("isRepost");
        Boolean isStudentRepost = doc.getBoolean("isStudentRepost");
        post.setRepost(Boolean.TRUE.equals(isRepost) || Boolean.TRUE.equals(isStudentRepost) || "Repost".equalsIgnoreCase(badgeText));
        post.setOriginalPostId(doc.getString("originalPostId"));
        post.setOriginalAuthor(doc.getString("originalAuthor"));
        return post;
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
