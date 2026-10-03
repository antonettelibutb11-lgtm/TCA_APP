package com.example.tca_app;

import android.Manifest;
import android.content.Intent;
import android.content.res.ColorStateList;
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

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
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

    private LinearLayout layoutChatBlockedBanner;
    private TextView tvChatBlockedMessage;
    private TextView btnChatUnblock;
    private View layoutChatInputBar;
    private ImageView btnChatMoreOptions;

    private boolean isConversationArchived = false;
    private boolean isConversationBlocked = false;

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

        layoutChatBlockedBanner = findViewById(R.id.layoutChatBlockedBanner);
        tvChatBlockedMessage = findViewById(R.id.tvChatBlockedMessage);
        btnChatUnblock = findViewById(R.id.btnChatUnblock);
        layoutChatInputBar = findViewById(R.id.layoutChatInputBar);
        btnChatMoreOptions = findViewById(R.id.btnChatMoreOptions);

        if (btnBackChat != null) {
            btnBackChat.setOnClickListener(v -> finish());
        }

        if (btnChatMoreOptions != null) {
            btnChatMoreOptions.setOnClickListener(v -> showConversationActionsDialog());
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
        adapter.setOnMessageActionListener(new ChatMessageAdapter.OnMessageActionListener() {
            @Override
            public void onEditMessage(ChatMessage message, int position) {
                showEditMessageDialog(message);
            }

            @Override
            public void onUnsendMessage(ChatMessage message, int position) {
                showUnsendMessageDialog(message);
            }
        });
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

    private com.google.firebase.firestore.ListenerRegistration chatDocListenerRegistration;
    private com.google.firebase.firestore.ListenerRegistration chatListenerRegistration;

    private void fetchChatMetadataAndListen() {
        if (chatId == null || chatId.trim().isEmpty()) {
            listenToChatMessages();
            return;
        }

        if (chatDocListenerRegistration != null) {
            chatDocListenerRegistration.remove();
        }

        chatDocListenerRegistration = db.collection("chats").document(chatId).addSnapshotListener((doc, error) -> {
            if (isFinishing() || isDestroyed()) return;
            if (error != null) {
                android.util.Log.e("MessageActivity", "Chat doc snapshot error: " + error.getMessage());
                return;
            }

            if (doc != null && doc.exists()) {
                Boolean blocked = doc.getBoolean("isBlocked");
                isConversationBlocked = Boolean.TRUE.equals(blocked);

                Boolean archived = doc.getBoolean("isArchived");
                isConversationArchived = Boolean.TRUE.equals(archived);

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

                // Apply blocked conversation status
                if (isConversationBlocked) {
                    if (layoutChatBlockedBanner != null) layoutChatBlockedBanner.setVisibility(View.VISIBLE);
                    if (isUserAdmin) {
                        if (tvChatBlockedMessage != null) tvChatBlockedMessage.setText("You have blocked this student conversation.");
                        if (btnChatUnblock != null) {
                            btnChatUnblock.setVisibility(View.VISIBLE);
                            btnChatUnblock.setOnClickListener(v -> toggleBlockConversation(false));
                        }
                        if (layoutChatInputBar != null) layoutChatInputBar.setVisibility(View.VISIBLE);
                    } else {
                        if (tvChatBlockedMessage != null) tvChatBlockedMessage.setText("You cannot reply to this conversation as it is currently blocked.");
                        if (btnChatUnblock != null) btnChatUnblock.setVisibility(View.GONE);
                        if (layoutChatInputBar != null) layoutChatInputBar.setVisibility(View.GONE);
                    }
                } else {
                    if (layoutChatBlockedBanner != null) layoutChatBlockedBanner.setVisibility(View.GONE);
                    if (layoutChatInputBar != null) layoutChatInputBar.setVisibility(View.VISIBLE);
                }
            }
        });

        listenToChatMessages();
    }

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
                            Boolean isEdited = doc.getBoolean("isEdited");
                            Boolean isUnsent = doc.getBoolean("isUnsent");
                            Long editedAt = doc.getLong("editedAt");

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
                            msg.setMessageId(doc.getId());
                            if (isEdited != null) msg.setEdited(isEdited);
                            if (isUnsent != null) msg.setUnsent(isUnsent);
                            if (editedAt != null) msg.setEditedAt(editedAt);
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

    private void showEditMessageDialog(ChatMessage msg) {
        if (msg == null || msg.getMessageId().isEmpty()) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_chat_message, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etEditMessageText = dialogView.findViewById(R.id.etEditMessageText);
        Button btnCancelEditMessage = dialogView.findViewById(R.id.btnCancelEditMessage);
        Button btnSaveEditMessage = dialogView.findViewById(R.id.btnSaveEditMessage);

        if (etEditMessageText != null) {
            etEditMessageText.setText(msg.getText());
            if (msg.getText() != null) {
                etEditMessageText.setSelection(msg.getText().length());
            }
        }

        if (btnCancelEditMessage != null) {
            btnCancelEditMessage.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSaveEditMessage != null) {
            btnSaveEditMessage.setOnClickListener(v -> {
                String newText = etEditMessageText != null ? etEditMessageText.getText().toString().trim() : "";
                if (newText.isEmpty()) {
                    Toast.makeText(this, "Message cannot be empty.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newText.equals(msg.getText())) {
                    dialog.dismiss();
                    return;
                }

                dialog.dismiss();
                long now = System.currentTimeMillis();

                Map<String, Object> updates = new HashMap<>();
                updates.put("text", newText);
                updates.put("isEdited", true);
                updates.put("editedAt", now);

                db.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .document(msg.getMessageId())
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Message edited.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to edit message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

                // If this is the latest message, update the parent chat preview
                if (!messageList.isEmpty() && messageList.get(messageList.size() - 1).getMessageId().equals(msg.getMessageId())) {
                    db.collection("chats").document(chatId).update(
                            "lastMessage", newText,
                            "updatedAt", FieldValue.serverTimestamp()
                    );
                }
            });
        }

        dialog.show();
    }

    private void showUnsendMessageDialog(ChatMessage msg) {
        if (msg == null || msg.getMessageId().isEmpty()) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_unsend_chat_message, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnCancelUnsend = dialogView.findViewById(R.id.btnCancelUnsend);
        Button btnConfirmUnsend = dialogView.findViewById(R.id.btnConfirmUnsend);

        if (btnCancelUnsend != null) {
            btnCancelUnsend.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirmUnsend != null) {
            btnConfirmUnsend.setOnClickListener(v -> {
                dialog.dismiss();
                long now = System.currentTimeMillis();

                Map<String, Object> updates = new HashMap<>();
                updates.put("isUnsent", true);
                updates.put("text", "");
                updates.put("imageUrl", "");
                updates.put("unsentAt", now);

                db.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .document(msg.getMessageId())
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Message unsent.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to unsend message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

                // If this was the latest message, update parent
                if (!messageList.isEmpty() && messageList.get(messageList.size() - 1).getMessageId().equals(msg.getMessageId())) {
                    db.collection("chats").document(chatId).update(
                            "lastMessage", "A message was unsent",
                            "updatedAt", FieldValue.serverTimestamp()
                    );
                }
            });
        }

        dialog.show();
    }

    private void showConversationActionsDialog() {
        if (chatId == null || chatId.trim().isEmpty()) {
            Toast.makeText(this, "No active conversation to manage.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_conversation_actions, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvConvActionsTitle = dialogView.findViewById(R.id.tvConvActionsTitle);
        TextView tvConvActionsSubtitle = dialogView.findViewById(R.id.tvConvActionsSubtitle);
        View actionArchive = dialogView.findViewById(R.id.actionArchiveConversation);
        TextView tvArchiveLabel = dialogView.findViewById(R.id.tvArchiveLabel);
        TextView tvArchiveDesc = dialogView.findViewById(R.id.tvArchiveDesc);
        ImageView ivArchiveIcon = dialogView.findViewById(R.id.ivArchiveIcon);

        View actionBlock = dialogView.findViewById(R.id.actionBlockConversation);
        TextView tvBlockLabel = dialogView.findViewById(R.id.tvBlockLabel);
        TextView tvBlockDesc = dialogView.findViewById(R.id.tvBlockDesc);
        ImageView ivBlockIcon = dialogView.findViewById(R.id.ivBlockIcon);

        View actionDelete = dialogView.findViewById(R.id.actionDeleteConversation);
        View btnCancel = dialogView.findViewById(R.id.btnCancelConvActions);

        if (isUserAdmin) {
            if (tvConvActionsTitle != null) tvConvActionsTitle.setText("Conversation Options");
            if (tvConvActionsSubtitle != null) tvConvActionsSubtitle.setText("Student Inquiry • Conversation Management");
            if (tvArchiveLabel != null) {
                tvArchiveLabel.setText(isConversationArchived ? "Unarchive Conversation" : "Archive Conversation");
            }
            if (tvArchiveDesc != null) {
                tvArchiveDesc.setText(isConversationArchived ? "Restore this conversation back to active inbox" : "Move to archived folder to keep active desk clean");
            }
            if (ivArchiveIcon != null) {
                ivArchiveIcon.setImageResource(R.drawable.ic_archive);
            }

            if (tvBlockLabel != null) {
                tvBlockLabel.setText(isConversationBlocked ? "Unblock Student" : "Block Student");
            }
            if (tvBlockDesc != null) {
                tvBlockDesc.setText(isConversationBlocked ? "Allow this student to send messages again" : "Prevent this student from sending new messages");
            }
            if (ivBlockIcon != null) {
                if (isConversationBlocked) {
                    ivBlockIcon.setImageResource(R.drawable.ic_check_circle_purple);
                    ivBlockIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.green_success, null)));
                } else {
                    ivBlockIcon.setImageResource(R.drawable.ic_block);
                    ivBlockIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.text_secondary, null)));
                }
            }

            if (actionArchive != null) {
                actionArchive.setOnClickListener(v -> {
                    dialog.dismiss();
                    toggleArchiveConversation(!isConversationArchived);
                });
            }

            if (actionBlock != null) {
                actionBlock.setOnClickListener(v -> {
                    dialog.dismiss();
                    toggleBlockConversation(!isConversationBlocked);
                });
            }
        } else {
            // Student options
            if (tvConvActionsTitle != null) tvConvActionsTitle.setText("Chat Options");
            if (tvConvActionsSubtitle != null) tvConvActionsSubtitle.setText("Manage your conversation thread");
            if (actionBlock != null) actionBlock.setVisibility(View.GONE);
            if (actionArchive != null) {
                if (tvArchiveLabel != null) {
                    tvArchiveLabel.setText(isConversationArchived ? "Unarchive Chat" : "Archive Chat");
                }
                if (tvArchiveDesc != null) {
                    tvArchiveDesc.setText(isConversationArchived ? "Move back to your active message inbox" : "Hide from active inbox without losing history");
                }
                actionArchive.setOnClickListener(v -> {
                    dialog.dismiss();
                    toggleArchiveConversation(!isConversationArchived);
                });
            }
        }

        if (actionDelete != null) {
            actionDelete.setOnClickListener(v -> {
                dialog.dismiss();
                confirmDeleteConversation();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void toggleArchiveConversation(boolean archive) {
        if (chatId == null || chatId.trim().isEmpty()) return;
        db.collection("chats").document(chatId).update("isArchived", archive)
                .addOnSuccessListener(aVoid -> {
                    isConversationArchived = archive;
                    Toast.makeText(this, archive ? "Conversation moved to Archive." : "Conversation unarchived.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update archive status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void toggleBlockConversation(boolean block) {
        if (chatId == null || chatId.trim().isEmpty()) return;
        db.collection("chats").document(chatId).update("isBlocked", block)
                .addOnSuccessListener(aVoid -> {
                    isConversationBlocked = block;
                    Toast.makeText(this, block ? "Student has been blocked." : "Student has been unblocked.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update block status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void confirmDeleteConversation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Conversation")
                .setMessage("Are you sure you want to permanently delete this conversation? All messages will be removed.")
                .setPositiveButton("Delete", (dialog, which) -> deleteEntireConversation())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteEntireConversation() {
        if (chatId == null || chatId.trim().isEmpty()) return;
        Toast.makeText(this, "Deleting conversation...", Toast.LENGTH_SHORT).show();

        // Delete messages in subcollection
        db.collection("chats").document(chatId).collection("messages").get().addOnSuccessListener(snapshot -> {
            if (snapshot != null) {
                com.google.firebase.firestore.WriteBatch batch = db.batch();
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    batch.delete(doc.getReference());
                }
                batch.delete(db.collection("chats").document(chatId));
                batch.commit().addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Conversation deleted.", Toast.LENGTH_SHORT).show();
                    finish();
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).addOnFailureListener(e -> {
            db.collection("chats").document(chatId).delete().addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Conversation deleted.", Toast.LENGTH_SHORT).show();
                finish();
            });
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
        if (chatDocListenerRegistration != null) {
            chatDocListenerRegistration.remove();
            chatDocListenerRegistration = null;
        }
        if (chatListenerRegistration != null) {
            chatListenerRegistration.remove();
            chatListenerRegistration = null;
        }
        backgroundExecutor.shutdown();
    }
}
