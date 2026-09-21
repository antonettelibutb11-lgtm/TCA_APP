package com.example.tca_app;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class VideoScrollHelper {

    public static void attachToRecyclerView(RecyclerView recyclerView) {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                handleVideoVisibility(recyclerView);
            }
        });
    }

    public static void handleVideoVisibility(RecyclerView recyclerView) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) return;

        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        int lastVisible = layoutManager.findLastVisibleItemPosition();
        
        if (firstVisible < 0 || lastVisible < 0) return;

        for (int i = 0; i < layoutManager.getChildCount(); i++) {
            View child = layoutManager.getChildAt(i);
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);

            if (holder instanceof PostAdapter.PostViewHolder) {
                PostAdapter.PostViewHolder postHolder = (PostAdapter.PostViewHolder) holder;
                
                if (postHolder.layoutSingleVideo != null && postHolder.layoutSingleVideo.getVisibility() == View.VISIBLE && postHolder.vvSingleVideo != null) {
                    
                    Rect scrollBounds = new Rect();
                    recyclerView.getHitRect(scrollBounds);
                    
                    if (child.getLocalVisibleRect(scrollBounds)) {
                        // The item is at least partially visible
                        float visibleHeight = scrollBounds.height();
                        float totalHeight = child.getHeight();
                        
                        // If it's more than 50% visible, play it
                        if (visibleHeight / totalHeight > 0.5f) {
                            if (!postHolder.vvSingleVideo.isPlaying()) {
                                postHolder.vvSingleVideo.start();
                            }
                        } else {
                            // Less than 50% visible, pause it
                            if (postHolder.vvSingleVideo.isPlaying()) {
                                postHolder.vvSingleVideo.pause();
                            }
                        }
                    } else {
                        // Completely invisible
                        if (postHolder.vvSingleVideo.isPlaying()) {
                            postHolder.vvSingleVideo.pause();
                        }
                    }
                }
            }
        }
    }
}
