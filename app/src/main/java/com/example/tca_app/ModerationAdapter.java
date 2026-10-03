package com.example.tca_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModerationAdapter extends RecyclerView.Adapter<ModerationAdapter.ModerationViewHolder> {

    private List<ModerationItem> moderationList;

    public ModerationAdapter(List<ModerationItem> moderationList) {
        this.moderationList = moderationList;
    }

    @NonNull
    @Override
    public ModerationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_moderation, parent, false);
        return new ModerationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModerationViewHolder holder, int position) {
        ModerationItem item = moderationList.get(position);
        holder.tvReason.setText("Reason: " + item.getReason());
        holder.tvAiConfidence.setText("AI Confidence: " + item.getAiScore());
        holder.tvContentPreview.setText(item.getPreviewText());

        holder.btnWarn.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION && currentPos < moderationList.size()) {
                ModerationItem targetItem = moderationList.get(currentPos);
                setButtonsEnabled(holder, false);
                processModerationActionViaFunctions("WARN", targetItem.getId(), targetItem.getPostId(), targetItem.getReason(), v.getContext(), () -> {
                    int latestPos = holder.getAdapterPosition();
                    if (latestPos != RecyclerView.NO_POSITION && latestPos < moderationList.size()) {
                        moderationList.remove(latestPos);
                        notifyItemRemoved(latestPos);
                        notifyItemRangeChanged(latestPos, moderationList.size());
                    }
                }, () -> setButtonsEnabled(holder, true));
            }
        });

        holder.btnRemove.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION && currentPos < moderationList.size()) {
                ModerationItem targetItem = moderationList.get(currentPos);
                setButtonsEnabled(holder, false);
                processModerationActionViaFunctions("DELETE", targetItem.getId(), targetItem.getPostId(), targetItem.getReason(), v.getContext(), () -> {
                    int latestPos = holder.getAdapterPosition();
                    if (latestPos != RecyclerView.NO_POSITION && latestPos < moderationList.size()) {
                        moderationList.remove(latestPos);
                        notifyItemRemoved(latestPos);
                        notifyItemRangeChanged(latestPos, moderationList.size());
                    }
                }, () -> setButtonsEnabled(holder, true));
            }
        });

        holder.btnApprove.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION && currentPos < moderationList.size()) {
                ModerationItem targetItem = moderationList.get(currentPos);
                setButtonsEnabled(holder, false);
                processModerationActionViaFunctions("APPROVE", targetItem.getId(), targetItem.getPostId(), targetItem.getReason(), v.getContext(), () -> {
                    int latestPos = holder.getAdapterPosition();
                    if (latestPos != RecyclerView.NO_POSITION && latestPos < moderationList.size()) {
                        moderationList.remove(latestPos);
                        notifyItemRemoved(latestPos);
                        notifyItemRangeChanged(latestPos, moderationList.size());
                    }
                }, () -> setButtonsEnabled(holder, true));
            }
        });
    }

    private void setButtonsEnabled(ModerationViewHolder holder, boolean enabled) {
        if (holder.btnWarn != null) holder.btnWarn.setEnabled(enabled);
        if (holder.btnRemove != null) holder.btnRemove.setEnabled(enabled);
        if (holder.btnApprove != null) holder.btnApprove.setEnabled(enabled);
    }

    private void processModerationActionViaFunctions(String action, String moderationId, String postId, String reason,
                                                    Context context, Runnable onSuccess, Runnable onFailure) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        
        if (moderationId != null && !moderationId.trim().isEmpty()) {
            batch.delete(db.collection("moderation_queue").document(moderationId));
        }

        if (postId != null && !postId.trim().isEmpty()) {
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            if ("APPROVE".equals(action) || "NONE".equals(action)) {
                updates.put("moderationStatus", "APPROVED");
                updates.put("status", "APPROVED");
                updates.put("isDuplicate", false);
                batch.set(db.collection("posts").document(postId), updates, com.google.firebase.firestore.SetOptions.merge());
            } else if ("DELETE".equals(action) || "REMOVE".equals(action)) {
                batch.delete(db.collection("posts").document(postId));
            } else if ("WARN".equals(action)) {
                updates.put("moderationStatus", "FLAGGED");
                updates.put("status", "FLAGGED");
                batch.set(db.collection("posts").document(postId), updates, com.google.firebase.firestore.SetOptions.merge());
            }
        }
        
        batch.commit()
                .addOnSuccessListener(v -> {
                    Toast.makeText(context, "Moderation action '" + action + "' processed.", Toast.LENGTH_SHORT).show();
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Action failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (onFailure != null) onFailure.run();
                });
    }

    @Override
    public int getItemCount() {
        return moderationList.size();
    }

    static class ModerationViewHolder extends RecyclerView.ViewHolder {
        TextView tvReason, tvAiConfidence, tvContentPreview;
        Button btnWarn, btnRemove, btnApprove;

        public ModerationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvAiConfidence = itemView.findViewById(R.id.tvAiConfidence);
            tvContentPreview = itemView.findViewById(R.id.tvContentPreview);
            btnWarn = itemView.findViewById(R.id.btnWarn);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            btnApprove = itemView.findViewById(R.id.btnApprove);
        }
    }
}
