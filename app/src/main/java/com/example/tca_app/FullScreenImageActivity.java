package com.example.tca_app;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide title bar and make full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
                
        setContentView(R.layout.activity_full_screen_image);

        ImageView ivFullScreenImage = findViewById(R.id.ivFullScreenImage);
        ImageView btnClose = findViewById(R.id.btnClose);
        TextView tvCounter = findViewById(R.id.tvCounter);

        String photoUri = getIntent().getStringExtra("photoUri");
        int position = getIntent().getIntExtra("position", 0);
        int total = getIntent().getIntExtra("total", 0);

        if (photoUri != null) {
            if (photoUri.equals("default_logo")) {
                Glide.with(this)
                     .load(R.drawable.ic_bisu_logo_hd)
                     .into(ivFullScreenImage);
            } else {
                Glide.with(this)
                     .load(photoUri)
                     .into(ivFullScreenImage);
            }
        }

        if (total > 0) {
            tvCounter.setText((position + 1) + " of " + total);
        } else {
            tvCounter.setText("");
        }

        ImageView btnDownloadImage = findViewById(R.id.btnDownloadImage);
        if (btnDownloadImage != null) {
            btnDownloadImage.setOnClickListener(v -> {
                if (photoUri != null && !photoUri.isEmpty() && !photoUri.equals("default_logo")) {
                    String fileName = "TCA_Photo_" + System.currentTimeMillis() + ".jpg";
                    MediaDownloadHelper.downloadFile(this, photoUri, fileName, "image/jpeg");
                } else {
                    android.widget.Toast.makeText(this, "Cannot download default placeholder logo.", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }

        btnClose.setOnClickListener(v -> finish());
    }
}
