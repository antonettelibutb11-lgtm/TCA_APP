package com.example.tca_app;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CreatePostMediaAdapter extends RecyclerView.Adapter<CreatePostMediaAdapter.MediaViewHolder> {

    private final Context context;
    private final List<Uri> mediaUris;

    public CreatePostMediaAdapter(Context context, List<Uri> mediaUris) {
        this.context = context;
        this.mediaUris = mediaUris;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_media, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        Uri uri = mediaUris.get(position);
        String mimeType = context.getContentResolver().getType(uri);
        boolean isVideo = mimeType != null && mimeType.startsWith("video");

        if (isVideo) {
            holder.ivPlayIcon.setVisibility(View.VISIBLE);
        } else {
            holder.ivPlayIcon.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(uri)
                .centerCrop()
                .override(400)
                .into(holder.ivCarouselImage);
    }

    @Override
    public int getItemCount() {
        return mediaUris.size();
    }

    public static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCarouselImage;
        ImageView ivPlayIcon;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCarouselImage = itemView.findViewById(R.id.ivCarouselImage);
            ivPlayIcon = itemView.findViewById(R.id.ivCarouselPlayVideo);
        }
    }
}
