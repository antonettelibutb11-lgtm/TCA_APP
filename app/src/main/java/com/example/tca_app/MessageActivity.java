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

        if (getIntent() != null) {
            if (getIntent().hasExtra("RECIPIENT_UID")) {
                recipientUid = getIntent().getStringExtra("RECIPIENT_UID");
            }
            if (getIntent().hasExtra("RECIPIENT_NAME")) {
                recipientName = getIntent().getStringExtra("RECIPIENT_NAME");
            }
            if (getIntent().hasExtra("RECIPIENT_EMAIL")) {
                recipientEmail = getIntent().getStringExtra("RECIPIENT_EMAIL");
            }
            isAdminReply = getIntent().getBooleanExtra("IS_ADMIN_REPLY", false);
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to send direct messages.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        currentUid = currentUser.getUid() != null ? currentUser.getUid() : "guest_user";

        // Deterministic chatId between the 2 parties
        if (currentUid.compareTo(recipientUid) < 0) {
            chatId = currentUid + "_" + recipientUid;
        } else {
            chatId = recipientUid + "_" + currentUid;
        }

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

            if (isUserAdmin) {
                // ADMIN VIEW: Talking to a student
                currentSenderName = "The Campus Access Editorial Desk";
                if (tvChatRecipientName != null) {
                    tvChatRecipientName.setText(recipientName != null ? recipientName : "Student Inquiry");
                }
                if (tvChatRecipientStatus != null) {
                    String statusText = "Student Inquiry";
                    if (recipientEmail != null && !recipientEmail.isEmpty()) {
                        statusText += " • " + recipientEmail;
                    } else {
                        statusText += " • BISU Balilihan";
                    }
                    tvChatRecipientStatus.setText(statusText);
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

        listenToChatMessages();
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
        chatMeta.put("participants", Arrays.asList(currentUid, recipientUid));
        chatMeta.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("chats")
                .document(chatId)
                .set(chatMeta, SetOptions.merge());
    }

    private com.google.firebase.firestore.ListenerRegistration chatListenerRegistration;

    private void listenToChatMessages() {
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
                            messageList.add(msg);
                        }

                        // Sort chronologically (oldest to newest)
                        java.util.Collections.sort(messageList, (m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));

                        adapter.notifyDataSetChanged();
                        if (!messageList.isEmpty()) {
                            rvChatMessages.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListenerRegistration != null) {
            chatListenerRegistration.remove();
            chatListenerRegistration = null;
        }
        backgroundExecutor.shutdown();
    }
}
