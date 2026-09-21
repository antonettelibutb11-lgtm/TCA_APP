package com.example.tca_app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class HomeFeedFragment extends Fragment {

    private RecyclerView rvNewsFeed;
    private PostAdapter adapter;
    private List<Post> allPostsList;
    private List<Post> displayPostsList;
    private FirebaseFirestore db;
    private ListenerRegistration postsListener;

    private TextView chipAll, chipAcademics, chipEvents, chipLiterature, chipSports, chipBroadcast, chipHoroscopes;
    private EditText etSearch;
    private String selectedCategory = "All";
    private String currentSearchQuery = "";

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyStateFeed;

    @Nullable
    private LinearLayout layoutLoadingFeed;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_feed, container, false);

        db = FirebaseFirestore.getInstance();

        etSearch = view.findViewById(R.id.etSearch);
        chipAll = view.findViewById(R.id.chipAll);
        chipAcademics = view.findViewById(R.id.chipAcademics);
        chipEvents = view.findViewById(R.id.chipEvents);
        chipLiterature = view.findViewById(R.id.chipLiterature);
        chipSports = view.findViewById(R.id.chipSports);
        chipBroadcast = view.findViewById(R.id.chipBroadcast);
        chipHoroscopes = view.findViewById(R.id.chipHoroscopes);

        layoutLoadingFeed = view.findViewById(R.id.layoutLoadingFeed);
        tvEmptyStateFeed = view.findViewById(R.id.tvEmptyStateFeed);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                listenToFirebasePosts();
            });
        }

        rvNewsFeed = view.findViewById(R.id.rvNewsFeed);
        rvNewsFeed.setLayoutManager(new LinearLayoutManager(getContext()));

        allPostsList = new ArrayList<>();
        displayPostsList = new ArrayList<>();
        adapter = new PostAdapter(displayPostsList);
        rvNewsFeed.setAdapter(adapter);
        VideoScrollHelper.attachToRecyclerView(rvNewsFeed);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                rvNewsFeed.post(() -> VideoScrollHelper.handleVideoVisibility(rvNewsFeed));
            }
        });


        setupCategoryClickListeners();
        setupSearchListener();
        
        // Quick access shortcut cards
        LinearLayout cardOrganizations = view.findViewById(R.id.cardOrganizations);

        if (cardOrganizations != null) {
            cardOrganizations.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new OrganizationFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // Create Post shortcut card — Campus Access Members and Admins only
        LinearLayout cardCreatePost = view.findViewById(R.id.cardCreatePost);
        if (cardCreatePost != null) {
            cardCreatePost.setVisibility(View.GONE);
            cardCreatePost.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(getActivity(), CreatePostActivity.class);
                startActivity(intent);
            });
        }

        // Check user role for admin & member capabilities
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            AuthUtils.checkCurrentUserAccess((isApprovedMember, isAdmin, role) -> {
                if (!isAdded() || getContext() == null) return;
                if (adapter != null) {
                    adapter.setAdmin(isAdmin || isApprovedMember);
                }
                if (cardCreatePost != null) {
                    cardCreatePost.setVisibility((isApprovedMember || isAdmin) ? View.VISIBLE : View.GONE);
                }
            });
        }

        listenToFirebasePosts();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postsListener != null) {
            postsListener.remove();
            postsListener = null;
        }
    }

    private void setupCategoryClickListeners() {
        if (chipAll != null) chipAll.setOnClickListener(v -> selectCategory("All", chipAll));
        if (chipAcademics != null) chipAcademics.setOnClickListener(v -> selectCategory("Academics", chipAcademics));
        if (chipEvents != null) chipEvents.setOnClickListener(v -> selectCategory("Events", chipEvents));
        if (chipLiterature != null) chipLiterature.setOnClickListener(v -> selectCategory("Literature", chipLiterature));
        if (chipSports != null) chipSports.setOnClickListener(v -> selectCategory("Sports", chipSports));
        if (chipBroadcast != null) chipBroadcast.setOnClickListener(v -> selectCategory("Broadcast", chipBroadcast));
        if (chipHoroscopes != null) chipHoroscopes.setOnClickListener(v -> selectCategory("Horoscopes", chipHoroscopes));
    }

    private void selectCategory(String category, TextView selectedChipView) {
        this.selectedCategory = category;
        updateChipStyles(selectedChipView);
        listenToFirebasePosts();
    }

    private void updateChipStyles(TextView activeChipView) {
        TextView[] chips = {chipAll, chipAcademics, chipEvents, chipLiterature, chipSports, chipBroadcast, chipHoroscopes};
        for (TextView chip : chips) {
            if (chip != null) {
                if (chip == activeChipView) {
                    chip.setBackgroundResource(R.drawable.bg_chip_selected);
                    chip.setTextColor(getResources().getColor(R.color.white, null));
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_unselected);
                    chip.setTextColor(getResources().getColor(R.color.text_primary, null));
                }
            }
        }
    }

    private void setupSearchListener() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                filterPosts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterPosts() {
        List<Post> newList = new ArrayList<>();
        for (Post post : allPostsList) {
            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                    post.getContent().toLowerCase().contains(currentSearchQuery) ||
                    post.getAuthorName().toLowerCase().contains(currentSearchQuery) ||
                    post.getBadgeText().toLowerCase().contains(currentSearchQuery) ||
                    post.getCategory().toLowerCase().contains(currentSearchQuery);

            if (matchesSearch) {
                newList.add(post);
            }
        }

        java.util.Collections.sort(newList, (p1, p2) -> {
            if (p1.isPinned() && !p2.isPinned()) return -1;
            if (!p1.isPinned() && p2.isPinned()) return 1;
            return Long.compare(p2.getTimestamp(), p1.getTimestamp());
        });

        if (tvEmptyStateFeed != null) {
            if (newList.isEmpty()) {
                tvEmptyStateFeed.setVisibility(View.VISIBLE);
            } else {
                tvEmptyStateFeed.setVisibility(View.GONE);
            }
        }

        if (layoutLoadingFeed != null) layoutLoadingFeed.setVisibility(View.GONE);
        if (rvNewsFeed != null) rvNewsFeed.setVisibility(View.VISIBLE);

        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return displayPostsList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return displayPostsList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Post oldPost = displayPostsList.get(oldItemPosition);
                Post newPost = newList.get(newItemPosition);
                return oldPost.getContent().equals(newPost.getContent()) && 
                       oldPost.getLikeCount() == newPost.getLikeCount() &&
                       oldPost.isPinned() == newPost.isPinned();
            }
        });
        displayPostsList.clear();
        displayPostsList.addAll(newList);
        diffResult.dispatchUpdatesTo(adapter);
    }

    private void listenToFirebasePosts() {
        if (db == null) return;

        if (layoutLoadingFeed != null) layoutLoadingFeed.setVisibility(View.VISIBLE);

        if (postsListener != null) {
            postsListener.remove();
        }

        // Removed the scheduledTimestamp filter based on user request.
        // Users want to see ALL posts immediately, even if they have a future date set.
        // We now sort by the actual creation timestamp so new posts always appear at the top.
        long now = System.currentTimeMillis();
        Query query = db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50);
        
        // We remove the server-side category filter to avoid requiring a composite index.
        // Instead, we filter the category client-side below.

        // We remove the server-side category filter to avoid requiring a composite index.
        // Instead, we will filter the category client-side below.

        postsListener = query.addSnapshotListener((queryDocumentSnapshots, error) -> {
            if (!isAdded() || getContext() == null || getView() == null) return;
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }

            List<Post> firebasePosts = new ArrayList<>();
            if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    String category = doc.getString("category");
                    
                    // Client-side category filtering
                    if (!selectedCategory.equalsIgnoreCase("All") && !selectedCategory.equalsIgnoreCase(category)) {
                        continue;
                    }

                    String authorName = doc.getString("authorName");
                    String content = doc.getString("content");
                    String badgeText = doc.getString("badgeText");
                    Boolean isPinned = doc.getBoolean("isPinned");
                    Boolean isAiPick = doc.getBoolean("isAiPick");
                    Long likeCount = doc.getLong("likeCount");
                    Long loveCount = doc.getLong("loveCount");
                    Long commentCount = doc.getLong("commentCount");
                    Long scheduledTimestamp = doc.getLong("scheduledTimestamp");
                    Long timestamp = doc.getLong("timestamp");
                    
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

                    String moderationStatus = doc.getString("moderationStatus");
                    if ("DELETED".equals(moderationStatus) || "FLAGGED".equals(moderationStatus) || "ARCHIVED".equals(moderationStatus)) {
                        continue;
                    }

                    // scheduledTimestamp filtering is now handled server-side via the Firestore query.
                    // No client-side date filtering needed here.

                    if (timestamp == null || timestamp == 0) {
                        timestamp = now;
                    }

                    String dynamicPostMeta = TimeUtils.getRelativeTimeString(getContext(), timestamp, category != null && !category.isEmpty() ? category : "Events");

                    Post post = new Post(
                            authorName != null && !authorName.isEmpty() ? authorName : "BISU Community",
                            dynamicPostMeta,
                            content != null ? content : "",
                            badgeText != null ? badgeText : "",
                            category != null && !category.isEmpty() ? category : "Events",
                            isPinned != null && isPinned,
                            isAiPick != null && isAiPick,
                            likeCount != null ? likeCount.intValue() : 0,
                            loveCount != null ? loveCount.intValue() : 0,
                            commentCount != null ? commentCount.intValue() : 0
                    );
                    post.setTimestamp(timestamp);
                    post.setId(doc.getId());
                    post.setAuthorUid(authorUid);
                    post.setPhotoUri(photoUri);
                    post.setVideoUri(videoUri);
                    post.setDocUri(docUri);
                    post.setMediaUris(mediaUris);
                    firebasePosts.add(post);
                }
            }

            allPostsList.clear();
            allPostsList.addAll(firebasePosts);

            filterPosts();
        });
    }
}
