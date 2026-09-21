package com.example.tca_app;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;

public class MediaViewerActivity extends AppCompatActivity {

    private ViewPager2 vpMediaGallery;
    private TextView tvGalleryCounter;
    private ImageView btnCloseGallery;
    private ArrayList<String> mediaUris;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_viewer);

        vpMediaGallery = findViewById(R.id.vpMediaGallery);
        tvGalleryCounter = findViewById(R.id.tvGalleryCounter);
        btnCloseGallery = findViewById(R.id.btnCloseGallery);

        mediaUris = getIntent().getStringArrayListExtra("media_uris");
        int startIndex = getIntent().getIntExtra("start_index", 0);

        if (mediaUris == null || mediaUris.isEmpty()) {
            finish();
            return;
        }

        // Set vertical scrolling
        vpMediaGallery.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        MediaViewerAdapter adapter = new MediaViewerAdapter(this, mediaUris);
        vpMediaGallery.setAdapter(adapter);
        
        vpMediaGallery.setCurrentItem(startIndex, false);
        updateCounter(startIndex);

        vpMediaGallery.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateCounter(position);
            }
        });

        btnCloseGallery.setOnClickListener(v -> finish());
    }

    private void updateCounter(int position) {
        if (mediaUris != null) {
            tvGalleryCounter.setText((position + 1) + " of " + mediaUris.size());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (vpMediaGallery != null) {
            for (int i = 0; i < vpMediaGallery.getChildCount(); i++) {
                android.view.View child = vpMediaGallery.getChildAt(i);
                if (child instanceof androidx.recyclerview.widget.RecyclerView) {
                    androidx.recyclerview.widget.RecyclerView rv = (androidx.recyclerview.widget.RecyclerView) child;
                    for (int j = 0; j < rv.getChildCount(); j++) {
                        android.view.View item = rv.getChildAt(j);
                        android.widget.VideoView vv = item.findViewById(R.id.vvFullscreenVideo);
                        if (vv != null && vv.isPlaying()) {
                            vv.pause();
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vpMediaGallery != null) {
            for (int i = 0; i < vpMediaGallery.getChildCount(); i++) {
                android.view.View child = vpMediaGallery.getChildAt(i);
                if (child instanceof androidx.recyclerview.widget.RecyclerView) {
                    androidx.recyclerview.widget.RecyclerView rv = (androidx.recyclerview.widget.RecyclerView) child;
                    for (int j = 0; j < rv.getChildCount(); j++) {
                        android.view.View item = rv.getChildAt(j);
                        android.widget.VideoView vv = item.findViewById(R.id.vvFullscreenVideo);
                        if (vv != null) {
                            vv.stopPlayback();
                            vv.suspend();
                        }
                    }
                }
            }
            vpMediaGallery.setAdapter(null);
        }
    }
}
