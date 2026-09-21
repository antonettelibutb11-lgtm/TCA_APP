package com.example.tca_app;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class GridMediaAdapter extends RecyclerView.Adapter<GridMediaAdapter.MediaViewHolder> {

    public static class GridItem {
        public String uri;
        public boolean isVideo;
        public Post parentPost;
        
        public GridItem(String uri, boolean isVideo, Post parentPost) {
            this.uri = uri;
            this.isVideo = isVideo;
            this.parentPost = parentPost;
        }
    }

    private List<GridItem> flattenedMedia = new ArrayList<>();
    private boolean isVideoGrid;

    public GridMediaAdapter(List<Post> mediaList, boolean isVideoGrid) {
        this.isVideoGrid = isVideoGrid;
        setMediaList(mediaList);
    }

    public void setMediaList(List<Post> mediaList) {
        flattenedMedia.clear();
        if (mediaList != null) {
            for (Post post : mediaList) {
                if (isVideoGrid) {
                    // Check dedicated videoUri field
                    if (post.getVideoUri() != null && !post.getVideoUri().isEmpty()) {
                        flattenedMedia.add(new GridItem(post.getVideoUri(), true, post));
                    } else {
                        // Also check photoUri in case it was stored wrong (old posts)
                        String photo = post.getPhotoUri();
                        if (photo != null && (photo.contains("/video/") || photo.endsWith(".mp4") || photo.contains(".mp4?"))) {
                            flattenedMedia.add(new GridItem(photo, true, post));
                        } else if (post.getMediaUris() != null) {
                            for (String uri : post.getMediaUris()) {
                                if (uri != null && (uri.contains(".mp4") || uri.contains("/video/"))) {
                                    flattenedMedia.add(new GridItem(uri, true, post));
                                }
                            }
                        }
                    }
                } else {
                    // Gallery grid is strictly for pictures / photos!
                    List<String> uris = post.getMediaUris();
                    if (uris != null && !uris.isEmpty()) {
                        for (String uri : uris) {
                            if (uri != null && !uri.contains(".mp4") && !uri.contains("/video/")) {
                                flattenedMedia.add(new GridItem(uri, false, post));
                            }
                        }
                    } else if (post.getPhotoUri() != null && !post.getPhotoUri().isEmpty()) {
                        String photo = post.getPhotoUri();
                        if (!photo.contains(".mp4") && !photo.contains("/video/")) {
                            flattenedMedia.add(new GridItem(photo, false, post));
                        }
                    }
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grid_media, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        GridItem item = flattenedMedia.get(position);
        
        String urlToLoad = item.uri;
        if (urlToLoad != null && !urlToLoad.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                 .load(urlToLoad)
                 .placeholder(R.color.divider_color)
                 .into(holder.ivGridMedia);
        } else {
            holder.ivGridMedia.setImageResource(R.color.divider_color);
        }
        
        if (item.isVideo) {
            holder.ivPlayIcon.setVisibility(View.VISIBLE);
        } else {
            holder.ivPlayIcon.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (!item.isVideo) {
                // Determine which index this image represents within its parent post
                List<String> urisToPass = new ArrayList<>();
                int startIndex = 0;
                
                List<String> postUris = item.parentPost.getMediaUris();
                if (postUris != null && !postUris.isEmpty()) {
                    urisToPass.addAll(postUris);
                    startIndex = postUris.indexOf(item.uri);
                    if (startIndex == -1) startIndex = 0;
                } else {
                    urisToPass.add(item.uri);
                }

                Intent intent = new Intent(v.getContext(), MediaViewerActivity.class);
                intent.putStringArrayListExtra("media_uris", new ArrayList<>(urisToPass));
                intent.putExtra("start_index", startIndex);
                v.getContext().startActivity(intent);
            } else {
                Intent intent = new Intent(v.getContext(), FullScreenVideoActivity.class);
                intent.putExtra("videoUri", item.uri);
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return flattenedMedia.size();
    }

    static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView ivGridMedia;
        ImageView ivPlayIcon;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGridMedia = itemView.findViewById(R.id.ivGridMedia);
            ivPlayIcon = itemView.findViewById(R.id.ivPlayIcon);
        }
    }
}
