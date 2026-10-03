package com.example.tca_app;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;
    private String postId;
    private OnCommentActionCallback actionCallback;
    private List<ListenerRegistration> replyListeners = new ArrayList<>();
    private Map<String, ListenerRegistration> activeReplyListeners = new HashMap<>();

    public interface OnCommentActionCallback {
        void onReplyClicked(Comment parentComment);
    }

    public CommentAdapter(List<Comment> commentList, String postId, OnCommentActionCallback callback) {
        this.commentList = commentList;
        this.postId = postId;
        this.actionCallback = callback;
    }

    public void cleanup() {
        for (ListenerRegistration reg : replyListeners) {
            if (reg != null) reg.remove();
        }
        replyListeners.clear();

        if (activeReplyListeners != null) {
            for (ListenerRegistration reg : activeReplyListeners.values()) {
                if (reg != null) reg.remove();
            }
            activeReplyListeners.clear();
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.tvCommentAuthor.setText(comment.getAuthor());
        holder.tvCommentText.setText(comment.getText());
        holder.tvCommentTime.setText(TimeUtils.getRelativeTimeString(
                holder.itemView.getContext(),
                comment.getTimestamp() > 0 ? comment.getTimestamp() : System.currentTimeMillis(), "Comment"));

        // Determine current user reaction
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = currentUser != null ? currentUser.getUid() : "";
        String userRx = comment.getCurrentUserReaction(currentUid);

        // Update React Button appearance (like Facebook)
        if (!userRx.isEmpty()) {
            if ("❤️".equals(userRx)) {
                holder.btnReact.setText("❤️ Love");
                holder.btnReact.setTextColor(Color.parseColor("#E11D48"));
            } else if ("😂".equals(userRx) || "😆".equals(userRx)) {
                holder.btnReact.setText(userRx + " Haha");
                holder.btnReact.setTextColor(Color.parseColor("#D97706"));
            } else if ("👍".equals(userRx)) {
                holder.btnReact.setText("👍 Like");
                holder.btnReact.setTextColor(Color.parseColor("#2563EB"));
            } else if ("🤗".equals(userRx) || "🥰".equals(userRx)) {
                holder.btnReact.setText(userRx + " Care");
                holder.btnReact.setTextColor(Color.parseColor("#D97706"));
            } else if ("😮".equals(userRx)) {
                holder.btnReact.setText("😮 Wow");
                holder.btnReact.setTextColor(Color.parseColor("#D97706"));
            } else if ("😢".equals(userRx)) {
                holder.btnReact.setText("😢 Sad");
                holder.btnReact.setTextColor(Color.parseColor("#D97706"));
            } else if ("😡".equals(userRx)) {
                holder.btnReact.setText("😡 Angry");
                holder.btnReact.setTextColor(Color.parseColor("#DC2626"));
            } else {
                holder.btnReact.setText(userRx + " React");
                holder.btnReact.setTextColor(Color.parseColor("#5C0099"));
            }
        } else {
            holder.btnReact.setText("React");
            holder.btnReact.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_secondary));
        }

        // Reaction Badge UI (e.g. ❤️ 😂 15)
        if (comment.getReactionCount() > 0) {
            holder.layoutReaction.setVisibility(View.VISIBLE);
            holder.tvCommentReactionIcon.setText(comment.getTopReactionEmojis());
            holder.tvCommentReactionCount.setText(String.valueOf(comment.getReactionCount()));

            // Tap reaction badge to view FB-style breakdown dialog!
            holder.layoutReaction.setOnClickListener(v -> showReactionDetailsDialog(v.getContext(), comment));
        } else {
            holder.layoutReaction.setVisibility(View.GONE);
        }

        // Action Buttons
        holder.btnReply.setOnClickListener(v -> {
            if (actionCallback != null) {
                actionCallback.onReplyClicked(comment);
            }
        });

        holder.btnReact.setOnClickListener(v -> showReactionPicker(v.getContext(), comment, holder));
        holder.btnReact.setOnLongClickListener(v -> {
            showReactionPicker(v.getContext(), comment, holder);
            return true;
        });

        // Load Replies
        loadReplies(holder, comment);
    }

    private void loadReplies(CommentViewHolder holder, Comment comment) {
        if (activeReplyListeners.containsKey(comment.getId())) return;

        holder.layoutReplies.removeAllViews();
        if (postId == null || postId.isEmpty() || comment.getId() == null || comment.getId().isEmpty()) {
            return;
        }

        ListenerRegistration reg = FirebaseFirestore.getInstance()
                .collection("posts").document(postId)
                .collection("comments").document(comment.getId())
                .collection("replies")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    holder.layoutReplies.removeAllViews();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String author = doc.getString("author");
                        String text = doc.getString("text");
                        Long ts = doc.getLong("timestamp");

                        View replyView = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.item_reply, holder.layoutReplies, false);
                        TextView tvReplyAuthor = replyView.findViewById(R.id.tvReplyAuthor);
                        TextView tvReplyTime = replyView.findViewById(R.id.tvReplyTime);
                        TextView tvReplyText = replyView.findViewById(R.id.tvReplyText);

                        tvReplyAuthor.setText(author != null ? author : "Student Author");
                        tvReplyText.setText(text != null ? text : "");
                        tvReplyTime.setText(TimeUtils.getRelativeTimeString(
                                holder.itemView.getContext(),
                                ts != null ? ts : System.currentTimeMillis(), "Reply"));

                        holder.layoutReplies.addView(replyView);
                    }
                });
        replyListeners.add(reg);
        activeReplyListeners.put(comment.getId(), reg);
    }

    private void showReactionPicker(Context context, Comment comment, CommentViewHolder holder) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reactions_horizontal, null);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setDimAmount(0.25f);
        }

        View container = dialogView.findViewById(R.id.reactionsContainer);
        if (container != null) {
            container.setScaleX(0.75f);
            container.setScaleY(0.75f);
            container.setAlpha(0f);
            container.animate()
                    .scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(220)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.3f))
                    .start();
        }

        TextView tvReactLike = dialogView.findViewById(R.id.tvReactLike);
        TextView tvReactHeart = dialogView.findViewById(R.id.tvReactHeart);
        TextView tvReactCare = dialogView.findViewById(R.id.tvReactCare);
        TextView tvReactHaha = dialogView.findViewById(R.id.tvReactHaha);
        TextView tvReactWow = dialogView.findViewById(R.id.tvReactWow);
        TextView tvReactSad = dialogView.findViewById(R.id.tvReactSad);
        TextView tvReactAngry = dialogView.findViewById(R.id.tvReactAngry);

        final java.util.List<android.animation.Animator> activeAnimators = new java.util.ArrayList<>();
        TextView[] emojis = {tvReactLike, tvReactHeart, tvReactCare, tvReactHaha, tvReactWow, tvReactSad, tvReactAngry};

        // Staggered pop-in + continuous alive bobbing & breathing pulse ("mag lihok2")
        for (int i = 0; i < emojis.length; i++) {
            final TextView emojiView = emojis[i];
            if (emojiView == null) continue;
            final int index = i;

            emojiView.setScaleX(0f);
            emojiView.setScaleY(0f);
            emojiView.setTranslationY(24f);

            emojiView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(260)
                    .setStartDelay(index * 35)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(2.2f))
                    .withEndAction(() -> {
                        // 1. Continuous Bobbing Float Animation (lihok-lihok)
                        android.animation.ObjectAnimator bob = android.animation.ObjectAnimator.ofFloat(emojiView, "translationY", 0f, -6f, 0f);
                        bob.setDuration(950 + (index % 3) * 150);
                        bob.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                        bob.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                        bob.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
                        bob.setStartDelay((index * 80) % 360);
                        bob.start();
                        activeAnimators.add(bob);

                        // 2. Continuous Subtle Breathing Pulse
                        android.animation.ObjectAnimator pulseX = android.animation.ObjectAnimator.ofFloat(emojiView, "scaleX", 1f, 1.07f, 1f);
                        pulseX.setDuration(950 + (index % 3) * 150);
                        pulseX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                        pulseX.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                        pulseX.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
                        pulseX.setStartDelay((index * 80) % 360);
                        pulseX.start();
                        activeAnimators.add(pulseX);

                        android.animation.ObjectAnimator pulseY = android.animation.ObjectAnimator.ofFloat(emojiView, "scaleY", 1f, 1.07f, 1f);
                        pulseY.setDuration(950 + (index % 3) * 150);
                        pulseY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                        pulseY.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                        pulseY.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
                        pulseY.setStartDelay((index * 80) % 360);
                        pulseY.start();
                        activeAnimators.add(pulseY);
                    })
                    .start();

            // Touch zoom effect
            emojiView.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(1.45f).scaleY(1.45f).translationY(-12f).setDuration(120).start();
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).translationY(0f).setDuration(120).start();
                }
                return false;
            });
        }

        dialog.setOnDismissListener(d -> {
            for (android.animation.Animator a : activeAnimators) {
                if (a != null) a.cancel();
            }
            activeAnimators.clear();
        });

        View.OnClickListener reactionListener = v -> {
            String emoji = "👍";
            if (v == tvReactHeart) emoji = "❤️";
            else if (v == tvReactCare) emoji = "🥰";
            else if (v == tvReactHaha) emoji = "😆";
            else if (v == tvReactWow) emoji = "😮";
            else if (v == tvReactSad) emoji = "😢";
            else if (v == tvReactAngry) emoji = "😡";

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String uid = (currentUser != null && currentUser.getUid() != null) ? currentUser.getUid() : "guest_user";
            String userName = (currentUser != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().trim().isEmpty())
                    ? currentUser.getDisplayName()
                    : "BISU Student";

            final String finalEmoji = emoji;
            v.animate().scaleX(1.5f).scaleY(1.5f).setDuration(150).withEndAction(() -> {
                dialog.dismiss();
                if (postId != null && !postId.isEmpty() && comment.getId() != null && !comment.getId().isEmpty()) {

                    DocumentReference commentRef = FirebaseFirestore.getInstance()
                            .collection("posts").document(postId)
                            .collection("comments").document(comment.getId());

                    FirebaseFirestore.getInstance().runTransaction(transaction -> {
                        DocumentSnapshot snapshot = transaction.get(commentRef);
                        if (!snapshot.exists()) return null;

                        Map<String, Object> summaryRaw = (Map<String, Object>) snapshot.get("reactionsSummary");
                        Map<String, Long> summary = new HashMap<>();
                        if (summaryRaw != null) {
                            for (Map.Entry<String, Object> e : summaryRaw.entrySet()) {
                                if (e.getValue() instanceof Number) {
                                    summary.put(e.getKey(), ((Number) e.getValue()).longValue());
                                }
                            }
                        }

                        Map<String, Object> userRxsRaw = (Map<String, Object>) snapshot.get("userReactions");
                        Map<String, String> userRxs = new HashMap<>();
                        if (userRxsRaw != null) {
                            for (Map.Entry<String, Object> e : userRxsRaw.entrySet()) {
                                if (e.getValue() != null) userRxs.put(e.getKey(), e.getValue().toString());
                            }
                        }

                        Map<String, Object> userRxNamesRaw = (Map<String, Object>) snapshot.get("userReactionNames");
                        Map<String, String> userRxNames = new HashMap<>();
                        if (userRxNamesRaw != null) {
                            for (Map.Entry<String, Object> e : userRxNamesRaw.entrySet()) {
                                if (e.getValue() != null) userRxNames.put(e.getKey(), e.getValue().toString());
                            }
                        }

                        String previousRx = userRxs.get(uid);
                        long currentTotal = snapshot.getLong("reactionCount") != null ? snapshot.getLong("reactionCount") : 0;

                        if (finalEmoji.equals(previousRx)) {
                            // Tap same reaction -> unreact
                            userRxs.remove(uid);
                            userRxNames.remove(uid);
                            long oldC = summary.getOrDefault(previousRx, 1L);
                            if (oldC > 1) {
                                summary.put(previousRx, oldC - 1);
                            } else {
                                summary.remove(previousRx);
                            }
                            currentTotal = Math.max(0, currentTotal - 1);
                        } else {
                            if (previousRx != null && !previousRx.isEmpty()) {
                                long oldC = summary.getOrDefault(previousRx, 1L);
                                if (oldC > 1) {
                                    summary.put(previousRx, oldC - 1);
                                } else {
                                    summary.remove(previousRx);
                                }
                            } else {
                                currentTotal++;
                            }
                            long newC = summary.getOrDefault(finalEmoji, 0L);
                            summary.put(finalEmoji, newC + 1);

                            userRxs.put(uid, finalEmoji);
                            userRxNames.put(uid, userName);
                        }

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("reactionCount", currentTotal);
                        updates.put("reactionType", finalEmoji);
                        updates.put("reactionsSummary", summary);
                        updates.put("userReactions", userRxs);
                        updates.put("userReactionNames", userRxNames);

                        transaction.update(commentRef, updates);
                        return null;
                    }).addOnSuccessListener(aVoid -> {
                        // Also record in subcollection for permanence
                        Map<String, Object> subData = new HashMap<>();
                        subData.put("uid", uid);
                        subData.put("authorName", userName);
                        subData.put("reaction", finalEmoji);
                        subData.put("timestamp", FieldValue.serverTimestamp());

                        FirebaseFirestore.getInstance()
                                .collection("posts").document(postId)
                                .collection("comments").document(comment.getId())
                                .collection("reactions").document(uid)
                                .set(subData);

                        Toast.makeText(context, "Reacted " + finalEmoji, Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to react: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        };

        if (tvReactLike != null) tvReactLike.setOnClickListener(reactionListener);
        if (tvReactHeart != null) tvReactHeart.setOnClickListener(reactionListener);
        if (tvReactCare != null) tvReactCare.setOnClickListener(reactionListener);
        if (tvReactHaha != null) tvReactHaha.setOnClickListener(reactionListener);
        if (tvReactWow != null) tvReactWow.setOnClickListener(reactionListener);
        if (tvReactSad != null) tvReactSad.setOnClickListener(reactionListener);
        if (tvReactAngry != null) tvReactAngry.setOnClickListener(reactionListener);

        dialog.show();
    }

    private void showReactionDetailsDialog(Context context, Comment comment) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_comment_reactions_breakdown, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvBreakdownTitle);
        TextView btnDismiss = dialogView.findViewById(R.id.btnDismissBreakdown);
        LinearLayout llTabs = dialogView.findViewById(R.id.llBreakdownTabs);
        LinearLayout llUsersContainer = dialogView.findViewById(R.id.llBreakdownUsersContainer);
        TextView tvEmpty = dialogView.findViewById(R.id.tvBreakdownEmpty);

        int totalCount = comment.getReactionCount();
        tvTitle.setText("Reactions • " + totalCount);
        btnDismiss.setOnClickListener(v -> dialog.dismiss());

        Map<String, String> userRxs = new HashMap<>(comment.getUserReactions());
        Map<String, String> userRxNames = new HashMap<>(comment.getUserReactionNames());
        Map<String, Long> summary = new HashMap<>(comment.getReactionsSummary());

        List<String> tabKeys = new ArrayList<>();
        tabKeys.add("All");
        for (Map.Entry<String, Long> entry : summary.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                tabKeys.add(entry.getKey());
            }
        }
        if (tabKeys.size() == 1 && totalCount > 0 && !comment.getReactionType().isEmpty()) {
            tabKeys.add(comment.getReactionType());
        }

        final String[] activeFilter = new String[]{"All"};
        final List<TextView> tabViews = new ArrayList<>();

        Runnable renderUsers = () -> {
            llUsersContainer.removeAllViews();
            String filter = activeFilter[0];
            int visibleUsers = 0;

            for (Map.Entry<String, String> entry : userRxs.entrySet()) {
                String uid = entry.getKey();
                String emoji = entry.getValue();
                if ("All".equals(filter) || emoji.equals(filter)) {
                    visibleUsers++;
                    View userRow = LayoutInflater.from(context).inflate(R.layout.item_reaction_user, llUsersContainer, false);
                    TextView tvInitial = userRow.findViewById(R.id.tvReactionUserInitial);
                    TextView tvName = userRow.findViewById(R.id.tvReactionUserName);
                    TextView tvEmoji = userRow.findViewById(R.id.tvReactionUserEmoji);

                    String name = userRxNames.get(uid);
                    if (name == null || name.trim().isEmpty()) {
                        name = "BISU Student";
                    }
                    tvName.setText(name);
                    tvEmoji.setText(emoji);
                    String init = name.length() > 0 ? name.substring(0, 1).toUpperCase(Locale.getDefault()) : "?";
                    tvInitial.setText(init);

                    llUsersContainer.addView(userRow);
                }
            }

            if (visibleUsers == 0) {
                TextView tvSummaryRow = new TextView(context);
                tvSummaryRow.setPadding(24, 32, 24, 32);
                tvSummaryRow.setGravity(android.view.Gravity.CENTER);
                tvSummaryRow.setTextColor(context.getResources().getColor(R.color.text_secondary));
                tvSummaryRow.setTextSize(13);

                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Long> e : summary.entrySet()) {
                    if (e.getValue() != null && e.getValue() > 0) {
                        if (sb.length() > 0) sb.append("  •  ");
                        sb.append(e.getKey()).append(" ").append(e.getValue());
                    }
                }
                if (sb.length() == 0 && totalCount > 0) {
                    sb.append("❤️ ").append(totalCount);
                }
                tvSummaryRow.setText(sb.toString() + "\n(" + totalCount + " total reactions)");
                llUsersContainer.addView(tvSummaryRow);
            }
        };

        // Render Category Tabs
        for (String key : tabKeys) {
            TextView tab = new TextView(context);
            tab.setTextSize(12);
            tab.setPadding(28, 14, 28, 14);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd(12);
            tab.setLayoutParams(lp);

            if ("All".equals(key)) {
                tab.setText("All " + totalCount);
            } else {
                long c = summary.getOrDefault(key, 1L);
                tab.setText(key + " " + c);
            }

            tab.setClickable(true);
            tab.setFocusable(true);
            tabViews.add(tab);

            tab.setOnClickListener(v -> {
                activeFilter[0] = key;
                for (TextView t : tabViews) {
                    t.setBackgroundResource(R.drawable.bg_chip_unselected);
                    t.setTextColor(context.getResources().getColor(R.color.text_secondary));
                    t.setTypeface(null, Typeface.NORMAL);
                }
                tab.setBackgroundResource(R.drawable.bg_chip_selected);
                tab.setTextColor(context.getResources().getColor(R.color.white));
                tab.setTypeface(null, Typeface.BOLD);
                renderUsers.run();
            });

            llTabs.addView(tab);
        }

        // Highlight first tab by default
        if (!tabViews.isEmpty()) {
            tabViews.get(0).setBackgroundResource(R.drawable.bg_chip_selected);
            tabViews.get(0).setTextColor(context.getResources().getColor(R.color.white));
            tabViews.get(0).setTypeface(null, Typeface.BOLD);
        }

        renderUsers.run();

        // Also fetch from reactions subcollection to populate any extra user details
        if (postId != null && !postId.isEmpty() && comment.getId() != null && !comment.getId().isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("posts").document(postId)
                    .collection("comments").document(comment.getId())
                    .collection("reactions")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            boolean hasNew = false;
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                String u = doc.getString("uid");
                                String rx = doc.getString("reaction");
                                String nm = doc.getString("authorName");
                                if (u != null && rx != null) {
                                    if (!userRxs.containsKey(u)) {
                                        userRxs.put(u, rx);
                                        hasNew = true;
                                    }
                                    if (nm != null && !nm.isEmpty()) {
                                        userRxNames.put(u, nm);
                                    }
                                }
                            }
                            if (hasNew) {
                                renderUsers.run();
                            }
                        }
                    });
        }

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvCommentAuthor, tvCommentTime, tvCommentText, btnReply, btnReact, tvCommentReactionIcon, tvCommentReactionCount;
        LinearLayout layoutReaction, layoutReplies;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCommentAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvCommentTime = itemView.findViewById(R.id.tvCommentTime);
            tvCommentText = itemView.findViewById(R.id.tvCommentText);
            btnReply = itemView.findViewById(R.id.btnReply);
            btnReact = itemView.findViewById(R.id.btnReact);
            tvCommentReactionIcon = itemView.findViewById(R.id.tvCommentReactionIcon);
            tvCommentReactionCount = itemView.findViewById(R.id.tvCommentReactionCount);
            layoutReaction = itemView.findViewById(R.id.layoutReaction);
            layoutReplies = itemView.findViewById(R.id.layoutReplies);
        }
    }
}
