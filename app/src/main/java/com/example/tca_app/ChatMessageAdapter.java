package com.example.tca_app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private final List<ChatMessage> messageList;
    private final String currentUserId;
    private boolean isCurrentUserAdmin = false;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);

    public ChatMessageAdapter(List<ChatMessage> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    public void setCurrentUserAdmin(boolean admin) {
        this.isCurrentUserAdmin = admin;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage msg = messageList.get(position);
        Context context = holder.itemView.getContext();
        String formattedTime = timeFormat.format(new Date(msg.getTimestamp()));

        String text = msg.getText();
        String imageUrl = msg.getImageUrl();
        boolean hasImage = imageUrl != null && !imageUrl.trim().isEmpty();
        boolean hasText = text != null && !text.trim().isEmpty();

        String senderId = msg.getSenderId() != null ? msg.getSenderId().trim() : "";
        String myId = currentUserId != null ? currentUserId.trim() : "";
        boolean isSentByMe = (!myId.isEmpty() && senderId.equalsIgnoreCase(myId))
                || (isCurrentUserAdmin && "ADMIN".equalsIgnoreCase(msg.getSenderRole()));

        if (isSentByMe) {
            // Sent message (Right)
            holder.layoutSentMessage.setVisibility(View.VISIBLE);
            holder.layoutReceivedMessage.setVisibility(View.GONE);

            // Handle image
            if (hasImage) {
                holder.cardSentImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.bg_card_selected)
                        .into(holder.ivSentImage);

                holder.ivSentImage.setOnClickListener(v -> {
                    Intent intent = new Intent(context, FullScreenImageActivity.class);
                    intent.putExtra("photoUri", imageUrl);
                    context.startActivity(intent);
                });
            } else {
                holder.cardSentImage.setVisibility(View.GONE);
            }

            // Handle text
            if (hasText) {
                holder.tvSentText.setVisibility(View.VISIBLE);
                holder.tvSentText.setText(text);
            } else {
                holder.tvSentText.setVisibility(View.GONE);
            }

            holder.tvSentTime.setText(formattedTime);
        } else {
            // Received message (Left)
            holder.layoutReceivedMessage.setVisibility(View.VISIBLE);
            holder.layoutSentMessage.setVisibility(View.GONE);

            holder.tvReceivedSender.setText(msg.getSenderName());

            // Handle image
            if (hasImage) {
                holder.cardReceivedImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.bg_card_selected)
                        .into(holder.ivReceivedImage);

                holder.ivReceivedImage.setOnClickListener(v -> {
                    Intent intent = new Intent(context, FullScreenImageActivity.class);
                    intent.putExtra("photoUri", imageUrl);
                    context.startActivity(intent);
                });
            } else {
                holder.cardReceivedImage.setVisibility(View.GONE);
            }

            // Handle text
            if (hasText) {
                holder.tvReceivedText.setVisibility(View.VISIBLE);
                holder.tvReceivedText.setText(text);
            } else {
                holder.tvReceivedText.setVisibility(View.GONE);
            }

            holder.tvReceivedTime.setText(formattedTime);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutSentMessage, layoutReceivedMessage;
        CardView cardSentImage, cardReceivedImage;
        ImageView ivSentImage, ivReceivedImage;
        TextView tvSentText, tvSentTime, tvReceivedSender, tvReceivedText, tvReceivedTime;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutSentMessage = itemView.findViewById(R.id.layoutSentMessage);
            layoutReceivedMessage = itemView.findViewById(R.id.layoutReceivedMessage);

            cardSentImage = itemView.findViewById(R.id.cardSentImage);
            cardReceivedImage = itemView.findViewById(R.id.cardReceivedImage);
            ivSentImage = itemView.findViewById(R.id.ivSentImage);
            ivReceivedImage = itemView.findViewById(R.id.ivReceivedImage);

            tvSentText = itemView.findViewById(R.id.tvSentText);
            tvSentTime = itemView.findViewById(R.id.tvSentTime);
            tvReceivedSender = itemView.findViewById(R.id.tvReceivedSender);
            tvReceivedText = itemView.findViewById(R.id.tvReceivedText);
            tvReceivedTime = itemView.findViewById(R.id.tvReceivedTime);
        }
    }
}
