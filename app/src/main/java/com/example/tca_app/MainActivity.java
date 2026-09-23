package com.example.tca_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private TextView tvHeaderTitle;
    private boolean isAdmin = false;
    private View topBar;
    private FrameLayout btnTopBarChat;
    private TextView tvTopBarUnreadBadge;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    ChatNotificationHelper.startListening(this);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        topBar = findViewById(R.id.topBar);
        btnTopBarChat = findViewById(R.id.btnTopBarChat);
        tvTopBarUnreadBadge = findViewById(R.id.tvTopBarUnreadBadge);

        if (btnTopBarChat != null) {
            btnTopBarChat.setOnClickListener(v -> handleTopBarChatClick());
        }

        if (topBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + 12, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        CurvedBottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        // Initially hide Analytics tab — will be shown only after access check
        if (bottomNavigation != null) {
            bottomNavigation.setTabVisibility(2, false);
        }

        // ANALYTICS TAB VISIBILITY RULES:
        //   Admin                  → VISIBLE
        //   Approved Campus Member (isMember = true, accepted by admin) → VISIBLE
        //   Normal Student         → HIDDEN
        AuthUtils.checkCurrentUserAccess((isApprovedMember, isAdminUser, role) -> {
            isAdmin = isAdminUser;
            boolean canSeeAnalytics = isAdminUser || isApprovedMember;
            if (bottomNavigation != null) {
                bottomNavigation.setTabVisibility(2, canSeeAnalytics);
            }
        });

        // Load Default Fragment (Home Feed)
        loadFragment(new HomeFeedFragment(), getString(R.string.app_name));

        if (bottomNavigation != null) {
            bottomNavigation.setOnTabSelectedListener(position -> {
                switch (position) {
                    case 0: // Home
                        if (topBar != null) topBar.setVisibility(View.VISIBLE);
                        loadFragment(new HomeFeedFragment(), getString(R.string.app_name));
                        break;
                    case 1: // Campus Event
                        if (topBar != null) topBar.setVisibility(View.VISIBLE);
                        loadFragment(new EventCalendarFragment(), getString(R.string.title_campus_events));
                        break;
                    case 2: // Analytics / Dashboard — Admin & Approved Members only
                        if (topBar != null) topBar.setVisibility(View.VISIBLE);
                        // Double-check access even if tab is somehow tapped
                        AuthUtils.checkCurrentUserAccess((isApprovedMember, isAdminUser, role) -> {
                            if (isAdminUser || isApprovedMember) {
                                loadFragment(new AdminDashboardFragment(), getString(R.string.title_analytics_dashboard));
                            } else {
                                // Unauthorized student — redirect back to Home
                                loadFragment(new HomeFeedFragment(), getString(R.string.app_name));
                                android.widget.Toast.makeText(this,
                                        "Analytics is only available to admin and approved campus members.",
                                        android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case 3: // Profile
                        if (topBar != null) topBar.setVisibility(View.GONE);
                        loadFragment(new ProfileFragment(), getString(R.string.app_name));
                        break;
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ChatNotificationHelper.startListening(this);
        ChatNotificationHelper.setUnreadListener(count -> runOnUiThread(() -> {
            if (tvTopBarUnreadBadge != null) {
                if (count > 0) {
                    tvTopBarUnreadBadge.setVisibility(View.VISIBLE);
                    tvTopBarUnreadBadge.setText(count > 9 ? "9+" : String.valueOf(count));
                } else {
                    tvTopBarUnreadBadge.setVisibility(View.GONE);
                }
            }
        }));
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void handleTopBarChatClick() {
        checkNotificationPermission();
        AuthUtils.checkCurrentUserAccess((isApprovedMember, isAdminUser, role) -> {
            if (isFinishing() || isDestroyed()) return;
            if (isAdminUser) {
                startActivity(new Intent(MainActivity.this, AdminInboxActivity.class));
            } else {
                openStudentMessaging();
            }
        });
    }

    private void openStudentMessaging() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance().collection("chats")
                    .whereEqualTo("studentUid", user.getUid())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnap -> {
                        if (querySnap != null && !querySnap.isEmpty()) {
                            DocumentSnapshot doc = querySnap.getDocuments().get(0);
                            String existingChatId = doc.getId();
                            String adminUid = doc.getString("adminUid");
                            Intent intent = new Intent(MainActivity.this, MessageActivity.class);
                            intent.putExtra("CHAT_ID", existingChatId);
                            intent.putExtra("RECIPIENT_UID", adminUid != null ? adminUid : "campus_admin_desk");
                            intent.putExtra("RECIPIENT_NAME", "The Campus Access Editorial Desk");
                            startActivity(intent);
                        } else {
                            fallbackOpenStudentMessaging();
                        }
                    })
                    .addOnFailureListener(e -> fallbackOpenStudentMessaging());
            return;
        }
        fallbackOpenStudentMessaging();
    }

    private void fallbackOpenStudentMessaging() {
        FirebaseFirestore.getInstance().collection("system_config").document("admin_contact").get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists() && snapshot.getString("adminUid") != null) {
                        launchMessageActivity(snapshot.getString("adminUid"),
                                snapshot.getString("adminName") != null ? snapshot.getString("adminName") : "The Campus Access");
                    } else {
                        FirebaseFirestore.getInstance().collection("users")
                                .whereEqualTo("role", "ADMIN")
                                .limit(1)
                                .get()
                                .addOnSuccessListener(querySnap -> {
                                    if (querySnap != null && !querySnap.isEmpty()) {
                                        DocumentSnapshot adminDoc = querySnap.getDocuments().get(0);
                                        launchMessageActivity(adminDoc.getId(),
                                                adminDoc.getString("name") != null ? adminDoc.getString("name") : "The Campus Access Editorial Desk");
                                    } else {
                                        launchMessageActivity("campus_admin_desk", "The Campus Access Editorial Desk");
                                    }
                                })
                                .addOnFailureListener(e -> launchMessageActivity("campus_admin_desk", "The Campus Access Editorial Desk"));
                    }
                })
                .addOnFailureListener(e -> launchMessageActivity("campus_admin_desk", "The Campus Access Editorial Desk"));
    }

    private void launchMessageActivity(String recipientUid, String recipientName) {
        Intent intent = new Intent(MainActivity.this, MessageActivity.class);
        intent.putExtra("RECIPIENT_UID", recipientUid);
        intent.putExtra("RECIPIENT_NAME", recipientName);
        startActivity(intent);
    }

    private void loadFragment(Fragment fragment, String title) {
        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText(title);
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragmentContainer);
        if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
            return;
        }
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    public void navigateToTab(int position) {
        CurvedBottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.selectTab(position, true);
        }
    }
}