package com.example.tca_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 102;

    private RecyclerView rvChatMessages;
    private ChatMessageAdapter adapter;
    private List<ChatMessage> messageList;

    private EditText etChatMessage;
    private TextView tvChatRecipientName;
    private TextView tvChatRecipientStatus;
    private ImageView imgChatUserAvatar;

    private String recipientUid = "campus_admin_desk";
    private String recipientName = "The Campus Access";
    private String recipientEmail = "";
    private boolean isAdminReply = false;
    private boolean isUserAdmin = false;

    private String currentUid = "guest_user";
    private String currentSenderName = "BISU Student";
    private String chatId = "";

    private FirebaseFirestore db;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    private File tempCameraFile;
    private Uri tempCameraUri;

    // Gallery Picker Launcher
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadAndSendImage(uri, null);
                }
            }
    );

    // Camera Launcher
    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (Boolean.TRUE.equals(success) && tempCameraFile != null && tempCameraFile.exists()) {
                    uploadAndSendImage(tempCameraUri, tempCameraFile);
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        db = FirebaseFirestore.getInstance();

        handleIntentData(getIntent());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to send direct messages.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        currentUid = currentUser.getUid() != null ? currentUser.getUid() : "guest_user";

        ImageView btnBackChat = findViewById(R.id.btnBackChat);
        tvChatRecipientName = findViewById(R.id.tvChatRecipientName);
        tvChatRecipientStatus = findViewById(R.id.tvChatRecipientStatus);
        imgChatUserAvatar = findViewById(R.id.imgChatUserAvatar);
        etChatMessage = findViewById(R.id.etChatMessage);
        View btnSendMessage = findViewById(R.id.btnSendMessage);
        View btnAttachCamera = findViewById(R.id.btnAttachCamera);
        View btnAttachGallery = findViewById(R.id.btnAttachGallery);
        View btnAttachFile = findViewById(R.id.btnAttachFile);

        if (btnBackChat != null) {
            btnBackChat.setOnClickListener(v -> finish());
        }

        // Configure role & header display
        AuthUtils.checkCurrentUserAccess((isApprovedMember, isAdmin, role) -> {
            if (isFinishing() || isDestroyed()) return;
            isUserAdmin = isAdmin || isAdminReply;
            if (adapter != null) {
                adapter.setCurrentUserAdmin(isUserAdmin);
            }

            if (isUserAdmin) {
                // ADMIN VIEW: Talking to a student
                currentSenderName = "The Campus Access Editorial Desk";
                if (tvChatRecipientName != null) {
                    tvChatRecipientName.setText(recipientName != null && !recipientName.isEmpty() ? recipientName : "Student Inquiry");
                }
                if (tvChatRecipientStatus != null) {
                    tvChatRecipientStatus.setText("Student Inquiry • BISU Balilihan");
                }
            } else {
                // STUDENT VIEW: Talking to Editorial Desk
                currentSenderName = PostAdapter.getSafeDisplayName(currentUser);
                if (tvChatRecipientName != null) {
                    tvChatRecipientName.setText("The Campus Access Editorial Desk");
                }
                if (tvChatRecipientStatus != null) {
                    tvChatRecipientStatus.setText("Official BISU Balilihan Publication • Active Now");
                }
            }
        });

        rvChatMessages = findViewById(R.id.rvChatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);

        messageList = new ArrayList<>();
        adapter = new ChatMessageAdapter(messageList, currentUid);
        adapter.setCurrentUserAdmin(isAdminReply);
        rvChatMessages.setAdapter(adapter);

        if (btnSendMessage != null) {
            btnSendMessage.setOnClickListener(v -> sendMessage());
        }

        if (btnAttachCamera != null) {
            btnAttachCamera.setOnClickListener(v -> openCamera());
        }
        if (btnAttachGallery != null) {
            btnAttachGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        }
        if (btnAttachFile != null) {
            btnAttachFile.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        }

        resolveChatAndStartListening();
    }

    private void handleIntentData(Intent intent) {
        if (intent != null) {
            if (intent.hasExtra("CHAT_ID") && intent.getStringExtra("CHAT_ID") != null && !intent.getStringExtra("CHAT_ID").trim().isEmpty()) {
                chatId = intent.getStringExtra("CHAT_ID").trim();
            }
            if (intent.hasExtra("RECIPIENT_UID")) {
                recipientUid = intent.getStringExtra("RECIPIENT_UID");
            }
            if (intent.hasExtra("RECIPIENT_NAME")) {
                recipientName = intent.getStringExtra("RECIPIENT_NAME");
            }
            if (intent.hasExtra("RECIPIENT_EMAIL")) {
                recipientEmail = intent.getStringExtra("RECIPIENT_EMAIL");
            }
            isAdminReply = intent.getBooleanExtra("IS_ADMIN_REPLY", false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentData(intent);
        resolveChatAndStartListening();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            return;
        }

        try {
            tempCameraFile = File.createTempFile(
                    "chat_img_" + System.currentTimeMillis(),
                    ".jpg",
                    getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            );
            tempCameraUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    tempCameraFile
            );
            cameraLauncher.launch(tempCameraUri);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to take photo.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadAndSendImage(Uri uri, File file) {
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

        backgroundExecutor.execute(() -> {
            try {
                InputStream stream = (file != null && file.exists())
                        ? new FileInputStream(file)
                        : getContentResolver().openInputStream(uri);

                if (stream == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Could not open image stream.", Toast.LENGTH_SHORT).show());
                    return;
                }

                CloudinaryUploader.uploadImage(stream, new CloudinaryUploader.CloudinaryUploadCallback() {
                    @Override
                    public void onSuccess(String secureUrl) {
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                sendImageMessage(secureUrl);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                Toast.makeText(MessageActivity.this, "Image upload failed: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void sendImageMessage(String imageUrl) {
        String caption = etChatMessage.getText().toString().trim();
        etChatMessage.setText("");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        long now = System.currentTimeMillis();

        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("senderId", currentUid);
        msgMap.put("senderName", currentSenderName);
        msgMap.put("senderRole", isUserAdmin ? "ADMIN" : "STUDENT");
        msgMap.put("text", caption);
        msgMap.put("imageUrl", imageUrl);
        msgMap.put("messageType", "IMAGE");
        msgMap.put("timestamp", now);

        ChatMessage localMsg = new ChatMessage(currentUid, currentSenderName, caption, imageUrl, "IMAGE", now);
        messageList.add(localMsg);
        adapter.notifyItemInserted(messageList.size() - 1);
        rvChatMessages.scrollToPosition(messageList.size() - 1);

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(msgMap)
                .addOnFailureListener(e -> {
                    Toast.makeText(MessageActivity.this, "Failed to send image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        String summary = caption.isEmpty() ? "Sent a photo" : caption;
        updateConversationMetadata(summary, now, currentUser);

        if (tempCameraFile != null && tempCameraFile.exists()) {
            tempCameraFile.delete();
            tempCameraFile = null;
        }
    }

    private void sendMessage() {
        String text = etChatMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        etChatMessage.setText("");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        long now = System.currentTimeMillis();

        // Optimistically add to UI immediately
        ChatMessage localMsg = new ChatMessage(currentUid, currentSenderName, text, "", "TEXT", now);
        messageList.add(localMsg);
        adapter.notifyItemInserted(messageList.size() - 1);
        rvChatMessages.scrollToPosition(messageList.size() - 1);

        // 1. Add individual message item to subcollection
        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("senderId", currentUid);
        msgMap.put("senderName", currentSenderName);
        msgMap.put("senderRole", isUserAdmin ? "ADMIN" : "STUDENT");
        msgMap.put("text", text);
        msgMap.put("imageUrl", "");
        msgMap.put("messageType", "TEXT");
        msgMap.put("timestamp", now);

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(msgMap)
                .addOnFailureListener(e -> {
                    Toast.makeText(MessageActivity.this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        updateConversationMetadata(text, now, currentUser);
    }

    private void updateConversationMetadata(String lastMsgText, long timestamp, FirebaseUser currentUser) {
        Map<String, Object> chatMeta = new HashMap<>();
        chatMeta.put("chatId", chatId);
        chatMeta.put("lastMessage", lastMsgText);
        chatMeta.put("lastMessageTimestamp", timestamp);
        chatMeta.put("lastSenderId", currentUid);
        chatMeta.put("lastSenderName", currentSenderName);
        chatMeta.put("lastSenderRole", isUserAdmin ? "ADMIN" : "STUDENT");

        if (isUserAdmin) {
            chatMeta.put("adminUid", currentUid);
            chatMeta.put("studentUid", recipientUid);
            chatMeta.put("studentName", recipientName);
            if (recipientEmail != null && !recipientEmail.isEmpty()) {
                chatMeta.put("studentEmail", recipientEmail);
            }
        } else {
            chatMeta.put("studentUid", currentUid);
            chatMeta.put("studentName", currentSenderName);
            if (currentUser != null && currentUser.getEmail() != null) {
                chatMeta.put("studentEmail", currentUser.getEmail());
            }
            chatMeta.put("adminUid", recipientUid);
        }

        List<String> participants = new ArrayList<>();
        if (currentUid != null && !currentUid.isEmpty()) participants.add(currentUid);
        if (recipientUid != null && !recipientUid.isEmpty() && !participants.contains(recipientUid)) participants.add(recipientUid);
        if (!participants.contains("campus_admin_desk")) participants.add("campus_admin_desk");
        chatMeta.put("participants", participants);
        chatMeta.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("chats")
                .document(chatId)
                .set(chatMeta, SetOptions.merge());
    }

    private void resolveChatAndStartListening() {
        if (chatId != null && !chatId.trim().isEmpty()) {
            fetchChatMetadataAndListen();
            return;
        }

        if (isAdminReply) {
            // Admin replying to student: look up existing conversation for this student
            db.collection("chats")
                    .whereEqualTo("studentUid", recipientUid)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnap -> {
                        if (querySnap != null && !querySnap.isEmpty()) {
                            DocumentSnapshot doc = querySnap.getDocuments().get(0);
                            chatId = doc.getId();
                        } else {
                            chatId = (currentUid.compareTo(recipientUid) < 0)
                                    ? currentUid + "_" + recipientUid
                                    : recipientUid + "_" + currentUid;
                        }
                        listenToChatMessages();
                    })
                    .addOnFailureListener(e -> {
                        chatId = (currentUid.compareTo(recipientUid) < 0)
                                ? currentUid + "_" + recipientUid
                                : recipientUid + "_" + currentUid;
                        listenToChatMessages();
                    });
        } else {
            // Student reaching out: look up existing inquiry for current user
            db.collection("chats")
                    .whereEqualTo("studentUid", currentUid)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnap -> {
                        if (querySnap != null && !querySnap.isEmpty()) {
                            DocumentSnapshot doc = querySnap.getDocuments().get(0);
                            chatId = doc.getId();
                            String adminUid = doc.getString("adminUid");
                            if (adminUid != null && !adminUid.isEmpty()) {
                                recipientUid = adminUid;
                            }
                        } else {
                            chatId = (currentUid.compareTo(recipientUid) < 0)
                                    ? currentUid + "_" + recipientUid
                                    : recipientUid + "_" + currentUid;
                        }
                        listenToChatMessages();
                    })
                    .addOnFailureListener(e -> {
                        chatId = (currentUid.compareTo(recipientUid) < 0)
                                ? currentUid + "_" + recipientUid
                                : recipientUid + "_" + currentUid;
                        listenToChatMessages();
                    });
        }
    }

    private void fetchChatMetadataAndListen() {
        if (chatId == null || chatId.trim().isEmpty()) {
            listenToChatMessages();
            return;
        }

        db.collection("chats").document(chatId).get().addOnSuccessListener(doc -> {
            if (doc != null && doc.exists()) {
                String sUid = doc.getString("studentUid");
                String sName = doc.getString("studentName");
                String sEmail = doc.getString("studentEmail");
                String aUid = doc.getString("adminUid");

                if (isUserAdmin || isAdminReply) {
                    if (sUid != null && !sUid.isEmpty()) recipientUid = sUid;
                    if (sName != null && !sName.isEmpty()) {
                        recipientName = sName;
                        if (tvChatRecipientName != null) tvChatRecipientName.setText(recipientName);
                    }
                    if (sEmail != null && !sEmail.isEmpty()) recipientEmail = sEmail;
                } else {
                    if (aUid != null && !aUid.isEmpty()) recipientUid = aUid;
                }
            }
            listenToChatMessages();
        }).addOnFailureListener(e -> listenToChatMessages());
    }

    private com.google.firebase.firestore.ListenerRegistration chatListenerRegistration;

    private void listenToChatMessages() {
        if (chatId == null || chatId.trim().isEmpty()) {
            return;
        }

        if (chatListenerRegistration != null) {
            chatListenerRegistration.remove();
        }

        chatListenerRegistration = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (error != null) {
                        android.util.Log.e("MessageActivity", "Snapshot error: " + error.getMessage());
                        return;
                    }

                    if (snapshots != null) {
                        messageList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String senderId = doc.getString("senderId");
                            String senderName = doc.getString("senderName");
                            String text = doc.getString("text");
                            String imageUrl = doc.getString("imageUrl");
                            String messageType = doc.getString("messageType");
                            String senderRole = doc.getString("senderRole");

                            long timestamp = System.currentTimeMillis();
                            Object tsObj = doc.get("timestamp");
                            if (tsObj instanceof Long) {
                                timestamp = (Long) tsObj;
                            } else if (tsObj instanceof com.google.firebase.Timestamp) {
                                timestamp = ((com.google.firebase.Timestamp) tsObj).toDate().getTime();
                            } else if (tsObj instanceof Number) {
                                timestamp = ((Number) tsObj).longValue();
                            }

                            ChatMessage msg = new ChatMessage(
                                    senderId != null ? senderId : "",
                                    senderName != null ? senderName : "",
                                    text != null ? text : "",
                                    imageUrl != null ? imageUrl : "",
                                    messageType != null ? messageType : "TEXT",
                                    timestamp
                            );
                            if (senderRole != null) {
                                msg.setSenderRole(senderRole);
                            }
                            messageList.add(msg);
                        }

                        if (messageList.isEmpty()) {
                            // Defensive recovery: If messages subcollection is empty, check parent chat document
                            checkAndRecoverParentMessage();
                        } else {
                            // Sort chronologically (oldest to newest)
                            java.util.Collections.sort(messageList, (m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));

                            adapter.notifyDataSetChanged();
                            rvChatMessages.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    private void checkAndRecoverParentMessage() {
        if (chatId == null || chatId.trim().isEmpty()) return;

        db.collection("chats").document(chatId).get().addOnSuccessListener(doc -> {
            if (isFinishing() || isDestroyed()) return;
            if (doc != null && doc.exists() && messageList.isEmpty()) {
                String lastMsg = doc.getString("lastMessage");
                if (lastMsg != null && !lastMsg.trim().isEmpty() && !"New inquiry started".equalsIgnoreCase(lastMsg.trim())) {
                    String senderId = doc.getString("lastSenderId");
                    String senderName = doc.getString("lastSenderName");
                    String senderRole = doc.getString("lastSenderRole");
                    Long ts = doc.getLong("lastMessageTimestamp");
                    long time = (ts != null && ts > 0) ? ts : System.currentTimeMillis();

                    if (senderId == null || senderId.isEmpty()) {
                        senderId = doc.getString("studentUid");
                    }
                    if (senderName == null || senderName.isEmpty()) {
                        senderName = doc.getString("studentName");
                    }

                    ChatMessage recoveredMsg = new ChatMessage(
                            senderId != null ? senderId : "",
                            senderName != null ? senderName : "Student",
                            lastMsg,
                            "",
                            "TEXT",
                            time
                    );
                    if (senderRole != null) recoveredMsg.setSenderRole(senderRole);
                    messageList.add(recoveredMsg);
                    adapter.notifyDataSetChanged();
                    rvChatMessages.scrollToPosition(messageList.size() - 1);

                    // Formalize in subcollection for permanent persistence
                    Map<String, Object> syncMap = new HashMap<>();
                    syncMap.put("senderId", senderId != null ? senderId : "");
                    syncMap.put("senderName", senderName != null ? senderName : "Student");
                    syncMap.put("senderRole", senderRole != null ? senderRole : "STUDENT");
                    syncMap.put("text", lastMsg);
                    syncMap.put("imageUrl", "");
                    syncMap.put("messageType", "TEXT");
                    syncMap.put("timestamp", time);
                    db.collection("chats").document(chatId).collection("messages").add(syncMap);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ChatNotificationHelper.activeChatId = chatId;
        ChatNotificationHelper.markChatAsRead(this, chatId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (chatId != null && chatId.equals(ChatNotificationHelper.activeChatId)) {
            ChatNotificationHelper.activeChatId = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatId != null && chatId.equals(ChatNotificationHelper.activeChatId)) {
            ChatNotificationHelper.activeChatId = null;
        }
        if (chatListenerRegistration != null) {
            chatListenerRegistration.remove();
            chatListenerRegistration = null;
        }
        backgroundExecutor.shutdown();
    }
}
