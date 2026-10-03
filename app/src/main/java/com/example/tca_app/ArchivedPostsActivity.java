package com.example.tca_app;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for displaying, reviewing, and restoring archived posts.
 * Allows administrators and authors to bring back hidden posts to the live campus feed
 * or delete them permanently.
 */
public class ArchivedPostsActivity extends AppCompatActivity implements ArchivedPostAdapter.OnArchivedPostActionListener {

    private RecyclerView rvArchivedPosts;
    private SwipeRefreshLayout swipeRefreshArchived;
    private ProgressBar pbArchivedLoading;
    private LinearLayout layoutEmptyArchived;
    private TextView tvArchivedCountBadge;

    private ArchivedPostAdapter adapter;
    private final List<Post> archivedPostList = new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration postsListener;
    private boolean isAdminUser = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archived_posts);

        View rootView = findViewById(R.id.layoutArchivedRoot);
        if (rootView != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                androidx.core.graphics.Insets bars = insets.getInsets(
                        androidx.core.view.WindowInsetsCompat.Type.systemBars() |
                        androidx.core.view.WindowInsetsCompat.Type.displayCutout()
                );
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        db = FirebaseFirestore.getInstance();

        initViews();
        checkUserAccessAndLoad();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvArchivedCountBadge = findViewById(R.id.tvArchivedCountBadge);
        pbArchivedLoading = findViewById(R.id.pbArchivedLoading);
        layoutEmptyArchived = findViewById(R.id.layoutEmptyArchived);
        swipeRefreshArchived = findViewById(R.id.swipeRefreshArchived);
        rvArchivedPosts = findViewById(R.id.rvArchivedPosts);

        if (rvArchivedPosts != null) {
            rvArchivedPosts.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ArchivedPostAdapter(this, archivedPostList, this);
            rvArchivedPosts.setAdapter(adapter);
        }

        if (swipeRefreshArchived != null) {
            swipeRefreshArchived.setColorSchemeResources(R.color.purple_primary);
            swipeRefreshArchived.setOnRefreshListener(this::loadArchivedPosts);
        }
    }

    private void checkUserAccessAndLoad() {
        AuthUtils.checkCurrentUserAccess((isApprovedMember, isAdmin, role) -> {
            if (isFinishing() || isDestroyed()) return;
            this.isAdminUser = isAdmin;
            loadArchivedPosts();
        });
    }

    private void loadArchivedPosts() {
        if (postsListener != null) {
            postsListener.remove();
        }

        if (pbArchivedLoading != null && !swipeRefreshArchived.isRefreshing()) {
            pbArchivedLoading.setVisibility(View.VISIBLE);
        }

        long now = System.currentTimeMillis();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = currentUser != null ? currentUser.getUid() : "";

        // Query posts and filter for ARCHIVED status
        Query query = db.collection("posts").orderBy("timestamp", Query.Direction.DESCENDING).limit(100);

        postsListener = query.addSnapshotListener((snapshots, error) -> {
            if (isFinishing() || isDestroyed()) return;

            if (pbArchivedLoading != null) {
                pbArchivedLoading.setVisibility(View.GONE);
            }
            if (swipeRefreshArchived != null && swipeRefreshArchived.isRefreshing()) {
                swipeRefreshArchived.setRefreshing(false);
            }

            if (error != null) {
                Toast.makeText(ArchivedPostsActivity.this, "Error loading archived posts: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            archivedPostList.clear();

            if (snapshots != null && !snapshots.isEmpty()) {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    String moderationStatus = doc.getString("moderationStatus");
                    String status = doc.getString("status");

                    boolean isArchived = "ARCHIVED".equalsIgnoreCase(moderationStatus) || "ARCHIVED".equalsIgnoreCase(status);
                    if (!isArchived) continue;

                    Post post = PostRepository.documentToPost(doc, now);
                    if (post != null) {
                        // Admins see all archived posts; students see their own archived posts
                        if (isAdminUser || (post.getAuthorUid() != null && post.getAuthorUid().equals(currentUid))) {
                            archivedPostList.add(post);
                        }
                    }
                }
            }

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

            updateEmptyState();
        });
    }

    private void updateEmptyState() {
        int count = archivedPostList.size();
        if (tvArchivedCountBadge != null) {
            tvArchivedCountBadge.setText(count + (count == 1 ? " post" : " posts"));
        }

        if (layoutEmptyArchived != null && rvArchivedPosts != null) {
            if (count == 0) {
                layoutEmptyArchived.setVisibility(View.VISIBLE);
                rvArchivedPosts.setVisibility(View.GONE);
            } else {
                layoutEmptyArchived.setVisibility(View.GONE);
                rvArchivedPosts.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onRestorePost(Post post, int position) {
        if (post == null || post.getId() == null || post.getId().isEmpty()) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Restore Post")
                .setMessage("Restore this post back to the Campus Feed? Students and members will be able to see it again.")
                .setPositiveButton("Restore to Feed", (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("moderationStatus", "APPROVED");
                    updates.put("status", "APPROVED");

                    db.collection("posts").document(post.getId())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(ArchivedPostsActivity.this, "Post restored to campus feed!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(ArchivedPostsActivity.this, "Failed to restore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDeletePost(Post post, int position) {
        if (post == null || post.getId() == null || post.getId().isEmpty()) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Permanently Delete")
                .setMessage("Are you sure you want to permanently delete this post? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("posts").document(post.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(ArchivedPostsActivity.this, "Post permanently deleted.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(ArchivedPostsActivity.this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (postsListener != null) {
            postsListener.remove();
            postsListener = null;
        }
    }
}
