package com.example.tca_app;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    public interface OnMessageActionListener {
        void onEditMessage(ChatMessage message, int position);
        void onUnsendMessage(ChatMessage message, int position);
    }

    private final List<ChatMessage> messageList;
    private final String currentUserId;
    private boolean isCurrentUserAdmin = false;
    private OnMessageActionListener actionListener;

    public ChatMessageAdapter(List<ChatMessage> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.actionListener = listener;
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
        String formattedTime = TimeUtils.getChatMessageTimeString(msg.getTimestamp());

        String text = msg.getText();
        String imageUrl = msg.getImageUrl();
        boolean hasImage = imageUrl != null && !imageUrl.trim().isEmpty();
        boolean hasText = text != null && !text.trim().isEmpty();
        boolean isUnsent = msg.isUnsent();
        boolean isEdited = msg.isEdited();

        String senderId = msg.getSenderId() != null ? msg.getSenderId().trim() : "";
        String myId = currentUserId != null ? currentUserId.trim() : "";
        boolean isSentByMe = !myId.isEmpty() && senderId.equalsIgnoreCase(myId);

        if (isSentByMe) {
            // Sent message (Right)
            holder.layoutSentMessage.setVisibility(View.VISIBLE);
            holder.layoutReceivedMessage.setVisibility(View.GONE);

            if (isUnsent) {
                holder.cardSentImage.setVisibility(View.GONE);
                holder.tvSentText.setVisibility(View.VISIBLE);
                holder.tvSentText.setText("You unsent a message");
                holder.tvSentText.setTypeface(null, Typeface.ITALIC);
                holder.tvSentText.setTextColor(Color.parseColor("#E0C2EC"));
                holder.tvSentEdited.setVisibility(View.GONE);
                holder.tvSentTime.setText(formattedTime);
                holder.layoutSentMessage.setOnClickListener(null);
                holder.layoutSentMessage.setOnLongClickListener(null);
            } else {
                holder.tvSentText.setTypeface(null, Typeface.NORMAL);
                holder.tvSentText.setTextColor(Color.WHITE);

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

                // Handle edited status
                holder.tvSentEdited.setVisibility(isEdited ? View.VISIBLE : View.GONE);
                holder.tvSentTime.setText(formattedTime);

                // Options dialog on click / long click
                View.OnLongClickListener optionsListener = v -> {
                    showMessageOptionsDialog(context, msg, holder.getAdapterPosition());
                    return true;
                };

                holder.layoutSentMessage.setOnLongClickListener(optionsListener);
                holder.layoutSentMessage.setOnClickListener(v -> {
                    // Quick option dialog for ease of access
                    showMessageOptionsDialog(context, msg, holder.getAdapterPosition());
                });
            }
        } else {
            // Received message (Left)
            holder.layoutReceivedMessage.setVisibility(View.VISIBLE);
            holder.layoutSentMessage.setVisibility(View.GONE);

            holder.tvReceivedSender.setText(msg.getSenderName());

            if (isUnsent) {
                holder.cardReceivedImage.setVisibility(View.GONE);
                holder.tvReceivedText.setVisibility(View.VISIBLE);
                holder.tvReceivedText.setText("This message was unsent");
                holder.tvReceivedText.setTypeface(null, Typeface.ITALIC);
                holder.tvReceivedText.setTextColor(Color.parseColor("#9A84AD"));
                holder.tvReceivedEdited.setVisibility(View.GONE);
                holder.tvReceivedTime.setText(formattedTime);
            } else {
                holder.tvReceivedText.setTypeface(null, Typeface.NORMAL);
                holder.tvReceivedText.setTextColor(context.getResources().getColor(R.color.text_secondary, null));

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

                holder.tvReceivedEdited.setVisibility(isEdited ? View.VISIBLE : View.GONE);
                holder.tvReceivedTime.setText(formattedTime);

                // Allow copying received text on long click
                holder.layoutReceivedMessage.setOnLongClickListener(v -> {
                    if (hasText) {
                        copyTextToClipboard(context, text);
                    }
                    return true;
                });
            }
        }
    }

    private void showMessageOptionsDialog(Context context, ChatMessage msg, int position) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_chat_message_actions, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View actionEditMessage = dialogView.findViewById(R.id.actionEditMessage);
        View actionCopyMessage = dialogView.findViewById(R.id.actionCopyMessage);
        View actionUnsendMessage = dialogView.findViewById(R.id.actionUnsendMessage);

        // Edit is only available for text messages
        boolean canEdit = "TEXT".equalsIgnoreCase(msg.getMessageType()) && msg.getText() != null && !msg.getText().trim().isEmpty();
        if (actionEditMessage != null) {
            actionEditMessage.setVisibility(canEdit ? View.VISIBLE : View.GONE);
            actionEditMessage.setOnClickListener(v -> {
                dialog.dismiss();
                if (actionListener != null) {
                    actionListener.onEditMessage(msg, position);
                }
            });
        }

        if (actionCopyMessage != null) {
            actionCopyMessage.setOnClickListener(v -> {
                dialog.dismiss();
                copyTextToClipboard(context, msg.getText());
            });
        }

        if (actionUnsendMessage != null) {
            actionUnsendMessage.setOnClickListener(v -> {
                dialog.dismiss();
                if (actionListener != null) {
                    actionListener.onUnsendMessage(msg, position);
                }
            });
        }

        View btnCancel = dialogView.findViewById(R.id.btnCancelMessageActions);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void copyTextToClipboard(Context context, String text) {
        if (text == null || text.trim().isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Chat Message", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Message copied to clipboard.", Toast.LENGTH_SHORT).show();
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
        TextView tvSentText, tvSentTime, tvSentEdited;
        TextView tvReceivedSender, tvReceivedText, tvReceivedTime, tvReceivedEdited;

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
            tvSentEdited = itemView.findViewById(R.id.tvSentEdited);

            tvReceivedSender = itemView.findViewById(R.id.tvReceivedSender);
            tvReceivedText = itemView.findViewById(R.id.tvReceivedText);
            tvReceivedTime = itemView.findViewById(R.id.tvReceivedTime);
            tvReceivedEdited = itemView.findViewById(R.id.tvReceivedEdited);
        }
    }
}
