package com.example.tca_app;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MediaViewerAdapter extends RecyclerView.Adapter<MediaViewerAdapter.ViewerHolder> {

    private final Context context;
    private final List<String> mediaUris;

    public MediaViewerAdapter(Context context, List<String> mediaUris) {
        this.context = context;
        this.mediaUris = mediaUris;
    }

    @NonNull
    @Override
    public ViewerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_media_fullscreen, parent, false);
        return new ViewerHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewerHolder holder, int position) {
        String uriStr = mediaUris.get(position);
        boolean isVideo = uriStr != null && (uriStr.contains(".mp4") || uriStr.contains("video"));
        // A simple check. Ideally we resolve mime type.

        if (isVideo) {
            holder.ivImage.setVisibility(View.GONE);
            holder.vvVideo.setVisibility(View.VISIBLE);
            holder.vvVideo.setVideoURI(Uri.parse(uriStr));
            holder.vvVideo.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                // holder.vvVideo.start();
            });
        } else {
            holder.vvVideo.setVisibility(View.GONE);
            holder.ivImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(uriStr)
                    .fitCenter()
                    .into(holder.ivImage);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewerHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder.vvVideo != null && holder.vvVideo.getVisibility() == View.VISIBLE) {
            holder.vvVideo.start();
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewerHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder.vvVideo != null) {
            if (holder.vvVideo.getVisibility() == View.VISIBLE) {
                holder.vvVideo.pause();
            }
            // Release hardware MediaPlayer resources when scrolled off-screen
            holder.vvVideo.suspend();
            holder.vvVideo.stopPlayback();
        }
    }

    @Override
    public int getItemCount() {
        return mediaUris.size();
    }

    public static class ViewerHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        VideoView vvVideo;

        public ViewerHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivFullscreenImage);
            vvVideo = itemView.findViewById(R.id.vvFullscreenVideo);
        }
    }
}
