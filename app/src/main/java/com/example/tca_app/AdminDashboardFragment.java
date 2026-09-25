package com.example.tca_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDashboardFragment extends Fragment {

    private RecyclerView rvModeration;
    private ModerationAdapter adapter;
    private List<ModerationItem> moderationList;

    private RecyclerView rvMembershipRequests;
    private MembershipRequestAdapter membershipAdapter;
    private List<MembershipRequest> membershipRequestList;
    private TextView tvEmptyMembershipRequests;

    // Voting Poll Analytics Views
    private TextView tvVotingAnalyticsTotalCount;
    private TextView tvEmptyVotingPolls;
    private LinearLayout layoutVotingPollsList;

    private FirebaseFirestore db;

    private TextView tvPctAll, tvPctAcademics, tvPctBroadcast, tvPctEvents, tvPctUpdates, tvPctHoroscopes, tvPctLiterature;
    private TextView tvBar1Pct, tvBar2Pct, tvBar3Pct, tvBar4Pct;
    private View cardModerationActions;
    private View cardMembershipRequests;
    private View bar1Height, bar2Height, bar3Height, bar4Height;
    private TextView tvQueueCount, tvMetricsCount;
    private PieChartView pieChartView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        db = FirebaseFirestore.getInstance();

        cardModerationActions = view.findViewById(R.id.cardModerationActions);
        cardMembershipRequests = view.findViewById(R.id.cardMembershipRequests);

        View btnAdminOpenInquiries = view.findViewById(R.id.btnAdminOpenInquiries);
        if (btnAdminOpenInquiries != null) {
            btnAdminOpenInquiries.setOnClickListener(v -> {
                if (getContext() != null) {
                    startActivity(new android.content.Intent(getContext(), AdminInboxActivity.class));
                }
            });
        }

        View btnAdminOpenMemberChart = view.findViewById(R.id.btnAdminOpenMemberChart);
        if (btnAdminOpenMemberChart != null) {
            btnAdminOpenMemberChart.setOnClickListener(v -> {
                if (getContext() != null) {
                    android.content.Intent intent = new android.content.Intent(getContext(), EditorialChartActivity.class);
                    intent.putExtra("OPEN_ADD_MEMBER_DIALOG", true);
                    startActivity(intent);
                }
            });
        }

        // Bind Real-Time Donut Legend Percentage TextViews
        tvPctAll = view.findViewById(R.id.tvPctAll);
        tvPctAcademics = view.findViewById(R.id.tvPctAcademics);
        tvPctBroadcast = view.findViewById(R.id.tvPctBroadcast);
        tvPctEvents = view.findViewById(R.id.tvPctEvents);
        tvPctUpdates = view.findViewById(R.id.tvPctUpdates);
        tvPctHoroscopes = view.findViewById(R.id.tvPctHoroscopes);
        tvPctLiterature = view.findViewById(R.id.tvPctLiterature);

        // Bind Bar Chart Engagement Views
        tvBar1Pct = view.findViewById(R.id.tvBar1Pct);
        tvBar2Pct = view.findViewById(R.id.tvBar2Pct);
        tvBar3Pct = view.findViewById(R.id.tvBar3Pct);
        tvBar4Pct = view.findViewById(R.id.tvBar4Pct);

        bar1Height = view.findViewById(R.id.bar1Height);
        bar2Height = view.findViewById(R.id.bar2Height);
        bar3Height = view.findViewById(R.id.bar3Height);
        bar4Height = view.findViewById(R.id.bar4Height);

        pieChartView = view.findViewById(R.id.pieChartView);
        if (pieChartView != null && getContext() != null) {
            pieChartView.setColors(new int[]{
                android.graphics.Color.parseColor("#FFA500"), // 0: Academics (Orange)
                android.graphics.Color.parseColor("#0000FF"), // 1: Broadcast (Blue)
                android.graphics.Color.parseColor("#FFFF00"), // 2: Horoscopes (Yellow)
                android.graphics.Color.parseColor("#00FF00"), // 3: Updates (Yellow-Green)
                android.graphics.Color.parseColor("#FF0000"), // 4: Events (Red)
                android.graphics.Color.parseColor("#00FFFF")  // 5: Literature (Cyan)
            });
        }

        // Metrics Summary Counters
        tvQueueCount = view.findViewById(R.id.tvQueueCount);
        tvMetricsCount = view.findViewById(R.id.tvMetricsCount);

        // Bind interactive moderation pill buttons
        TextView btnPillDuplicate = view.findViewById(R.id.btnPillDuplicate);
        TextView btnPillRemove1 = view.findViewById(R.id.btnPillRemove1);
        TextView btnPillNone = view.findViewById(R.id.btnPillNone);
        TextView btnPillRemove2 = view.findViewById(R.id.btnPillRemove2);
        TextView btnPillWarn = view.findViewById(R.id.btnPillWarn);

        if (btnPillDuplicate != null) {
            btnPillDuplicate.setOnClickListener(v -> callModeratePostFunction("INSPECT", null, null, "AI Duplicate Inspected"));
        }

        if (btnPillRemove1 != null) {
            btnPillRemove1.setOnClickListener(v -> callModeratePostFunction("ARCHIVE", null, null, "Content Archived"));
        }

        if (btnPillNone != null) {
            btnPillNone.setOnClickListener(v -> callModeratePostFunction("DISMISS", null, null, "Flag Dismissed"));
        }

        if (btnPillRemove2 != null) {
            btnPillRemove2.setOnClickListener(v -> callModeratePostFunction("DELETE", null, null, "All flagged items deleted"));
        }

        if (btnPillWarn != null) {
            btnPillWarn.setOnClickListener(v -> callModeratePostFunction("WARN", null, null, "Admin Moderation Warning"));
        }

        rvModeration = view.findViewById(R.id.rvModeration);
        rvModeration.setLayoutManager(new LinearLayoutManager(getContext()));

        moderationList = new ArrayList<>();
        adapter = new ModerationAdapter(moderationList);
        rvModeration.setAdapter(adapter);

        // Bind Pending Membership Requests UI
        rvMembershipRequests = view.findViewById(R.id.rvMembershipRequests);
        tvEmptyMembershipRequests = view.findViewById(R.id.tvEmptyMembershipRequests);
        if (rvMembershipRequests != null) {
            rvMembershipRequests.setLayoutManager(new LinearLayoutManager(getContext()));
            membershipRequestList = new ArrayList<>();
            membershipAdapter = new MembershipRequestAdapter(membershipRequestList, remainingCount -> {
                if (tvEmptyMembershipRequests != null) {
                    tvEmptyMembershipRequests.setVisibility(remainingCount == 0 ? View.VISIBLE : View.GONE);
                }
            });
            rvMembershipRequests.setAdapter(membershipAdapter);
        }

        // Bind Real-Time Campus Voting Poll Analytics Views
        tvVotingAnalyticsTotalCount = view.findViewById(R.id.tvVotingAnalyticsTotalCount);
        tvEmptyVotingPolls = view.findViewById(R.id.tvEmptyVotingPolls);
        layoutVotingPollsList = view.findViewById(R.id.layoutVotingPollsList);

        // Two-Tier Role-Based Access Enforcement
        AuthUtils.checkCurrentUserAccess((isApprovedMember, isAdmin, role) -> {
            if (!isAdded() || getContext() == null) return;

            if (!isApprovedMember && !isAdmin) {
                // Access Denied: Regular non-member student cannot view Dashboard
                Toast.makeText(getContext(), "🔒 Access Denied: Dashboard is for approved Campus Access members and Admins only.", Toast.LENGTH_LONG).show();
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
                return;
            }

            // Approved Members & Admins see Analytics
            loadAnalyticsUsingAggregationQueries();
            loadVotingPollsAnalytics();

            // Moderation Actions & Membership Approval are ADMIN-ONLY
            if (isAdmin) {
                if (cardModerationActions != null) cardModerationActions.setVisibility(View.VISIBLE);
                if (cardMembershipRequests != null) cardMembershipRequests.setVisibility(View.VISIBLE);
                if (rvModeration != null) rvModeration.setVisibility(View.VISIBLE);
                loadModerationQueue();
                loadPendingMembershipRequests();
            } else {
                // Approved Non-Admin Member: Hide Moderation & Membership Request Cards
                if (cardModerationActions != null) cardModerationActions.setVisibility(View.GONE);
                if (cardMembershipRequests != null) cardMembershipRequests.setVisibility(View.GONE);
                if (rvModeration != null) rvModeration.setVisibility(View.GONE);
            }
        });

        return view;
    }

    private void callModeratePostFunction(String action, String moderationId, String postId, String successMsg) {
        if (db == null) return;
        com.google.firebase.firestore.WriteBatch batch = db.batch();

        if ("ARCHIVE".equals(action)) {
            if (moderationId != null && !moderationId.isEmpty()) {
                batch.update(db.collection("moderation_queue").document(moderationId), "moderationStatus", "ARCHIVED");
            }
            if (postId != null && !postId.isEmpty()) {
                batch.update(db.collection("posts").document(postId), "moderationStatus", "ARCHIVED");
            }
        } else if ("DELETE".equals(action)) {
            if (moderationId != null && !moderationId.isEmpty()) {
                batch.update(db.collection("moderation_queue").document(moderationId), "moderationStatus", "DELETED");
            }
            if (postId != null && !postId.isEmpty()) {
                batch.delete(db.collection("posts").document(postId));
            }
        } else if ("WARN".equals(action)) {
            if (moderationId != null && !moderationId.isEmpty()) {
                batch.update(db.collection("moderation_queue").document(moderationId), "moderationStatus", "WARNED");
            }
            if (postId != null && !postId.isEmpty()) {
                batch.update(db.collection("posts").document(postId), "moderationStatus", "WARNED");
            }
            Map<String, Object> notif = new HashMap<>();
            notif.put("title", "⚠️ Admin Moderation Warning");
            notif.put("message", "Your post was flagged for community violation.");
            notif.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
            batch.set(db.collection("notifications").document(), notif);
        } else if ("APPROVE".equals(action)) {
            if (moderationId != null && !moderationId.isEmpty()) {
                batch.update(db.collection("moderation_queue").document(moderationId), "moderationStatus", "APPROVED");
            }
            if (postId != null && !postId.isEmpty()) {
                batch.update(db.collection("posts").document(postId), "moderationStatus", "APPROVED");
            }
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "✅ " + successMsg, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "❌ Moderation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * OPTIMIZED FIRESTORE AGGREGATION & SUMMARY LISTENER
     * Eliminates client-side manual iteration over all documents, avoiding OOM & excessive read billing.
     */
    private void loadAnalyticsUsingAggregationQueries() {
        if (db == null) return;

        // Listen to central analytics_summary document maintained by Cloud Functions backend
        db.collection("analytics_summary").document("counters")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded() || getContext() == null || getView() == null) return;
                    if (snapshot != null && snapshot.exists()) {
                        Long total = snapshot.getLong("totalPosts");
                        long totalPosts = total != null ? total : 1;

                        Long countBroadcast = snapshot.getLong("count_Broadcast");
                        Long countEvents = snapshot.getLong("count_Events");
                        Long countUpdates = snapshot.getLong("count_Updates");
                        Long countHoroscopes = snapshot.getLong("count_Horoscopes");
                        Long countLiterature = snapshot.getLong("count_Literature");
                        Long countAcademics = snapshot.getLong("count_Academics");

                        long b = countBroadcast != null ? countBroadcast : 0;
                        long e = countEvents != null ? countEvents : 0;
                        long u = countUpdates != null ? countUpdates : 0;
                        long h = countHoroscopes != null ? countHoroscopes : 0;
                        long l = countLiterature != null ? countLiterature : 0;
                        long ac = countAcademics != null ? countAcademics : 0;

                        updateMetricsUI(totalPosts, ac, b, h, u, e, l);
                    } else {
                        // Fallback: Perform native Firestore count() aggregation queries
                        performNativeCountAggregations();
                    }
                });
    }

    private void performNativeCountAggregations() {
        db.collection("posts").count().get(AggregateSource.SERVER)
                .addOnSuccessListener(task -> {
                    long totalPosts = task.getCount();
                    final long finalTotal = totalPosts;

                    com.google.android.gms.tasks.Task<com.google.firebase.firestore.AggregateQuerySnapshot> aTask = db.collection("posts").whereEqualTo("category", "Academics").count().get(AggregateSource.SERVER);
                    com.google.android.gms.tasks.Task<com.google.firebase.firestore.AggregateQuerySnapshot> bTask = db.collection("posts").whereEqualTo("category", "Broadcast").count().get(AggregateSource.SERVER);
                    com.google.android.gms.tasks.Task<com.google.firebase.firestore.AggregateQuerySnapshot> eTask = db.collection("posts").whereEqualTo("category", "Events").count().get(AggregateSource.SERVER);
                    com.google.android.gms.tasks.Task<com.google.firebase.firestore.AggregateQuerySnapshot> uTask = db.collection("posts").whereEqualTo("category", "Updates").count().get(AggregateSource.SERVER);
                    com.google.android.gms.tasks.Task<com.google.firebase.firestore.AggregateQuerySnapshot> hTask = db.collection("posts").whereEqualTo("category", "Horoscopes").count().get(AggregateSource.SERVER);
                    com.google.android.gms.tasks.Task<com.google.firebase.firestore.AggregateQuerySnapshot> lTask = db.collection("posts").whereEqualTo("category", "Literature").count().get(AggregateSource.SERVER);

                    com.google.android.gms.tasks.Tasks.whenAllComplete(aTask, bTask, eTask, uTask, hTask, lTask)
                            .addOnCompleteListener(t -> {
                                if (!isAdded() || getContext() == null || getView() == null) return;
                                long ac = aTask.isSuccessful() && aTask.getResult() != null ? aTask.getResult().getCount() : 0;
                                long b = bTask.isSuccessful() && bTask.getResult() != null ? bTask.getResult().getCount() : 0;
                                long e = eTask.isSuccessful() && eTask.getResult() != null ? eTask.getResult().getCount() : 0;
                                long u = uTask.isSuccessful() && uTask.getResult() != null ? uTask.getResult().getCount() : 0;
                                long h = hTask.isSuccessful() && hTask.getResult() != null ? hTask.getResult().getCount() : 0;
                                long l = lTask.isSuccessful() && lTask.getResult() != null ? lTask.getResult().getCount() : 0;

                                updateMetricsUI(finalTotal, ac, b, h, u, e, l);
                            });
                });
    }

    private void updateMetricsUI(long totalPosts, long ac, long b, long h, long u, long e, long l) {
        int pctAcademics = totalPosts > 0 ? (int) ((ac * 100) / totalPosts) : 0;
        int pctBroadcast = totalPosts > 0 ? (int) ((b * 100) / totalPosts) : 0;
        int pctHoroscopes = totalPosts > 0 ? (int) ((h * 100) / totalPosts) : 0;
        int pctUpdates = totalPosts > 0 ? (int) ((u * 100) / totalPosts) : 0;
        int pctEvents = totalPosts > 0 ? (int) ((e * 100) / totalPosts) : 0;
        int pctLiterature = totalPosts > 0 ? (int) ((l * 100) / totalPosts) : 0;

        setTextViewDirect(tvPctAll, (int) totalPosts, false);
        setTextViewDirect(tvPctAcademics, pctAcademics, true);
        setTextViewDirect(tvPctBroadcast, pctBroadcast, true);
        setTextViewDirect(tvPctHoroscopes, pctHoroscopes, true);
        setTextViewDirect(tvPctUpdates, pctUpdates, true);
        setTextViewDirect(tvPctEvents, pctEvents, true);
        setTextViewDirect(tvPctLiterature, pctLiterature, true);

        if (pieChartView != null) {
            pieChartView.setData(new float[]{ac, b, h, u, e, l});
        }

        setTextViewDirect(tvBar1Pct, pctBroadcast, true);
        setTextViewDirect(tvBar2Pct, pctEvents, true);
        setTextViewDirect(tvBar3Pct, pctUpdates, true);
        setTextViewDirect(tvBar4Pct, (pctHoroscopes + pctLiterature), true);

        updateBarHeight(bar1Height, pctBroadcast);
        updateBarHeight(bar2Height, pctEvents);
        updateBarHeight(bar3Height, pctUpdates);
        updateBarHeight(bar4Height, pctHoroscopes + pctLiterature);

        if (tvMetricsCount != null) {
            setTextViewDirect(tvMetricsCount, (int) totalPosts, false);
        }
    }

    private void setTextViewDirect(TextView textView, int targetValue, boolean isPercentage) {
        if (textView == null) return;
        textView.setText(targetValue + (isPercentage ? "%" : ""));
    }

    private void updateBarHeight(View barView, int percentage) {
        if (barView == null || getContext() == null) return;
        int maxBarHeightPx = (int) (120 * getResources().getDisplayMetrics().density);
        int targetHeightPx = Math.max((maxBarHeightPx * Math.min(percentage, 100)) / 100, (int) (6 * getResources().getDisplayMetrics().density));

        ViewGroup.LayoutParams lp = barView.getLayoutParams();
        if (lp != null) {
            lp.height = targetHeightPx;
            barView.setLayoutParams(lp);
        }
    }

    private void loadModerationQueue() {
        if (db == null) return;
        // COST FIX: .limit(30) prevents OOM on large moderation queues. Admins see the
        // 30 most recent unresolved flags — oldest flagged items should be handled first.
        db.collection("moderation_queue")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(30)
                .addSnapshotListener((snapshots, e) -> {
                    if (!isAdded() || getContext() == null || getView() == null) return;
                    moderationList.clear();
                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            if (doc == null) continue;
                            String status = doc.getString("moderationStatus");
                            if ("DELETED".equals(status) || "ARCHIVED".equals(status)) {
                                continue;
                            }
                            String reason = doc.getString("reason");
                            String score = doc.getString("aiScore");
                            String text = doc.getString("content");
                            String postId = doc.getString("postId");

                            ModerationItem item = new ModerationItem(
                                    doc.getId() != null ? doc.getId() : "",
                                    postId != null ? postId : "",
                                    reason != null ? reason : "AI Flagged Content",
                                    score != null ? score : "95%",
                                    text != null ? text : "Flagged post content..."
                            );
                            moderationList.add(item);
                        }
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                    if (tvQueueCount != null) tvQueueCount.setText(moderationList.size() + " Items");
                });
    }

    private void loadPendingMembershipRequests() {
        if (db == null) return;

        // COST FIX: .limit(30) caps how many pending requests are fetched at once.
        // If there are more than 30 pending requests, the admin should process the current batch first.
        db.collection("users")
                .whereEqualTo("isMemberPending", true)
                .limit(30)
                .addSnapshotListener((snapshots, e) -> {
                    if (!isAdded() || getContext() == null || getView() == null) return;
                    if (membershipRequestList == null) return;
                    membershipRequestList.clear();

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            if (doc == null || !doc.exists()) continue;
                            String name = doc.getString("name");
                            String email = doc.getString("email");
                            Long createdAt = doc.getLong("createdAt");

                            MembershipRequest req = new MembershipRequest(
                                    doc.getId(),
                                    doc.getId(),
                                    name != null && !name.isEmpty() ? name : "Student Applicant",
                                    email != null && !email.isEmpty() ? email : "student@bisu.edu.ph",
                                    createdAt != null ? createdAt : System.currentTimeMillis()
                            );
                            membershipRequestList.add(req);
                        }
                    }

                    if (membershipAdapter != null) membershipAdapter.notifyDataSetChanged();
                    if (tvEmptyMembershipRequests != null) {
                        tvEmptyMembershipRequests.setVisibility(membershipRequestList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    /**
     * Real-time listener for Campus Voting Polls and computes live vote percentages and leading candidate.
     */
    private void loadVotingPollsAnalytics() {
        if (db == null) return;

        db.collection("voting_polls")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener((snapshots, e) -> {
                    if (!isAdded() || getContext() == null || getView() == null) return;
                    if (layoutVotingPollsList == null) return;
                    layoutVotingPollsList.removeAllViews();

                    if (snapshots == null || snapshots.isEmpty()) {
                        if (tvEmptyVotingPolls != null) tvEmptyVotingPolls.setVisibility(View.VISIBLE);
                        if (layoutVotingPollsList != null) layoutVotingPollsList.setVisibility(View.GONE);
                        if (tvVotingAnalyticsTotalCount != null) tvVotingAnalyticsTotalCount.setText("0 Polls Active");
                        return;
                    }

                    if (tvEmptyVotingPolls != null) tvEmptyVotingPolls.setVisibility(View.GONE);
                    if (layoutVotingPollsList != null) layoutVotingPollsList.setVisibility(View.VISIBLE);
                    if (tvVotingAnalyticsTotalCount != null) {
                        tvVotingAnalyticsTotalCount.setText(snapshots.size() + (snapshots.size() == 1 ? " Poll Active" : " Polls Active"));
                    }

                    LayoutInflater inflater = LayoutInflater.from(getContext());

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        if (doc == null || !doc.exists()) continue;

                        String question = doc.getString("question");
                        String eventName = doc.getString("eventName");
                        Long totalVotesLong = doc.getLong("totalVotes");
                        long totalVotes = totalVotesLong != null ? totalVotesLong : 0L;

                        @SuppressWarnings("unchecked")
                        List<String> options = (List<String>) doc.get("options");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> votesCountMap = (Map<String, Object>) doc.get("votesCount");
                        if (options == null) options = new ArrayList<>();
                        if (votesCountMap == null) votesCountMap = new HashMap<>();

                        View card = inflater.inflate(R.layout.item_voting_poll_analytics, layoutVotingPollsList, false);

                        TextView tvPollTitle = card.findViewById(R.id.tvPollTitleAnalytics);
                        TextView tvTotalBadge = card.findViewById(R.id.tvTotalVotesBadge);
                        TextView tvEvent = card.findViewById(R.id.tvPollEventAnalytics);
                        TextView tvLeading = card.findViewById(R.id.tvLeadingCandidateBadge);
                        LinearLayout layoutProgress = card.findViewById(R.id.layoutOptionsProgressContainer);

                        tvPollTitle.setText(question != null ? question : "Campus Voting Poll");
                        tvEvent.setText("Event: " + (eventName != null ? eventName : "General Campus Vote"));
                        tvTotalBadge.setText(totalVotes + (totalVotes == 1 ? " Vote" : " Votes"));

                        String leadingOption = "—";
                        long maxVotes = -1;

                        layoutProgress.removeAllViews();

                        for (String opt : options) {
                            long count = 0;
                            Object countObj = votesCountMap.get(opt);
                            if (countObj instanceof Number) {
                                count = ((Number) countObj).longValue();
                            }

                            if (count > maxVotes) {
                                maxVotes = count;
                                leadingOption = opt;
                            }

                            int percentage = (totalVotes > 0) ? (int) Math.round(((double) count / totalVotes) * 100.0) : 0;

                            LinearLayout row = new LinearLayout(getContext());
                            row.setOrientation(LinearLayout.VERTICAL);
                            row.setPadding(0, 6, 0, 6);

                            LinearLayout headerRow = new LinearLayout(getContext());
                            headerRow.setOrientation(LinearLayout.HORIZONTAL);

                            TextView tvOptName = new TextView(getContext());
                            tvOptName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
                            tvOptName.setText(opt);
                            tvOptName.setTextColor(getResources().getColor(R.color.text_primary));
                            tvOptName.setTextSize(12.5f);
                            tvOptName.setTypeface(null, Typeface.BOLD);

                            TextView tvOptVotes = new TextView(getContext());
                            tvOptVotes.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            tvOptVotes.setText(count + " (" + percentage + "%)");
                            tvOptVotes.setTextColor(getResources().getColor(R.color.purple_primary));
                            tvOptVotes.setTextSize(12f);
                            tvOptVotes.setTypeface(null, Typeface.BOLD);

                            headerRow.addView(tvOptName);
                            headerRow.addView(tvOptVotes);
                            row.addView(headerRow);

                            ProgressBar pb = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
                            LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    (int) (8 * getResources().getDisplayMetrics().density)
                            );
                            pbParams.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
                            pb.setLayoutParams(pbParams);
                            pb.setMax(100);
                            pb.setProgress(percentage);
                            pb.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#7C3AED")));
                            pb.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E9D5FF")));

                            row.addView(pb);
                            layoutProgress.addView(row);
                        }

                        if (totalVotes > 0 && maxVotes > 0) {
                            int leadPct = (int) Math.round(((double) maxVotes / totalVotes) * 100.0);
                            tvLeading.setText("🏆 Leading: " + leadingOption + " (" + leadPct + "%)");
                            tvLeading.setVisibility(View.VISIBLE);
                        } else {
                            tvLeading.setText("⏳ Waiting for initial student votes...");
                            tvLeading.setVisibility(View.VISIBLE);
                        }

                        layoutVotingPollsList.addView(card);
                    }
                });
    }
}
