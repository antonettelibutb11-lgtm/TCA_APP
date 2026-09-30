package com.example.tca_app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

/**
 * Central Notification System for The Campus Access.
 * Manages heads-up (pop-up) system notifications on Android devices for:
 * 1. Direct Messages / Student Inquiries (tca_chat_channel)
 * 2. Likes, Comments & Post Reactions (tca_likes_channel)
 * 3. New Campus Posts & Announcements (tca_posts_channel)
 */
public class ChatNotificationHelper {

    private static final String TAG = "CampusNotifHelper";

    // Channel IDs
    public static final String CHANNEL_CHAT_ID = "tca_chat_channel";
    public static final String CHANNEL_LIKES_ID = "tca_likes_channel";
    public static final String CHANNEL_POSTS_ID = "tca_posts_channel";

    // Backward-compatibility aliases
    public static final String CHANNEL_ID = CHANNEL_CHAT_ID;
    public static final String CHANNEL_NAME = "Campus Inquiries & Editorial Messages";

    private static final String PREF_NAME = "tca_chat_notif_prefs";
    private static final String KEY_LAST_SEEN_TS = "last_seen_message_ts_";

    public static volatile String activeChatId = null;

    private static ListenerRegistration chatListener = null;
    private static ListenerRegistration userNotifListener = null;
    private static ListenerRegistration newPostListener = null;
    private static UnreadCountListener unreadListener = null;

    private static long appSessionStartTime = System.currentTimeMillis();
    private static final Map<String, Long> notifiedTimestamps = new HashMap<>();

    public interface UnreadCountListener {
        void onUnreadCountChanged(int totalUnread);
    }

    public static void setUnreadListener(UnreadCountListener listener) {
        unreadListener = listener;
    }

    /**
     * Initializes all high-importance notification channels on Android 8.0 (API 26) and above.
     * High importance is required for Android system to display floating Heads-Up Pop-up Banners.
     */
    public static void initNotificationChannel(Context context) {
        initNotificationChannels(context);
    }

    public static void initNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                    .build();

            // 1. Chat & Messages Channel (Heads-Up Pop-Up)
            NotificationChannel chatChannel = new NotificationChannel(
                    CHANNEL_CHAT_ID,
                    "Campus Inquiries & Direct Messages",
                    NotificationManager.IMPORTANCE_HIGH
            );
            chatChannel.setDescription("Heads-up pop-up alerts for direct messages and student inquiries");
            chatChannel.enableVibration(true);
            chatChannel.setVibrationPattern(new long[]{0, 250, 150, 250});
            chatChannel.setSound(defaultSound, audioAttributes);
            chatChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(chatChannel);

            // 2. Likes, Comments & Reactions Channel (Heads-Up Pop-Up)
            NotificationChannel likesChannel = new NotificationChannel(
                    CHANNEL_LIKES_ID,
                    "Likes, Reactions & Comments",
                    NotificationManager.IMPORTANCE_HIGH
            );
            likesChannel.setDescription("Heads-up pop-up alerts when someone likes or comments on your post");
            likesChannel.enableVibration(true);
            likesChannel.setVibrationPattern(new long[]{0, 200, 100, 200});
            likesChannel.setSound(defaultSound, audioAttributes);
            likesChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(likesChannel);

