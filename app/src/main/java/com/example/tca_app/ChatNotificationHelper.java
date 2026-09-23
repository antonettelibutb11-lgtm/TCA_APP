package com.example.tca_app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class ChatNotificationHelper {

    private static final String TAG = "ChatNotificationHelper";
    public static final String CHANNEL_ID = "tca_chat_channel";
    public static final String CHANNEL_NAME = "Campus Inquiries & Editorial Messages";
    private static final String PREF_NAME = "tca_chat_notif_prefs";
    private static final String KEY_LAST_SEEN_TS = "last_seen_message_ts_";

    public static volatile String activeChatId = null;

    private static ListenerRegistration chatListener = null;
    private static UnreadCountListener unreadListener = null;
    private static long appSessionStartTime = System.currentTimeMillis();
    private static final Map<String, Long> notifiedTimestamps = new HashMap<>();

    public interface UnreadCountListener {
        void onUnreadCountChanged(int totalUnread);
    }

    public static void setUnreadListener(UnreadCountListener listener) {
        unreadListener = listener;
    }

    public static void initNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Real-time notifications for student inquiries and replies");
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 250, 150, 250});
                channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static synchronized void startListening(Context context) {
        initNotificationChannel(context);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            stopListening();
            return;
        }

        final String currentUid = user.getUid();
        final Context appContext = context.getApplicationContext();

        if (chatListener != null) {
            chatListener.remove();
            chatListener = null;
        }

        appSessionStartTime = System.currentTimeMillis();

        chatListener = FirebaseFirestore.getInstance().collection("chats")
                .whereArrayContains("participants", currentUid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Chat notification listener error: " + error.getMessage());
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

                        // Determine if unread
                        boolean isFromOther = lastSenderId != null && !lastSenderId.equals(currentUid);
                        long lastReadTs = getLastReadTimestamp(appContext, currentUid, chatId);

                        if (isFromOther && timestamp > lastReadTs) {
                            unreadCount++;
                        }

                        // Determine whether to trigger a Heads-Up alert
                        if (isFromOther && timestamp > (appSessionStartTime - 3000)) {
                            Long lastNotified = notifiedTimestamps.get(chatId);
                            if (lastNotified == null || timestamp > lastNotified) {
                                notifiedTimestamps.put(chatId, timestamp);

                                // Check if user is currently looking at this conversation
                                if (activeChatId == null || !activeChatId.equals(chatId)) {
                                    // Target recipient resolution
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

    public static synchronized void stopListening() {
        if (chatListener != null) {
            chatListener.remove();
            chatListener = null;
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

    public static void showChatNotification(Context context, String chatId, String recipientUid,
                                            String recipientName, String senderName, String messageText,
                                            boolean isAdminReply) {
        try {
            initNotificationChannel(context);

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

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_bisu_logo_hd)
                    .setColor(context.getResources().getColor(R.color.purple_primary, null))
                    .setContentTitle(title)
                    .setContentText(body)
                    .setSubText("The Campus Access")
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body).setSummaryText("The Campus Access"))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setSound(defaultSound)
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
}
