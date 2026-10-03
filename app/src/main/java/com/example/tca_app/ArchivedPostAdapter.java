package com.example.tca_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ArchivedPostAdapter extends RecyclerView.Adapter<ArchivedPostAdapter.ArchivedViewHolder> {

    public interface OnArchivedPostActionListener {
        void onRestorePost(Post post, int position);
        void onDeletePost(Post post, int position);
    }

    private final Context context;
    private final List<Post> postList;
    private final OnArchivedPostActionListener actionListener;

    public ArchivedPostAdapter(Context context, List<Post> postList, OnArchivedPostActionListener actionListener) {
        this.context = context;
        this.postList = postList;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ArchivedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_archived_post, parent, false);
        return new ArchivedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArchivedViewHolder holder, int position) {
        Post post = postList.get(position);
        if (post == null) return;

        // Author Name
        String author = post.getAuthorName();
        holder.tvAuthorName.setText(author != null && !author.isEmpty() ? author : "BISU Community");

        // Relative time and category
        String category = post.getCategory() != null && !post.getCategory().isEmpty() ? post.getCategory() : "General";
        String timeStr = TimeUtils.getRelativeTimeString(context, post.getTimestamp(), category);
        holder.tvPostMeta.setText(timeStr);

        // Content
        String content = post.getContent();
        if (content != null && !content.trim().isEmpty()) {
            holder.tvPostContent.setVisibility(View.VISIBLE);
            holder.tvPostContent.setText(content);
        } else {
            holder.tvPostContent.setVisibility(View.GONE);
        }

        // Media Preview
        List<String> mediaList = post.getMediaUris();
        String primaryPhoto = post.getPhotoUri();
        String displayUri = null;

        if (primaryPhoto != null && !primaryPhoto.isEmpty()) {
            displayUri = primaryPhoto;
        } else if (mediaList != null && !mediaList.isEmpty()) {
            displayUri = mediaList.get(0);
        }

        if (displayUri != null && !displayUri.isEmpty()) {
            holder.layoutMediaPreview.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(displayUri)
                    .placeholder(R.drawable.ic_cover_banner)
                    .error(R.drawable.ic_cover_banner)
                    .centerCrop()
                    .into(holder.ivPostMedia);

            int totalMedia = mediaList != null ? mediaList.size() : 1;
            if (totalMedia > 1) {
                holder.tvExtraMediaCount.setVisibility(View.VISIBLE);
                holder.tvExtraMediaCount.setText("+" + (totalMedia - 1) + " more");
            } else {
                holder.tvExtraMediaCount.setVisibility(View.GONE);
            }
        } else {
            holder.layoutMediaPreview.setVisibility(View.GONE);
        }

        // Actions
        holder.btnRestorePost.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onRestorePost(post, holder.getAdapterPosition());
            }
        });

        holder.btnDeletePost.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDeletePost(post, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class ArchivedViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAuthorAvatar;
        TextView tvAuthorName;
        TextView tvPostMeta;
        TextView tvArchivedBadge;
        TextView tvPostContent;
        FrameLayout layoutMediaPreview;
        ImageView ivPostMedia;
        TextView tvExtraMediaCount;
        View btnRestorePost;
        View btnDeletePost;

        public ArchivedViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAuthorAvatar = itemView.findViewById(R.id.ivAuthorAvatar);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            tvPostMeta = itemView.findViewById(R.id.tvPostMeta);
            tvArchivedBadge = itemView.findViewById(R.id.tvArchivedBadge);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            layoutMediaPreview = itemView.findViewById(R.id.layoutMediaPreview);
            ivPostMedia = itemView.findViewById(R.id.ivPostMedia);
            tvExtraMediaCount = itemView.findViewById(R.id.tvExtraMediaCount);
            btnRestorePost = itemView.findViewById(R.id.btnRestorePost);
            btnDeletePost = itemView.findViewById(R.id.btnDeletePost);
        }
    }
}
