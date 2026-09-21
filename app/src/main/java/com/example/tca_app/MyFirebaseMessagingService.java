package com.example.tca_app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "campus_access_notifications";
    private static final String CHANNEL_NAME = "Campus Access Announcements";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "📢 Campus Access Notification";
        String body = "New advisory or update available!";

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

        sendHeadsUpNotification(title, body);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Persist token safely to Firestore only when user is authenticated
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getUid() != null && !user.getUid().trim().isEmpty()) {
                FirebaseFirestore.getInstance()
                        .collection("users").document(user.getUid())
                        .update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayUnion(token));
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during onNewToken processing: " + e.getMessage(), e);
        }
    }

    private void sendHeadsUpNotification(String title, String body) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for campus announcements and advisories");
            channel.enableVibration(true);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bisu_logo_hd)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}
