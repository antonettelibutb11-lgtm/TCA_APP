package com.example.tca_app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles incoming FCM Push Notifications in foreground and background.
 * Triggers heads-up (pop-up) system notifications on Android devices for:
 * 1. Direct Messages & Inquiries
 * 2. Post Likes, Reactions & Comments
 * 3. Newly published campus posts & announcements
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "The Campus Access";
        String body = "New update available!";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        } else if (!remoteMessage.getData().isEmpty()) {
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().containsKey("body")) {
                body = remoteMessage.getData().get("body");
            }
        }

        String type = remoteMessage.getData().get("type");
        if ("chat".equalsIgnoreCase(type) || remoteMessage.getData().containsKey("chatId")) {
            String chatId = remoteMessage.getData().get("chatId");
            String recipientUid = remoteMessage.getData().get("recipientUid");
            String recipientName = remoteMessage.getData().get("recipientName");
            boolean isAdminReply = Boolean.parseBoolean(remoteMessage.getData().get("isAdminReply"));
            sendChatHeadsUpNotification(title, body, chatId, recipientUid, recipientName, isAdminReply);
        } else if ("like".equalsIgnoreCase(type) || "reaction".equalsIgnoreCase(type) || "comment".equalsIgnoreCase(type)) {
            String postId = remoteMessage.getData().get("postId");
            sendLikeHeadsUpNotification(title, body, postId);
        } else if ("new_post".equalsIgnoreCase(type)) {
            String postId = remoteMessage.getData().get("postId");
            sendNewPostHeadsUpNotification(title, body, postId);
        } else {
            sendGeneralHeadsUpNotification(title, body);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getUid() != null && !user.getUid().trim().isEmpty()) {
                Map<String, Object> tokenUpdate = new HashMap<>();
                tokenUpdate.put("fcmToken", token);
                tokenUpdate.put("fcmTokens", FieldValue.arrayUnion(token));
                tokenUpdate.put("lastTokenSync", FieldValue.serverTimestamp());

                FirebaseFirestore.getInstance()
                        .collection("users").document(user.getUid())
                        .set(tokenUpdate, com.google.firebase.firestore.SetOptions.merge());
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during onNewToken processing: " + e.getMessage(), e);
        }
    }

    private void sendChatHeadsUpNotification(String title, String body, String chatId, String recipientUid, String recipientName, boolean isAdminReply) {
        ChatNotificationHelper.initNotificationChannels(this);

        Intent intent = new Intent(this, MessageActivity.class);
        if (chatId != null && !chatId.trim().isEmpty()) intent.putExtra("CHAT_ID", chatId);
        if (recipientUid != null) intent.putExtra("RECIPIENT_UID", recipientUid);
        if (recipientName != null) intent.putExtra("RECIPIENT_NAME", recipientName);
        intent.putExtra("IS_ADMIN_REPLY", isAdminReply);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) (System.currentTimeMillis() % 100000), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ChatNotificationHelper.CHANNEL_CHAT_ID)
                .setSmallIcon(R.drawable.ic_bisu_logo_hd)
                .setColor(ContextCompat.getColor(this, R.color.purple_primary))
                .setContentTitle(title)
                .setContentText(body)
                .setSubText("The Campus Access")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body).setSummaryText("The Campus Access"))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(defaultSound)
                .setVibrate(new long[]{0, 250, 150, 250})
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
        }
    }

    private void sendLikeHeadsUpNotification(String title, String body, String postId) {
        ChatNotificationHelper.initNotificationChannels(this);

        Intent intent = new Intent(this, MainActivity.class);
        if (postId != null && !postId.trim().isEmpty()) intent.putExtra("OPEN_POST_ID", postId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) (System.currentTimeMillis() % 100000), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ChatNotificationHelper.CHANNEL_LIKES_ID)
                .setSmallIcon(R.drawable.ic_bisu_logo_hd)
                .setColor(ContextCompat.getColor(this, R.color.purple_primary))
                .setContentTitle(title)
                .setContentText(body)
                .setSubText("The Campus Access")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body).setSummaryText("The Campus Access"))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(defaultSound)
                .setVibrate(new long[]{0, 200, 100, 200})
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
        }
    }

    private void sendNewPostHeadsUpNotification(String title, String body, String postId) {
        ChatNotificationHelper.initNotificationChannels(this);

        Intent intent = new Intent(this, MainActivity.class);
        if (postId != null && !postId.trim().isEmpty()) intent.putExtra("OPEN_POST_ID", postId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) (System.currentTimeMillis() % 100000), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ChatNotificationHelper.CHANNEL_POSTS_ID)
                .setSmallIcon(R.drawable.ic_bisu_logo_hd)
                .setColor(ContextCompat.getColor(this, R.color.purple_primary))
                .setContentTitle(title)
                .setContentText(body)
                .setSubText("The Campus Access")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body).setSummaryText("The Campus Access"))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(defaultSound)
                .setVibrate(new long[]{0, 250, 150, 250})
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
        }
    }

    private void sendGeneralHeadsUpNotification(String title, String body) {
        ChatNotificationHelper.initNotificationChannels(this);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ChatNotificationHelper.CHANNEL_POSTS_ID)
                .setSmallIcon(R.drawable.ic_bisu_logo_hd)
                .setColor(ContextCompat.getColor(this, R.color.purple_primary))
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(defaultSound)
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
        }
    }
}
