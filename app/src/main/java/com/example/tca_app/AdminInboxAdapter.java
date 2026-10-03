package com.example.tca_app;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.widget.ImageView;

public class AdminInboxAdapter extends RecyclerView.Adapter<AdminInboxAdapter.ViewHolder> {

    public interface OnConversationActionListener {
        void onConversationClick(ChatConversation conversation);
        void onConversationOptionsClick(ChatConversation conversation, int position, View anchorView);
    }

    private final List<ChatConversation> conversationList;
    private final OnConversationActionListener listener;

    public AdminInboxAdapter(List<ChatConversation> conversationList, OnConversationActionListener listener) {
        this.conversationList = conversationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inquiry_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatConversation item = conversationList.get(position);

        String name = item.getStudentName();
        holder.tvStudentInquiryName.setText(name);

        // Initials avatar
        String initial = "S";
        if (name != null && !name.trim().isEmpty()) {
            initial = String.valueOf(name.trim().charAt(0)).toUpperCase(Locale.getDefault());
        }
        holder.tvStudentAvatarInitials.setText(initial);

        // Student email is kept hidden for student privacy
        if (holder.tvStudentEmail != null) {
            holder.tvStudentEmail.setVisibility(View.GONE);
        }

        // Badges for Blocked and Archived status
        if (holder.tvConversationBlockedBadge != null) {
            holder.tvConversationBlockedBadge.setVisibility(item.isBlocked() ? View.VISIBLE : View.GONE);
        }
        if (holder.tvConversationArchivedBadge != null) {
            holder.tvConversationArchivedBadge.setVisibility(item.isArchived() ? View.VISIBLE : View.GONE);
        }

        // Message snippet
        String lastMsg = item.getLastMessage();
        if ("ADMIN".equalsIgnoreCase(item.getLastSenderRole())) {
            holder.tvInquirySnippet.setText("You: " + lastMsg);
        } else {
            holder.tvInquirySnippet.setText(lastMsg);
        }

        // Formatted timestamp
        long ts = item.getLastMessageTimestamp();
        holder.tvInquiryTimestamp.setText(TimeUtils.getInquiryTimeString(ts));

        // Unread indicator dot (shows if last sender was student)
        if (item.isUnread()) {
            holder.dotUnreadInquiry.setVisibility(View.VISIBLE);
            holder.tvStudentInquiryName.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.tvInquirySnippet.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_primary, null));
        } else {
            holder.dotUnreadInquiry.setVisibility(View.GONE);
            holder.tvStudentInquiryName.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.tvInquirySnippet.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_secondary, null));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onConversationOptionsClick(item, holder.getAdapterPosition(), v);
            }
            return true;
        });

        if (holder.btnConversationMore != null) {
            holder.btnConversationMore.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConversationOptionsClick(item, holder.getAdapterPosition(), v);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentAvatarInitials;
        View dotUnreadInquiry;
        TextView tvStudentInquiryName;
        TextView tvInquiryTimestamp;
        TextView tvStudentEmailBadge;
        TextView tvConversationBlockedBadge;
        TextView tvConversationArchivedBadge;
        TextView tvStudentEmail;
        TextView tvInquirySnippet;
        ImageView btnConversationMore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentAvatarInitials = itemView.findViewById(R.id.tvStudentAvatarInitials);
            dotUnreadInquiry = itemView.findViewById(R.id.dotUnreadInquiry);
            tvStudentInquiryName = itemView.findViewById(R.id.tvStudentInquiryName);
            tvInquiryTimestamp = itemView.findViewById(R.id.tvInquiryTimestamp);
            tvStudentEmailBadge = itemView.findViewById(R.id.tvStudentEmailBadge);
            tvConversationBlockedBadge = itemView.findViewById(R.id.tvConversationBlockedBadge);
            tvConversationArchivedBadge = itemView.findViewById(R.id.tvConversationArchivedBadge);
            tvStudentEmail = itemView.findViewById(R.id.tvStudentEmail);
            tvInquirySnippet = itemView.findViewById(R.id.tvInquirySnippet);
            btnConversationMore = itemView.findViewById(R.id.btnConversationMore);
        }
    }
}
