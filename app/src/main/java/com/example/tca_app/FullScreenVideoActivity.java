package com.example.tca_app;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class FullScreenVideoActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
                
        setContentView(R.layout.activity_full_screen_video);

        playerView = findViewById(R.id.videoViewFullScreen);
        ImageView btnClose = findViewById(R.id.btnCloseVideo);
        ProgressBar progressBar = findViewById(R.id.progressBarVideo);

        String videoUriStr = getIntent().getStringExtra("videoUri");

        if (videoUriStr != null && !videoUriStr.isEmpty()) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
            
            Uri videoUri = Uri.parse(videoUriStr);
            MediaItem mediaItem = MediaItem.fromUri(videoUri);
            player.setMediaItem(mediaItem);
            
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_READY) {
                        progressBar.setVisibility(View.GONE);
                    } else if (playbackState == Player.STATE_BUFFERING) {
                        progressBar.setVisibility(View.VISIBLE);
                    } else if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                        progressBar.setVisibility(View.GONE);
                    }
                }
            });
            
            player.prepare();
            player.play();
        } else {
            progressBar.setVisibility(View.GONE);
        }

        btnClose.setOnClickListener(v -> finish());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
