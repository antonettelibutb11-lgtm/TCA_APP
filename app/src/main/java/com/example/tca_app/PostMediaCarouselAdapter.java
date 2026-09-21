package com.example.tca_app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PostMediaCarouselAdapter extends RecyclerView.Adapter<PostMediaCarouselAdapter.MediaViewHolder> {

    private final Context context;
    private final List<String> mediaUris;

    public PostMediaCarouselAdapter(Context context, List<String> mediaUris) {
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
        String mediaUrl = mediaUris.get(position);
        boolean isVideo = mediaUrl != null && mediaUrl.contains("/video/");

        if (isVideo) {
            holder.ivCarouselPlayVideo.setVisibility(View.VISIBLE);
        } else {
            holder.ivCarouselPlayVideo.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(mediaUrl)
                .into(holder.ivCarouselImage);

        holder.itemView.setOnClickListener(v -> {
            if (isVideo) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mediaUrl));
                intent.setDataAndType(Uri.parse(mediaUrl), "video/*");
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mediaUris != null ? mediaUris.size() : 0;
    }

    public static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCarouselImage;
        ImageView ivCarouselPlayVideo;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCarouselImage = itemView.findViewById(R.id.ivCarouselImage);
            ivCarouselPlayVideo = itemView.findViewById(R.id.ivCarouselPlayVideo);
        }
    }
}