            // 3. New Posts & Announcements Channel (Heads-Up Pop-Up)
            NotificationChannel postsChannel = new NotificationChannel(
                    CHANNEL_POSTS_ID,
                    "New Campus Posts & Advisories",
                    NotificationManager.IMPORTANCE_HIGH
            );
            postsChannel.setDescription("Heads-up pop-up alerts when new announcements or articles are published");
            postsChannel.enableVibration(true);
            postsChannel.setVibrationPattern(new long[]{0, 250, 150, 250});
            postsChannel.setSound(defaultSound, audioAttributes);
            postsChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(postsChannel);
        }
    }

    /**
     * Subscribes device to the campus_posts FCM topic and syncs the FCM token to Firestore.
     */
    public static void syncFcmTokenAndTopics(Context context) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("campus_posts")
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Subscribed to FCM topic: campus_posts"))
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to subscribe to campus_posts topic: " + e.getMessage()));

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getUid() != null && !user.getUid().trim().isEmpty()) {
                final String uid = user.getUid();
                FirebaseMessaging.getInstance().getToken()
                        .addOnSuccessListener(token -> {
                            if (token != null && !token.trim().isEmpty()) {
                                Map<String, Object> tokenUpdate = new HashMap<>();
                                tokenUpdate.put("fcmToken", token);
                                tokenUpdate.put("fcmTokens", FieldValue.arrayUnion(token));
                                tokenUpdate.put("lastTokenSync", FieldValue.serverTimestamp());

                                FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(uid)
                                        .set(tokenUpdate, SetOptions.merge())
                                        .addOnSuccessListener(v -> Log.d(TAG, "FCM token saved to users/" + uid))
                                        .addOnFailureListener(err -> Log.w(TAG, "Failed to sync FCM token: " + err.getMessage()));
                            }
                        })
                        .addOnFailureListener(e -> Log.w(TAG, "Failed to retrieve FCM token: " + e.getMessage()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during syncFcmTokenAndTopics: " + e.getMessage(), e);
        }
    }

    /**
     * Starts real-time listeners for Chats, Likes/Comments, and New Posts.
     */
    public static synchronized void startListening(Context context) {
        initNotificationChannels(context);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            stopListening();
            return;
        }

        final String currentUid = user.getUid();
        final Context appContext = context.getApplicationContext();

        stopListening();
        appSessionStartTime = System.currentTimeMillis();

        syncFcmTokenAndTopics(appContext);

        // 1. Real-time Chat Listener
        startChatListener(appContext, currentUid);

        // 2. Real-time User Interaction Listener (Likes & Comments on user's posts)
        startUserInteractionListener(appContext, currentUid);

        // 3. Real-time New Post Listener (New campus posts published by others)
        startNewPostListener(appContext, currentUid);
    }

    private static void startChatListener(Context appContext, String currentUid) {
        chatListener = FirebaseFirestore.getInstance().collection("chats")
                .whereArrayContains("participants", currentUid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Chat listener error: " + error.getMessage());
                        return;
                    }
                    if (snapshots == null) return;

                    int unreadCount = 0;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String chatId = doc.getId();
                        String lastSenderId = doc.getString("lastSenderId");
                        String lastSenderName = doc.getString("lastSenderName");
                        String lastMessage = doc.getString("lastMessage");

                        long timestamp = 0L;
                        Object tsObj = doc.get("lastMessageTimestamp");
                        if (tsObj instanceof Long) {
                            timestamp = (Long) tsObj;
                        } else if (tsObj instanceof Number) {
                            timestamp = ((Number) tsObj).longValue();
                        }

                        boolean isFromOther = lastSenderId != null && !lastSenderId.equals(currentUid);
                        long lastReadTs = getLastReadTimestamp(appContext, currentUid, chatId);

                        if (isFromOther && timestamp > lastReadTs) {
                            unreadCount++;
                        }

                        // Trigger Heads-Up Pop-Up Alert on phone
                        if (isFromOther && timestamp > (appSessionStartTime - 3000)) {
                            Long lastNotified = notifiedTimestamps.get(chatId);
                            if (lastNotified == null || timestamp > lastNotified) {
                                notifiedTimestamps.put(chatId, timestamp);

                                if (activeChatId == null || !activeChatId.equals(chatId)) {
                                    String studentUid = doc.getString("studentUid");
                                    String adminUid = doc.getString("adminUid");
                                    String studentName = doc.getString("studentName");
                                    String recipientUid = currentUid.equals(studentUid) ? (adminUid != null ? adminUid : "campus_admin_desk") : studentUid;
                                    String recipientName = currentUid.equals(studentUid) ? "The Campus Access Editorial Desk" : (studentName != null ? studentName : "Student Inquiry");
                                    boolean isAdminReply = !currentUid.equals(studentUid);

                                    showChatNotification(appContext, chatId, recipientUid, recipientName, lastSenderName, lastMessage, isAdminReply);
                                }
                            }
                        }
                    }

                    if (unreadListener != null) {
                        unreadListener.onUnreadCountChanged(unreadCount);
                    }
                });
    }

    private static void startUserInteractionListener(Context appContext, String currentUid) {
        userNotifListener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUid)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "User notification listener error: " + error.getMessage());
                        return;
                    }
                    if (snapshots == null) return;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String notifId = doc.getId();
                        String title = doc.getString("title");
                        String message = doc.getString("message");
                        String postId = doc.getString("postId");

                        long timestamp = 0L;
                        Object tsObj = doc.get("timestamp");
                        if (tsObj instanceof com.google.firebase.Timestamp) {
                            timestamp = ((com.google.firebase.Timestamp) tsObj).toDate().getTime();
                        } else if (tsObj instanceof Long) {
                            timestamp = (Long) tsObj;
                        }

                        if (timestamp > (appSessionStartTime - 3000)) {
                            Long lastNotified = notifiedTimestamps.get(notifId);
                            if (lastNotified == null) {
                                notifiedTimestamps.put(notifId, timestamp);
                                showLikeNotification(appContext,
                                        title != null ? title : "❤️ New Reaction",
                                        message != null ? message : "Someone reacted to your post!",
                                        postId);
                            }
                        }
                    }
                });
    }

    private static void startNewPostListener(Context appContext, String currentUid) {
        newPostListener = FirebaseFirestore.getInstance()
                .collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "New post listener error: " + error.getMessage());
                        return;
                    }
                    if (snapshots == null || snapshots.isEmpty()) return;

                    DocumentSnapshot doc = snapshots.getDocuments().get(0);
                    String postId = doc.getId();
                    String authorUid = doc.getString("authorUid");
                    String authorName = doc.getString("authorName");
                    String content = doc.getString("content");

                    long timestamp = 0L;
                    Object tsObj = doc.get("timestamp");
                    if (tsObj instanceof Long) {
                        timestamp = (Long) tsObj;
                    } else if (tsObj instanceof Number) {
                        timestamp = ((Number) tsObj).longValue();
                    }

                    // Only notify for other users' posts created during this active session
                    boolean isFromOther = authorUid == null || !authorUid.equals(currentUid);
                    if (isFromOther && timestamp > (appSessionStartTime - 2000)) {
                        Long lastNotified = notifiedTimestamps.get("post_" + postId);
                        if (lastNotified == null) {
                            notifiedTimestamps.put("post_" + postId, timestamp);
                            String snippet = content != null ? content.trim() : "";
                            if (snippet.length() > 65) snippet = snippet.substring(0, 65) + "...";
                            showNewPostNotification(appContext,
                                    authorName != null && !authorName.isEmpty() ? authorName : "The Campus Access",
                                    snippet,
                                    postId);
                        }
                    }
                });
    }

    public static synchronized void stopListening() {
        if (chatListener != null) {
            chatListener.remove();
            chatListener = null;
        }
        if (userNotifListener != null) {
            userNotifListener.remove();
            userNotifListener = null;
        }
        if (newPostListener != null) {
            newPostListener.remove();
            newPostListener = null;
        }
        notifiedTimestamps.clear();
    }

    public static void markChatAsRead(Context context, String chatId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || chatId == null) return;
        setLastReadTimestamp(context.getApplicationContext(), user.getUid(), chatId, System.currentTimeMillis());
    }

    private static long getLastReadTimestamp(Context context, String userId, String chatId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_SEEN_TS + userId + "_" + chatId, 0L);
    }

    private static void setLastReadTimestamp(Context context, String userId, String chatId, long timestamp) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_SEEN_TS + userId + "_" + chatId, timestamp).apply();
    }

    /**
     * Writes a like/reaction notification document to the post author's notifications subcollection.
     */
    public static void notifyPostLiked(Context context, Post post, String likerUid, String likerName, String reaction) {
        if (post == null || post.getAuthorUid() == null || post.getAuthorUid().trim().isEmpty()) return;
        if (likerUid != null && likerUid.equals(post.getAuthorUid())) {
            return; // Don't notify self
        }

        String snippet = post.getContent() != null ? post.getContent().trim() : "";
        if (snippet.length() > 50) {
            snippet = snippet.substring(0, 50) + "...";
        }

        String emoji = (reaction != null && !reaction.trim().isEmpty()) ? reaction : "👍";
        String title = emoji + " New Reaction on your post";
        String message = (likerName != null ? likerName : "Someone") + " reacted " + emoji + " to: \"" + snippet + "\"";

        Map<String, Object> notifData = new HashMap<>();
        notifData.put("type", "LIKE");
        notifData.put("title", title);
        notifData.put("message", message);
        notifData.put("senderUid", likerUid != null ? likerUid : "");
        notifData.put("senderName", likerName != null ? likerName : "Campus Member");
        notifData.put("reaction", emoji);
        notifData.put("postId", post.getId());
        notifData.put("timestamp", FieldValue.serverTimestamp());
        notifData.put("read", false);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(post.getAuthorUid())
                .collection("notifications")
                .add(notifData)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to write like notification: " + e.getMessage()));
    }

    /**
     * Writes a comment notification document to the post author's notifications subcollection.
     */
    public static void notifyCommentAdded(Context context, Post post, String commenterUid, String commenterName, String commentText) {
        if (post == null || post.getAuthorUid() == null || post.getAuthorUid().trim().isEmpty()) return;
        if (commenterUid != null && commenterUid.equals(post.getAuthorUid())) {
            return; // Don't notify self
        }

        String commentSnippet = (commentText != null) ? commentText.trim() : "";
        if (commentSnippet.length() > 50) {
            commentSnippet = commentSnippet.substring(0, 50) + "...";
        }

        String title = "💬 New Comment on your post";
        String message = (commenterName != null ? commenterName : "Someone") + " commented: \"" + commentSnippet + "\"";

        Map<String, Object> notifData = new HashMap<>();
        notifData.put("type", "COMMENT");
        notifData.put("title", title);
        notifData.put("message", message);
        notifData.put("senderUid", commenterUid != null ? commenterUid : "");
        notifData.put("senderName", commenterName != null ? commenterName : "Campus Member");
        notifData.put("commentText", commentSnippet);
        notifData.put("postId", post.getId());
        notifData.put("timestamp", FieldValue.serverTimestamp());
        notifData.put("read", false);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(post.getAuthorUid())
                .collection("notifications")
                .add(notifData)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to write comment notification: " + e.getMessage()));
    }

    /**
     * Displays a Heads-Up Pop-Up Banner for Direct Messages.
     */
    public static void showChatNotification(Context context, String chatId, String recipientUid,
                                            String recipientName, String senderName, String messageText,
                                            boolean isAdminReply) {
        try {
            initNotificationChannels(context);

            Intent intent = new Intent(context, MessageActivity.class);
            intent.putExtra("CHAT_ID", chatId);
            intent.putExtra("RECIPIENT_UID", recipientUid);
            intent.putExtra("RECIPIENT_NAME", recipientName);
            intent.putExtra("IS_ADMIN_REPLY", isAdminReply);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            int requestCode = chatId.hashCode();
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            String title;
            if (isAdminReply) {
                title = "The Campus Access • Editorial Desk";
            } else {
                title = (senderName != null && !senderName.trim().isEmpty())
                        ? senderName + " (Student Inquiry)"
                        : "The Campus Access • Student Inquiry";
            }

            String body = (messageText != null && !messageText.trim().isEmpty())
                    ? messageText
                    : "Sent an attachment";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_CHAT_ID)
                    .setSmallIcon(R.drawable.ic_bisu_logo_hd)
                    .setColor(ContextCompat.getColor(context, R.color.purple_primary))
                    .setContentTitle(title)
                    .setContentText(body)
                    .setSubText("The Campus Access")
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body).setSummaryText("The Campus Access"))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setSound(defaultSound)
                    .setVibrate(new long[]{0, 250, 150, 250})
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                int notificationId = Math.abs(chatId.hashCode());
                manager.notify(notificationId, builder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to display chat notification: " + e.getMessage(), e);
        }
    }

    /**
     * Displays a Heads-Up Pop-Up Banner for Likes, Reactions & Comments.
     */
    public static void showLikeNotification(Context context, String title, String body, String postId) {
        try {
            initNotificationChannels(context);

            Intent intent = new Intent(context, MainActivity.class);
            if (postId != null && !postId.trim().isEmpty()) {
                intent.putExtra("OPEN_POST_ID", postId);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    (int) (System.currentTimeMillis() % 100000),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_LIKES_ID)
                    .setSmallIcon(R.drawable.ic_bisu_logo_hd)
                    .setColor(ContextCompat.getColor(context, R.color.purple_primary))
                    .setContentTitle(title)
                    .setContentText(body)
                    .setSubText("The Campus Access")
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body).setSummaryText("The Campus Access"))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setSound(defaultSound)
                    .setVibrate(new long[]{0, 200, 100, 200})
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                manager.notify(notificationId, builder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to display like notification: " + e.getMessage(), e);
        }
    }

    /**
     * Displays a Heads-Up Pop-Up Banner when a New Post is published.
     */
    public static void showNewPostNotification(Context context, String authorName, String postSnippet, String postId) {
        try {
            initNotificationChannels(context);

            Intent intent = new Intent(context, MainActivity.class);
            if (postId != null && !postId.trim().isEmpty()) {
                intent.putExtra("OPEN_POST_ID", postId);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    (int) (System.currentTimeMillis() % 100000),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            String title = "📰 New Post • The Campus Access";
            String body = authorName + ": \"" + postSnippet + "\"";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_POSTS_ID)
                    .setSmallIcon(R.drawable.ic_bisu_logo_hd)
                    .setColor(ContextCompat.getColor(context, R.color.purple_primary))
                    .setContentTitle(title)
                    .setContentText(body)
                    .setSubText("The Campus Access")
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body).setSummaryText("The Campus Access"))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setSound(defaultSound)
                    .setVibrate(new long[]{0, 250, 150, 250})
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                manager.notify(notificationId, builder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to display new post notification: " + e.getMessage(), e);
        }
    }
}
