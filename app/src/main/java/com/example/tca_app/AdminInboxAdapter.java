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

public class AdminInboxAdapter extends RecyclerView.Adapter<AdminInboxAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(ChatConversation conversation);
    }

    private final List<ChatConversation> conversationList;
    private final OnConversationClickListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

    public AdminInboxAdapter(List<ChatConversation> conversationList, OnConversationClickListener listener) {
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

        // Email
        String email = item.getStudentEmail();
        if (email != null && !email.isEmpty()) {
            holder.tvStudentEmail.setText(email);
            holder.tvStudentEmail.setVisibility(View.VISIBLE);
        } else {
            holder.tvStudentEmail.setVisibility(View.GONE);
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
        if (ts > 0) {
            long now = System.currentTimeMillis();
            if (DateUtils.isToday(ts)) {
                holder.tvInquiryTimestamp.setText(timeFormat.format(new Date(ts)));
            } else if (now - ts < 48 * 60 * 60 * 1000L) {
                holder.tvInquiryTimestamp.setText("Yesterday");
            } else {
                holder.tvInquiryTimestamp.setText(dateFormat.format(new Date(ts)));
            }
        } else {
            holder.tvInquiryTimestamp.setText("");
        }

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
        TextView tvStudentEmail;
        TextView tvInquirySnippet;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentAvatarInitials = itemView.findViewById(R.id.tvStudentAvatarInitials);
            dotUnreadInquiry = itemView.findViewById(R.id.dotUnreadInquiry);
            tvStudentInquiryName = itemView.findViewById(R.id.tvStudentInquiryName);
            tvInquiryTimestamp = itemView.findViewById(R.id.tvInquiryTimestamp);
            tvStudentEmailBadge = itemView.findViewById(R.id.tvStudentEmailBadge);
            tvStudentEmail = itemView.findViewById(R.id.tvStudentEmail);
            tvInquirySnippet = itemView.findViewById(R.id.tvInquirySnippet);
        }
    }
}
