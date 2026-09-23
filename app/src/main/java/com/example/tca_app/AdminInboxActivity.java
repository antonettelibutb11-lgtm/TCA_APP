package com.example.tca_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

    private boolean showUnreadOnly = false;
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

        rvAdminInbox = findViewById(R.id.rvAdminInbox);
        rvAdminInbox.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminInboxAdapter(displayedConversationList, conversation -> {
            Intent intent = new Intent(AdminInboxActivity.this, MessageActivity.class);
            intent.putExtra("CHAT_ID", conversation.getChatId());
            intent.putExtra("RECIPIENT_UID", conversation.getStudentUid());
            intent.putExtra("RECIPIENT_NAME", conversation.getStudentName());
            intent.putExtra("RECIPIENT_EMAIL", conversation.getStudentEmail());
            intent.putExtra("IS_ADMIN_REPLY", true);
            startActivity(intent);
        });
        rvAdminInbox.setAdapter(adapter);

        setupSearchAndFilterControls();
        listenToStudentInquiries();
    }

    private void setupSearchAndFilterControls() {
        // Tab switching
        if (tabInboxAll != null) {
            tabInboxAll.setOnClickListener(v -> {
                showUnreadOnly = false;
                updateTabStyles();
                applyFilterAndSearch();
            });
        }

        if (tabInboxUnread != null) {
            tabInboxUnread.setOnClickListener(v -> {
                showUnreadOnly = true;
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
        if (tabInboxAll == null || tabInboxUnread == null) return;

        if (!showUnreadOnly) {
            tabInboxAll.setBackgroundResource(R.drawable.bg_purple_button);
            tabInboxAll.setTextColor(getResources().getColor(R.color.white, null));

            tabInboxUnread.setBackgroundResource(R.drawable.bg_chip_unselected);
            tabInboxUnread.setTextColor(getResources().getColor(R.color.text_secondary, null));
        } else {
            tabInboxUnread.setBackgroundResource(R.drawable.bg_purple_button);
            tabInboxUnread.setTextColor(getResources().getColor(R.color.white, null));

            tabInboxAll.setBackgroundResource(R.drawable.bg_chip_unselected);
            tabInboxAll.setTextColor(getResources().getColor(R.color.text_secondary, null));
        }
    }

    private void applyFilterAndSearch() {
        displayedConversationList.clear();

        for (ChatConversation conv : allConversationList) {
            // 1. Check unread filter
            if (showUnreadOnly && !conv.isUnread()) {
                continue;
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
        int totalCount = allConversationList.size();
        int unreadCount = 0;
        for (ChatConversation c : allConversationList) {
            if (c.isUnread()) unreadCount++;
        }

        if (tabInboxAll != null) {
            tabInboxAll.setText("All (" + totalCount + ")");
        }
        if (tabInboxUnread != null) {
            tabInboxUnread.setText("Unread (" + unreadCount + ")");
        }
        if (tvInquiryBadgeCount != null) {
            tvInquiryBadgeCount.setText(totalCount + (totalCount == 1 ? " Total" : " Total"));
        }

        if (layoutEmptyInbox != null) {
            if (displayedConversationList.isEmpty()) {
                layoutEmptyInbox.setVisibility(View.VISIBLE);
                if (!currentSearchQuery.isEmpty()) {
                    if (tvEmptyStateTitle != null) tvEmptyStateTitle.setText("No Matches Found");
                } else if (showUnreadOnly) {
                    if (tvEmptyStateTitle != null) tvEmptyStateTitle.setText("No Unread Messages");
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

        return new ChatConversation(
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
