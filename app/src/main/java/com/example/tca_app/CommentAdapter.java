package com.example.tca_app;

import android.app.AlertDialog;
import android.content.Context;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        // Iterate securely through all tracking lists/maps and explicitly unregister background listeners
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

        // Reaction UI
        if (comment.getReactionCount() > 0) {
            holder.layoutReaction.setVisibility(View.VISIBLE);
            holder.tvCommentReactionIcon.setText(comment.getReactionType().isEmpty() ? "❤️" : comment.getReactionType());
            holder.tvCommentReactionCount.setText(String.valueOf(comment.getReactionCount()));
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
        // Prevent overlapping listener leaks for the same comment
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
        }

        TextView tvReactLike = dialogView.findViewById(R.id.tvReactLike);
        TextView tvReactHeart = dialogView.findViewById(R.id.tvReactHeart);
        TextView tvReactCare = dialogView.findViewById(R.id.tvReactCare);
        TextView tvReactHaha = dialogView.findViewById(R.id.tvReactHaha);
        TextView tvReactWow = dialogView.findViewById(R.id.tvReactWow);
        TextView tvReactAngry = dialogView.findViewById(R.id.tvReactAngry);

        TextView[] emojis = {tvReactLike, tvReactHeart, tvReactCare, tvReactHaha, tvReactWow, tvReactAngry};
        for (int i = 0; i < emojis.length; i++) {
            if (emojis[i] != null) {
                emojis[i].setScaleX(0f);
                emojis[i].setScaleY(0f);
                emojis[i].animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(250)
                        .setStartDelay(i * 30)
                        .setInterpolator(new android.view.animation.OvershootInterpolator())
                        .start();
            }
        }

        View.OnClickListener reactionListener = v -> {
            String emoji = "👍";
            if (v == tvReactHeart) emoji = "❤️";
            else if (v == tvReactCare) emoji = "🤗";
            else if (v == tvReactHaha) emoji = "😂";
            else if (v == tvReactWow) emoji = "😮";
            else if (v == tvReactAngry) emoji = "😡";

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String uid = (currentUser != null && currentUser.getUid() != null) ? currentUser.getUid() : "guest_user";

            final String finalEmoji = emoji;
            v.animate().scaleX(1.5f).scaleY(1.5f).setDuration(150).withEndAction(() -> {
                dialog.dismiss();
                if (postId != null && !postId.isEmpty() && comment.getId() != null && !comment.getId().isEmpty()) {
                    Map<String, Object> reactionData = new HashMap<>();
                    reactionData.put("uid", uid);
                    reactionData.put("reaction", finalEmoji);
                    reactionData.put("timestamp", FieldValue.serverTimestamp());

                    FirebaseFirestore.getInstance()
                            .collection("posts").document(postId)
                            .collection("comments").document(comment.getId())
                            .collection("reactions").document(uid)
                            .set(reactionData)
                            .addOnSuccessListener(aVoid -> {
                                FirebaseFirestore.getInstance()
                                        .collection("posts").document(postId)
                                        .collection("comments").document(comment.getId())
                                        .update("reactionCount", FieldValue.increment(1),
                                                "reactionType", finalEmoji);
                                Toast.makeText(context, "Reacted " + finalEmoji, Toast.LENGTH_SHORT).show();
                            });
                }
            }).start();
        };

        if (tvReactLike != null) tvReactLike.setOnClickListener(reactionListener);
        if (tvReactHeart != null) tvReactHeart.setOnClickListener(reactionListener);
        if (tvReactCare != null) tvReactCare.setOnClickListener(reactionListener);
        if (tvReactHaha != null) tvReactHaha.setOnClickListener(reactionListener);
        if (tvReactWow != null) tvReactWow.setOnClickListener(reactionListener);
        if (tvReactAngry != null) tvReactAngry.setOnClickListener(reactionListener);

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
