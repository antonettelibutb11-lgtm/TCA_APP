package com.example.tca_app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminInboxActivity extends AppCompatActivity {

    private static final String TAG = "AdminInboxActivity";

    private static final int TAB_ALL = 0;
    private static final int TAB_UNREAD = 1;
    private static final int TAB_ARCHIVED = 2;

    private RecyclerView rvAdminInbox;
    private AdminInboxAdapter adapter;

    private final List<ChatConversation> allConversationList = new ArrayList<>();
    private final List<ChatConversation> displayedConversationList = new ArrayList<>();

    private LinearLayout layoutEmptyInbox;
    private ImageView ivEmptyStateIcon;
    private TextView tvEmptyStateTitle;

    private ProgressBar pbInboxLoading;
    private TextView tvInquiryBadgeCount;
    private EditText etSearchInbox;
    private ImageView ivClearSearch;
    private TextView tabInboxAll;
    private TextView tabInboxUnread;
    private TextView tabInboxArchived;

    private int currentFilterTab = TAB_ALL;
    private String currentSearchQuery = "";

    private FirebaseFirestore db;
    private final List<ListenerRegistration> activeListeners = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_inbox);

        db = FirebaseFirestore.getInstance();

        ImageView btnBackInbox = findViewById(R.id.btnBackInbox);
        if (btnBackInbox != null) {
            btnBackInbox.setOnClickListener(v -> finish());
        }

        tvInquiryBadgeCount = findViewById(R.id.tvInquiryBadgeCount);
        layoutEmptyInbox = findViewById(R.id.layoutEmptyInbox);
        ivEmptyStateIcon = findViewById(R.id.ivEmptyStateIcon);
        tvEmptyStateTitle = findViewById(R.id.tvEmptyStateTitle);

        pbInboxLoading = findViewById(R.id.pbInboxLoading);
        etSearchInbox = findViewById(R.id.etSearchInbox);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        tabInboxAll = findViewById(R.id.tabInboxAll);
        tabInboxUnread = findViewById(R.id.tabInboxUnread);
        tabInboxArchived = findViewById(R.id.tabInboxArchived);

        rvAdminInbox = findViewById(R.id.rvAdminInbox);
        rvAdminInbox.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminInboxAdapter(displayedConversationList, new AdminInboxAdapter.OnConversationActionListener() {
            @Override
            public void onConversationClick(ChatConversation conversation) {
                Intent intent = new Intent(AdminInboxActivity.this, MessageActivity.class);
                intent.putExtra("CHAT_ID", conversation.getChatId());
                intent.putExtra("RECIPIENT_UID", conversation.getStudentUid());
                intent.putExtra("RECIPIENT_NAME", conversation.getStudentName());
                intent.putExtra("RECIPIENT_EMAIL", conversation.getStudentEmail());
                intent.putExtra("IS_ADMIN_REPLY", true);
                startActivity(intent);
            }

            @Override
            public void onConversationOptionsClick(ChatConversation conversation, int position, View anchorView) {
                showConversationActionModal(conversation);
            }
        });
        rvAdminInbox.setAdapter(adapter);

        setupSearchAndFilterControls();
        listenToStudentInquiries();
    }

    private void setupSearchAndFilterControls() {
        // Tab switching
        if (tabInboxAll != null) {
            tabInboxAll.setOnClickListener(v -> {
                currentFilterTab = TAB_ALL;
                updateTabStyles();
                applyFilterAndSearch();
            });
        }

        if (tabInboxUnread != null) {
            tabInboxUnread.setOnClickListener(v -> {
                currentFilterTab = TAB_UNREAD;
                updateTabStyles();
                applyFilterAndSearch();
            });
        }

        if (tabInboxArchived != null) {
            tabInboxArchived.setOnClickListener(v -> {
                currentFilterTab = TAB_ARCHIVED;
                updateTabStyles();
                applyFilterAndSearch();
            });
        }

        // Live Search
        if (etSearchInbox != null) {
            etSearchInbox.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s != null ? s.toString().trim().toLowerCase(Locale.getDefault()) : "";
                    if (ivClearSearch != null) {
                        ivClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    applyFilterAndSearch();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (ivClearSearch != null) {
            ivClearSearch.setOnClickListener(v -> {
                if (etSearchInbox != null) {
                    etSearchInbox.setText("");
                }
            });
        }
    }

    private void updateTabStyles() {
        if (tabInboxAll == null || tabInboxUnread == null || tabInboxArchived == null) return;

        tabInboxAll.setBackgroundResource(currentFilterTab == TAB_ALL ? R.drawable.bg_purple_button : R.drawable.bg_chip_unselected);
        tabInboxAll.setTextColor(getResources().getColor(currentFilterTab == TAB_ALL ? R.color.white : R.color.text_secondary, null));

        tabInboxUnread.setBackgroundResource(currentFilterTab == TAB_UNREAD ? R.drawable.bg_purple_button : R.drawable.bg_chip_unselected);
        tabInboxUnread.setTextColor(getResources().getColor(currentFilterTab == TAB_UNREAD ? R.color.white : R.color.text_secondary, null));

        tabInboxArchived.setBackgroundResource(currentFilterTab == TAB_ARCHIVED ? R.drawable.bg_purple_button : R.drawable.bg_chip_unselected);
        tabInboxArchived.setTextColor(getResources().getColor(currentFilterTab == TAB_ARCHIVED ? R.color.white : R.color.text_secondary, null));
    }

    private void applyFilterAndSearch() {
        displayedConversationList.clear();

        for (ChatConversation conv : allConversationList) {
            // 1. Check tab filter
            if (currentFilterTab == TAB_ARCHIVED) {
                if (!conv.isArchived()) continue;
            } else if (currentFilterTab == TAB_UNREAD) {
                if (conv.isArchived() || !conv.isUnread()) continue;
            } else {
                // TAB_ALL: Non-archived conversations
                if (conv.isArchived()) continue;
            }

            // 2. Check search query
            if (!currentSearchQuery.isEmpty()) {
                String name = conv.getStudentName().toLowerCase(Locale.getDefault());
                String email = conv.getStudentEmail().toLowerCase(Locale.getDefault());
                String msg = conv.getLastMessage().toLowerCase(Locale.getDefault());

                if (!name.contains(currentSearchQuery) && !email.contains(currentSearchQuery) && !msg.contains(currentSearchQuery)) {
                    continue;
                }
            }

            displayedConversationList.add(conv);
        }

        adapter.notifyDataSetChanged();
        updateCountersAndEmptyState();
    }

    private void updateCountersAndEmptyState() {
        int allCount = 0;
        int unreadCount = 0;
        int archivedCount = 0;

        for (ChatConversation c : allConversationList) {
            if (c.isArchived()) {
                archivedCount++;
            } else {
                allCount++;
                if (c.isUnread()) unreadCount++;
            }
        }

        if (tabInboxAll != null) {
            tabInboxAll.setText("All (" + allCount + ")");
        }
        if (tabInboxUnread != null) {
            tabInboxUnread.setText("Unread (" + unreadCount + ")");
        }
        if (tabInboxArchived != null) {
            tabInboxArchived.setText("Archived (" + archivedCount + ")");
        }
        if (tvInquiryBadgeCount != null) {
            tvInquiryBadgeCount.setText(allCount + (allCount == 1 ? " Active" : " Active"));
        }

        if (layoutEmptyInbox != null) {
            if (displayedConversationList.isEmpty()) {
                layoutEmptyInbox.setVisibility(View.VISIBLE);
                if (!currentSearchQuery.isEmpty()) {
                    if (tvEmptyStateTitle != null) tvEmptyStateTitle.setText("No Matches Found");
                } else if (currentFilterTab == TAB_UNREAD) {
                    if (tvEmptyStateTitle != null) tvEmptyStateTitle.setText("No Unread Messages");
                } else if (currentFilterTab == TAB_ARCHIVED) {
                    if (tvEmptyStateTitle != null) tvEmptyStateTitle.setText("No Archived Inquiries");
                } else {
                    if (tvEmptyStateTitle != null) tvEmptyStateTitle.setText("No Student Inquiries Yet");
                }
            } else {
                layoutEmptyInbox.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Listens to student inquiries with Firestore security rule resilience.
     */
    private void listenToStudentInquiries() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to view inbox.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String currentUid = currentUser.getUid();
        pbInboxLoading.setVisibility(View.VISIBLE);

        final Map<String, ChatConversation> conversationMap = new HashMap<>();

        // Strategy 1: Query chats where current user is in participants
        ListenerRegistration pListener = db.collection("chats")
                .whereArrayContains("participants", currentUid)
                .addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    pbInboxLoading.setVisibility(View.GONE);

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            ChatConversation conv = parseChatDocument(doc, currentUid);
                            if (conv != null) {
                                conversationMap.put(conv.getChatId(), conv);
                            }
                        }
                        updateConversationsFromMap(conversationMap);
                    }
                });
        activeListeners.add(pListener);

        // Strategy 2: Query chats where adminUid == currentUid
        ListenerRegistration aListener = db.collection("chats")
                .whereEqualTo("adminUid", currentUid)
                .addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    pbInboxLoading.setVisibility(View.GONE);

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            ChatConversation conv = parseChatDocument(doc, currentUid);
                            if (conv != null) {
                                conversationMap.put(conv.getChatId(), conv);
                            }
                        }
                        updateConversationsFromMap(conversationMap);
                    }
                });
        activeListeners.add(aListener);

        // Strategy 3: Query generic desk fallback adminUid == "campus_admin_desk"
        ListenerRegistration dListener = db.collection("chats")
                .whereEqualTo("adminUid", "campus_admin_desk")
                .addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    pbInboxLoading.setVisibility(View.GONE);

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            ChatConversation conv = parseChatDocument(doc, currentUid);
                            if (conv != null) {
                                conversationMap.put(conv.getChatId(), conv);
                            }
                        }
                        updateConversationsFromMap(conversationMap);
                    }
                });
        activeListeners.add(dListener);

        // Strategy 4: Fallback entire collection query for wide admin access (wrapped safely)
        ListenerRegistration allListener = db.collection("chats")
                .addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    pbInboxLoading.setVisibility(View.GONE);

                    if (error != null) {
                        Log.d(TAG, "Global chats query restricted by security rules. Relying on participant queries: " + error.getMessage());
                        // Do not show intrusive toast — Strategy 1, 2, & 3 seamlessly populate conversations
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            ChatConversation conv = parseChatDocument(doc, currentUid);
                            if (conv != null) {
                                conversationMap.put(conv.getChatId(), conv);
                            }
                        }
                        updateConversationsFromMap(conversationMap);
                    }
                });
        activeListeners.add(allListener);
    }

    private ChatConversation parseChatDocument(DocumentSnapshot doc, String currentUid) {
        String chatId = doc.getId();
        String studentUid = doc.getString("studentUid");
        String studentName = doc.getString("studentName");
        String studentEmail = doc.getString("studentEmail");
        String adminUid = doc.getString("adminUid");
        String lastMessage = doc.getString("lastMessage");
        Long lastTimestamp = doc.getLong("lastMessageTimestamp");
        String lastSenderId = doc.getString("lastSenderId");
        String lastSenderName = doc.getString("lastSenderName");
        String lastSenderRole = doc.getString("lastSenderRole");

        if (studentUid == null || studentUid.isEmpty()) {
            String[] parts = chatId.split("_");
            if (parts.length == 2) {
                studentUid = parts[0].equals(currentUid) ? parts[1] : parts[0];
            }
        }

        if (studentName == null || studentName.isEmpty()) {
            studentName = lastSenderName != null ? lastSenderName : "BISU Student";
        }

        long ts = lastTimestamp != null ? lastTimestamp : 0;
        if (ts == 0 && doc.getTimestamp("updatedAt") != null) {
            ts = doc.getTimestamp("updatedAt").toDate().getTime();
        }

        ChatConversation conv = new ChatConversation(
                chatId,
                studentUid,
                studentName,
                studentEmail != null ? studentEmail : "",
                adminUid != null ? adminUid : currentUid,
                lastMessage != null ? lastMessage : "New inquiry started",
                ts,
                lastSenderId != null ? lastSenderId : "",
                lastSenderName != null ? lastSenderName : studentName,
                lastSenderRole != null ? lastSenderRole : "STUDENT"
        );
        Boolean isArchived = doc.getBoolean("isArchived");
        conv.setArchived(Boolean.TRUE.equals(isArchived));

        Boolean isBlocked = doc.getBoolean("isBlocked");
        conv.setBlocked(Boolean.TRUE.equals(isBlocked));

        return conv;
    }

    private void showConversationActionModal(ChatConversation conversation) {
        if (conversation == null || conversation.getChatId().isEmpty()) return;

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

        if (tvConvActionsTitle != null) {
            tvConvActionsTitle.setText(conversation.getStudentName());
        }
        if (tvConvActionsSubtitle != null) {
            tvConvActionsSubtitle.setText("Student Inquiry • Conversation Management");
        }

        boolean isArchived = conversation.isArchived();
        if (tvArchiveLabel != null) {
            tvArchiveLabel.setText(isArchived ? "Unarchive Conversation" : "Archive Conversation");
        }
        if (tvArchiveDesc != null) {
            tvArchiveDesc.setText(isArchived ? "Restore this conversation back to active inbox" : "Move to archived folder to keep active desk clean");
        }
        if (ivArchiveIcon != null) {
            ivArchiveIcon.setImageResource(R.drawable.ic_archive);
        }

        boolean isBlocked = conversation.isBlocked();
        if (tvBlockLabel != null) {
            tvBlockLabel.setText(isBlocked ? "Unblock Student" : "Block Student");
        }
        if (tvBlockDesc != null) {
            tvBlockDesc.setText(isBlocked ? "Allow this student to send messages again" : "Prevent this student from sending new messages");
        }
        if (ivBlockIcon != null) {
            if (isBlocked) {
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
                toggleArchiveConversation(conversation, !isArchived);
            });
        }

        if (actionBlock != null) {
            actionBlock.setOnClickListener(v -> {
                dialog.dismiss();
                toggleBlockConversation(conversation, !isBlocked);
            });
        }

        if (actionDelete != null) {
            actionDelete.setOnClickListener(v -> {
                dialog.dismiss();
                confirmDeleteConversation(conversation);
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void toggleArchiveConversation(ChatConversation conv, boolean archive) {
        db.collection("chats").document(conv.getChatId()).update("isArchived", archive)
                .addOnSuccessListener(aVoid -> {
                    conv.setArchived(archive);
                    Toast.makeText(this, archive ? "Conversation moved to Archive." : "Conversation unarchived.", Toast.LENGTH_SHORT).show();
                    applyFilterAndSearch();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update archive status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void toggleBlockConversation(ChatConversation conv, boolean block) {
        db.collection("chats").document(conv.getChatId()).update("isBlocked", block)
                .addOnSuccessListener(aVoid -> {
                    conv.setBlocked(block);
                    Toast.makeText(this, block ? "Student has been blocked." : "Student has been unblocked.", Toast.LENGTH_SHORT).show();
                    applyFilterAndSearch();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update block status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void confirmDeleteConversation(ChatConversation conv) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Conversation")
                .setMessage("Are you sure you want to permanently delete the conversation with " + conv.getStudentName() + "? All messages will be removed.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Toast.makeText(this, "Deleting conversation...", Toast.LENGTH_SHORT).show();
                    db.collection("chats").document(conv.getChatId()).collection("messages").get().addOnSuccessListener(snapshot -> {
                        if (snapshot != null) {
                            com.google.firebase.firestore.WriteBatch batch = db.batch();
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                batch.delete(doc.getReference());
                            }
                            batch.delete(db.collection("chats").document(conv.getChatId()));
                            batch.commit().addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Conversation deleted.", Toast.LENGTH_SHORT).show();
                                allConversationList.remove(conv);
                                applyFilterAndSearch();
                            }).addOnFailureListener(e -> {
                                Toast.makeText(this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).addOnFailureListener(e -> {
                        db.collection("chats").document(conv.getChatId()).delete().addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Conversation deleted.", Toast.LENGTH_SHORT).show();
                            allConversationList.remove(conv);
                            applyFilterAndSearch();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateConversationsFromMap(Map<String, ChatConversation> map) {
        allConversationList.clear();
        allConversationList.addAll(map.values());

        // Sort newest first
        Collections.sort(allConversationList, (a, b) -> Long.compare(b.getLastMessageTimestamp(), a.getLastMessageTimestamp()));

        applyFilterAndSearch();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (ListenerRegistration listener : activeListeners) {
            if (listener != null) listener.remove();
        }
        activeListeners.clear();
    }
}
