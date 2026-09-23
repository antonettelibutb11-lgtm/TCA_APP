package com.example.tca_app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

public class ProfileFragment extends Fragment {

    private TextView tabAll, tabGallery, tabVideos;
    private RecyclerView rvProfilePosts;
    private androidx.core.widget.NestedScrollView scrollViewProfile;
    private PostAdapter adapter;
    private GridMediaAdapter gridAdapter;
    private List<Post> profilePostsList;

    private TextView tvEmptyStateProfile;
    private ListenerRegistration profileListenerRegistration;
    private android.widget.Spinner spinnerGalleryFolder;
    private List<Post> allCategoryPosts = new ArrayList<>();
    private String currentSelectedFolder = "All Folders";

    private ImageView imgProfileLogo;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private String currentProfileImageUrl = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        scrollViewProfile = view.findViewById(R.id.scrollViewProfile);
        if (scrollViewProfile != null) {
            scrollViewProfile.post(() -> scrollViewProfile.scrollTo(0, 0));
            scrollViewProfile.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener)
                    (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                        if (rvProfilePosts != null) {
                            VideoScrollHelper.handleVideoVisibility(rvProfilePosts);
                        }
                    });
        }

        imgProfileLogo = view.findViewById(R.id.imgProfileLogo);

        // Fetch and display profile picture on load
        loadProfilePicture();

        // Setup image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            uploadProfilePicture(selectedImageUri);
                        }
                    }
                }
        );

        if (imgProfileLogo != null) {
            imgProfileLogo.setOnClickListener(v -> {
                com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
                        new com.google.android.material.bottomsheet.BottomSheetDialog(v.getContext());
                View sheetView = LayoutInflater.from(v.getContext()).inflate(R.layout.layout_profile_picture_menu, null);
                bottomSheetDialog.setContentView(sheetView);

                sheetView.findViewById(R.id.btnSeeProfilePicture).setOnClickListener(btn -> {
                    bottomSheetDialog.dismiss();
                    Intent intent = new Intent(v.getContext(), FullScreenImageActivity.class);
                    intent.putExtra("photoUri", currentProfileImageUrl != null ? currentProfileImageUrl : "default_logo");
                    startActivity(intent);
                });

                sheetView.findViewById(R.id.btnChooseProfilePicture).setOnClickListener(btn -> {
                    bottomSheetDialog.dismiss();
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    imagePickerLauncher.launch(intent);
                });

                bottomSheetDialog.show();
            });
        }

        TextView btnProfileMenu = view.findViewById(R.id.btnProfileMenu);

        // Bind Profile Category Navigation Tabs
        tabAll = view.findViewById(R.id.tabAll);
        tabGallery = view.findViewById(R.id.tabGallery);
        tabVideos = view.findViewById(R.id.tabVideos);

        tvEmptyStateProfile = view.findViewById(R.id.tvEmptyStateProfile);

        spinnerGalleryFolder = view.findViewById(R.id.spinnerGalleryFolder);
        if (spinnerGalleryFolder != null) {
            spinnerGalleryFolder.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    currentSelectedFolder = parent.getItemAtPosition(position).toString();
                    filterAndDisplayPosts();
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        rvProfilePosts = view.findViewById(R.id.rvProfilePosts);
        if (rvProfilePosts != null) {
            rvProfilePosts.setLayoutManager(new LinearLayoutManager(getContext()));
            rvProfilePosts.setNestedScrollingEnabled(false);
            profilePostsList = new ArrayList<>();
            adapter = new PostAdapter(profilePostsList);
            rvProfilePosts.setAdapter(adapter);
            VideoScrollHelper.attachToRecyclerView(rvProfilePosts);
            adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    rvProfilePosts.post(() -> VideoScrollHelper.handleVideoVisibility(rvProfilePosts));
                }
            });
        }

        if (btnProfileMenu != null) {
            btnProfileMenu.setOnClickListener(v -> showCreativeLogoutDialog());
        }

        TextView btnMessageUser = view.findViewById(R.id.btnMessageUser);
        if (btnMessageUser != null) {
            AuthUtils.checkCurrentUserAccess((isApprovedMember, isAdmin, role) -> {
                if (!isAdded() || getContext() == null) return;
                if (isAdmin) {
                    btnMessageUser.setText("Student Inquiries");
                    btnMessageUser.setOnClickListener(v -> {
                        if (getContext() != null) {
                            startActivity(new Intent(getContext(), AdminInboxActivity.class));
                        }
                    });
                } else {
                    btnMessageUser.setText("Message");
                    btnMessageUser.setOnClickListener(v -> openAdminDirectMessaging());
                }
            });
        }

        if (tabAll != null) tabAll.setOnClickListener(v -> selectTab("ALL"));
        if (tabGallery != null) tabGallery.setOnClickListener(v -> selectTab("GALLERY"));
        if (tabVideos != null) tabVideos.setOnClickListener(v -> selectTab("VIDEOS"));

        // Default tab selection
        selectTab("ALL");

        return view;
    }

    /**
     * Dynamically fetches the Admin/Editor-in-Chief UID from Firestore config
     * instead of relying on a hardcoded string.
     */
    private void openAdminDirectMessaging() {
        if (getContext() == null) return;

        // Double-check if user is Admin: open Admin Inbox directly
        AuthUtils.checkCurrentUserAccess((isApprovedMember, isAdmin, role) -> {
            if (!isAdded() || getContext() == null) return;
            if (isAdmin) {
                startActivity(new Intent(getContext(), AdminInboxActivity.class));
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null) {
                db.collection("chats")
                        .whereEqualTo("studentUid", currentUser.getUid())
                        .limit(1)
                        .get()
                        .addOnSuccessListener(querySnap -> {
                            if (querySnap != null && !querySnap.isEmpty()) {
                                DocumentSnapshot doc = querySnap.getDocuments().get(0);
                                String existingChatId = doc.getId();
                                String adminUid = doc.getString("adminUid");
                                if (getContext() != null) {
                                    Intent intent = new Intent(getContext(), MessageActivity.class);
                                    intent.putExtra("CHAT_ID", existingChatId);
                                    intent.putExtra("RECIPIENT_UID", adminUid != null ? adminUid : "campus_admin_desk");
                                    intent.putExtra("RECIPIENT_NAME", "The Campus Access Editorial Desk");
                                    startActivity(intent);
                                }
                            } else {
                                fallbackProfileMessaging(db);
                            }
                        })
                        .addOnFailureListener(e -> fallbackProfileMessaging(db));
                return;
            }
            fallbackProfileMessaging(db);
        });
    }

    private void fallbackProfileMessaging(FirebaseFirestore db) {
        // 1. Check system_config/admin_contact document
        db.collection("system_config").document("admin_contact").get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists() && snapshot.getString("adminUid") != null) {
                        String adminUid = snapshot.getString("adminUid");
                        String adminName = snapshot.getString("adminName") != null ? snapshot.getString("adminName") : "The Campus Access";
                        launchMessageActivity(adminUid, adminName);
                    } else {
                        // 2. Fallback: Query first active ADMIN user dynamically from users collection
                        db.collection("users")
                                .whereEqualTo("role", "ADMIN")
                                .limit(1)
                                .get()
                                .addOnSuccessListener(querySnap -> {
                                    if (querySnap != null && !querySnap.isEmpty()) {
                                        DocumentSnapshot adminDoc = querySnap.getDocuments().get(0);
                                        String adminUid = adminDoc.getId();
                                        String adminName = adminDoc.getString("name") != null ? adminDoc.getString("name") : "The Campus Access Editorial Desk";
                                        launchMessageActivity(adminUid, adminName);
                                    } else {
                                        // Generic default desk
                                        launchMessageActivity("campus_admin_desk", "The Campus Access Editorial Desk");
                                    }
                                })
                                .addOnFailureListener(e -> launchMessageActivity("campus_admin_desk", "The Campus Access Editorial Desk"));
                    }
                })
                .addOnFailureListener(e -> launchMessageActivity("campus_admin_desk", "The Campus Access Editorial Desk"));
    }

    private void launchMessageActivity(String recipientUid, String recipientName) {
        if (getContext() != null) {
            Intent intent = new Intent(getContext(), MessageActivity.class);
            intent.putExtra("RECIPIENT_UID", recipientUid);
            intent.putExtra("RECIPIENT_NAME", recipientName);
            startActivity(intent);
        }
    }

    private void showCreativeLogoutDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_creative_logout, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvLogoutUserEmail = dialogView.findViewById(R.id.tvLogoutUserEmail);
        View layoutToggleDarkMode = dialogView.findViewById(R.id.layoutToggleDarkMode);
        TextView btnConfirmLogout = dialogView.findViewById(R.id.btnConfirmLogout);

        if (layoutToggleDarkMode != null) {
            layoutToggleDarkMode.setOnClickListener(v -> {
                dialog.dismiss();
                if (getContext() != null) {
                    Intent intent = new Intent(getContext(), DarkModeActivity.class);
                    startActivity(intent);
                }
            });
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (tvLogoutUserEmail != null) {
            if (currentUser != null) {
                tvLogoutUserEmail.setText(PostAdapter.getSafeDisplayName(currentUser));
            } else {
                tvLogoutUserEmail.setText("Guest Student");
            }
        }

        if (btnConfirmLogout != null) {
            btnConfirmLogout.setOnClickListener(v -> {
                dialog.dismiss();
                
                // Show a standard confirmation dialog matching app theme
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                        .setTitle("Log Out")
                        .setMessage("Are you sure you want to log out of your account?")
                        .setPositiveButton("Yes", (dialogInterface, i) -> {
                            FirebaseAuth.getInstance().signOut();
                            if (getActivity() != null) {
                                Intent intent = new Intent(getActivity(), LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                getActivity().finish();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }

        dialog.show();
    }

    private void selectTab(String category) {
        resetTabStyles();

        if ("ALL".equalsIgnoreCase(category)) {
            if (tabAll != null) {
                tabAll.setBackgroundResource(R.drawable.bg_chip_selected);
                tabAll.setTextColor(getResources().getColor(R.color.white, null));
            }
            if (spinnerGalleryFolder != null) spinnerGalleryFolder.setVisibility(View.GONE);
            if (rvProfilePosts != null) {
                rvProfilePosts.setLayoutManager(new LinearLayoutManager(getContext()));
                rvProfilePosts.setNestedScrollingEnabled(false);
                rvProfilePosts.setAdapter(adapter);
            }
        } else if ("GALLERY".equalsIgnoreCase(category)) {
            if (tabGallery != null) {
                tabGallery.setBackgroundResource(R.drawable.bg_chip_selected);
                tabGallery.setTextColor(getResources().getColor(R.color.white, null));
            }
            if (spinnerGalleryFolder != null) spinnerGalleryFolder.setVisibility(View.VISIBLE);
            if (rvProfilePosts != null) {
                rvProfilePosts.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 3));
                rvProfilePosts.setNestedScrollingEnabled(false);
                gridAdapter = new GridMediaAdapter(profilePostsList, false);
                rvProfilePosts.setAdapter(gridAdapter);
            }
        } else {
            if (spinnerGalleryFolder != null) spinnerGalleryFolder.setVisibility(View.GONE);

            if ("VIDEOS".equalsIgnoreCase(category) && tabVideos != null) {
                tabVideos.setBackgroundResource(R.drawable.bg_chip_selected);
                tabVideos.setTextColor(getResources().getColor(R.color.white, null));
            }
            if (rvProfilePosts != null) {
                rvProfilePosts.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 3));
                rvProfilePosts.setNestedScrollingEnabled(false);
                gridAdapter = new GridMediaAdapter(profilePostsList, true);
                rvProfilePosts.setAdapter(gridAdapter);
            }
        }

        fetchCategoryPostsFromFirestore(category);
    }

    private void fetchCategoryPostsFromFirestore(String category) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query query = db.collection("posts")
                     .orderBy("timestamp", Query.Direction.DESCENDING)
                     .limit(100);

        if (profileListenerRegistration != null) {
            profileListenerRegistration.remove();
            profileListenerRegistration = null;
        }

        profileListenerRegistration = query.addSnapshotListener((queryDocumentSnapshots, error) -> {
            if (!isAdded() || getContext() == null || getView() == null) return;
            if (queryDocumentSnapshots != null && profilePostsList != null) {
                allCategoryPosts.clear();
                long now = System.currentTimeMillis();
                java.util.Set<String> folderSet = new java.util.HashSet<>();

                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    String authorName = doc.getString("authorName");
                    String content = doc.getString("content");
                    String badgeText = doc.getString("badgeText");
                    String cat = doc.getString("category");
                    String folderName = doc.getString("folderName");
                    Boolean isPinned = doc.getBoolean("isPinned");
                    Boolean isAiPick = doc.getBoolean("isAiPick");
                    Long likeCount = doc.getLong("likeCount");
                    Long loveCount = doc.getLong("loveCount");
                    Long commentCount = doc.getLong("commentCount");
                    Long timestamp = doc.getLong("timestamp");
                    Long scheduledTimestamp = doc.getLong("scheduledTimestamp");
                    
                    String authorUid = doc.getString("authorUid");
                    String photoUri = doc.getString("photoUri");
                    String videoUri = doc.getString("videoUri");
                    String docUri = doc.getString("docUri");
                    
                    List<String> mediaUris = new ArrayList<>();
                    if (doc.contains("mediaUris") && doc.get("mediaUris") instanceof List) {
                        List<?> rawList = (List<?>) doc.get("mediaUris");
                        for (Object o : rawList) {
                            if (o instanceof String) {
                                mediaUris.add((String) o);
                            }
                        }
                    }

                    // Check if post contains video
                    boolean hasVideo = (videoUri != null && !videoUri.isEmpty()) || "Video".equalsIgnoreCase(cat);
                    if (!hasVideo) {
                        for (String u : mediaUris) {
                            if (u != null && (u.contains(".mp4") || u.contains("/video/"))) {
                                hasVideo = true;
                                break;
                            }
                        }
                    }

                    // Check if post contains photo
                    boolean hasPhoto = false;
                    if (photoUri != null && !photoUri.isEmpty() && !photoUri.contains(".mp4") && !photoUri.contains("/video/")) {
                        hasPhoto = true;
                    }
                    if (!hasPhoto) {
                        for (String u : mediaUris) {
                            if (u != null && !u.contains(".mp4") && !u.contains("/video/")) {
                                hasPhoto = true;
                                break;
                            }
                        }
                    }
                    if ("Gallery".equalsIgnoreCase(cat)) {
                        hasPhoto = true;
                    }

                    // Client-side category filtering
                    if ("GALLERY".equalsIgnoreCase(category)) {
                        // Gallery is strictly for pictures/photos — no videos allowed!
                        if (!hasPhoto || hasVideo) continue;
                    } else if ("VIDEOS".equalsIgnoreCase(category)) {
                        // Videos/Reels is strictly for videos
                        if (!hasVideo) continue;
                    }

                    String moderationStatus = doc.getString("moderationStatus");
                    if ("DELETED".equals(moderationStatus) || "FLAGGED".equals(moderationStatus) || "ARCHIVED".equals(moderationStatus)) {
                        continue;
                    }

                    // Also broaden video detection: check mediaUris for cloudinary /video/ pattern
                    // Re-check hasVideo in case videoUri field is missing but mediaUris has videos
                    if (!hasVideo && photoUri != null && (photoUri.contains("/video/") || photoUri.endsWith(".mp4"))) {
                        hasVideo = true;
                        hasPhoto = false;
                    }

                    // Removed the scheduledTimestamp and timestamp filters based on user request.
                    // Users want to see ALL posts immediately in their profile, even if they have a future date set.

                    String dynamicPostMeta = TimeUtils.getRelativeTimeString(getContext(), timestamp != null ? timestamp : now, cat != null ? cat : category);

                    Post post = new Post(
                            authorName != null ? authorName : "BISU Student",
                            dynamicPostMeta,
                            content != null ? content : "",
                            badgeText != null ? badgeText : "",
                            cat != null ? cat : category,
                            isPinned != null && isPinned,
                            isAiPick != null && isAiPick,
                            likeCount != null ? likeCount.intValue() : 0,
                            loveCount != null ? loveCount.intValue() : 0,
                            commentCount != null ? commentCount.intValue() : 0
                    );
                    post.setId(doc.getId());
                    post.setFolderName(folderName != null ? folderName : "");
                    post.setAuthorUid(authorUid);
                    post.setPhotoUri(photoUri);
                    post.setVideoUri(videoUri);
                    post.setDocUri(docUri);
                    post.setMediaUris(mediaUris);
                    post.setTimestamp(timestamp != null ? timestamp : now);
                    allCategoryPosts.add(post);
                    
                    if (folderName != null && !folderName.isEmpty()) {
                        folderSet.add(folderName);
                    }
                }

                if ("GALLERY".equalsIgnoreCase(category) && spinnerGalleryFolder != null && getContext() != null) {
                    List<String> folders = new ArrayList<>();
                    folders.add("All Folders");
                    folders.addAll(folderSet);
                    
                    // Only update adapter if contents changed to prevent infinite loops
                    boolean updateAdapter = true;
                    if (spinnerGalleryFolder.getAdapter() != null && spinnerGalleryFolder.getAdapter().getCount() == folders.size()) {
                        updateAdapter = false; // Simplified check for now
                    }
                    if (updateAdapter) {
                        android.widget.ArrayAdapter<String> folderAdapter = new android.widget.ArrayAdapter<>(
                                getContext(), android.R.layout.simple_spinner_dropdown_item, folders);
                        spinnerGalleryFolder.setAdapter(folderAdapter);
                        
                        // Try to re-select current folder if it still exists
                        int selectedPosition = folders.indexOf(currentSelectedFolder);
                        if (selectedPosition >= 0) {
                            spinnerGalleryFolder.setSelection(selectedPosition);
                        } else {
                            spinnerGalleryFolder.setSelection(0);
                            currentSelectedFolder = "All Folders";
                        }
                    }
                }

                filterAndDisplayPosts();
            }
        });
    }

    private void filterAndDisplayPosts() {
        if (profilePostsList == null || allCategoryPosts == null) return;
        profilePostsList.clear();
        for (Post post : allCategoryPosts) {
            if ("All Folders".equals(currentSelectedFolder) || currentSelectedFolder.equals(post.getFolderName())) {
                profilePostsList.add(post);
            }
        }
        
        java.util.Collections.sort(profilePostsList, (p1, p2) -> {
            if (p1.isPinned() && !p2.isPinned()) return -1;
            if (!p1.isPinned() && p2.isPinned()) return 1;
            return Long.compare(p2.getTimestamp(), p1.getTimestamp());
        });

        if (tvEmptyStateProfile != null) {
            if (profilePostsList.isEmpty()) {
                tvEmptyStateProfile.setVisibility(View.VISIBLE);
            } else {
                tvEmptyStateProfile.setVisibility(View.GONE);
            }
        }

        if (rvProfilePosts != null && rvProfilePosts.getAdapter() != null) {
            if (rvProfilePosts.getAdapter() instanceof GridMediaAdapter) {
                ((GridMediaAdapter) rvProfilePosts.getAdapter()).setMediaList(profilePostsList);
            } else {
                rvProfilePosts.getAdapter().notifyDataSetChanged();
            }
            if (scrollViewProfile != null) {
                scrollViewProfile.post(() -> scrollViewProfile.scrollTo(0, 0));
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (profileListenerRegistration != null) {
            profileListenerRegistration.remove();
            profileListenerRegistration = null;
        }
    }

    private void resetTabStyles() {
        TextView[] tabs = {tabAll, tabGallery, tabVideos};
        for (TextView tab : tabs) {
            if (tab != null) {
                tab.setBackground(null);
                tab.setTextColor(getResources().getColor(R.color.text_secondary, null));
            }
        }
    }

    private void loadProfilePicture() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || imgProfileLogo == null || getContext() == null) return;
        
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        Boolean isMember = doc.getBoolean("isMember");
                        boolean isAdmin = role != null && ("ADMIN".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role));
                        boolean isApprovedMember = Boolean.TRUE.equals(isMember);
                        
                        if (adapter != null) {
                            adapter.setAdmin(isAdmin || isApprovedMember);
                        }

                        if (doc.getString("profileImageUrl") != null) {
                            String url = doc.getString("profileImageUrl");
                            if (!url.isEmpty() && isAdded() && getContext() != null) {
                                currentProfileImageUrl = url;
                                com.bumptech.glide.Glide.with(requireContext())
                                     .load(url)
                                     .placeholder(R.drawable.ic_bisu_logo_hd)
                                     .into(imgProfileLogo);
                            }
                        }
                    }
                });
    }

    private void uploadProfilePicture(Uri fileUri) {
        if (getContext() == null) return;
        Toast.makeText(getContext(), "Uploading profile picture...", Toast.LENGTH_SHORT).show();

        try {
            java.io.InputStream inputStream = getContext().getContentResolver().openInputStream(fileUri);
            CloudinaryUploader.uploadImage(inputStream, new CloudinaryUploader.CloudinaryUploadCallback() {
                @Override
                public void onSuccess(String url) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) return;

                    FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                            .update("profileImageUrl", url)
                            .addOnSuccessListener(aVoid -> {
                                if (isAdded() && getContext() != null && imgProfileLogo != null) {
                                    Toast.makeText(getContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();
                                    currentProfileImageUrl = url;
                                    com.bumptech.glide.Glide.with(requireContext())
                                         .load(url)
                                         .placeholder(R.drawable.ic_bisu_logo_hd)
                                         .into(imgProfileLogo);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                                }
                            });
                }

                @Override
                public void onFailure(String error) {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Upload failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to read image", Toast.LENGTH_SHORT).show();
        }
    }
}
