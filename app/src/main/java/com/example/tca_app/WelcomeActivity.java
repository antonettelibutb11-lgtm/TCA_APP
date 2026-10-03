package com.example.tca_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Welcome & Onboarding Activity:
 * Acts as the entry screen allowing users to get started and navigate to Login / Main feed.
 * For returning authenticated users, displays a 5-second campus loading splash screen before launching feed.
 */
public class WelcomeActivity extends AppCompatActivity {

    private static final long SPLASH_LOADING_DURATION_MS = 5000L; // 5 seconds loading duration
    private Handler splashHandler;
    private Runnable navigateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView btnGetStarted = findViewById(R.id.btnGetStarted);
        LinearLayout layoutSplashLoading = findViewById(R.id.layoutSplashLoading);
        TextView tvSplashStatus = findViewById(R.id.tvSplashStatus);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // Returning authenticated user: Show 5-second loading splash before opening feed
            if (btnGetStarted != null) {
                btnGetStarted.setVisibility(View.GONE);
            }
            if (layoutSplashLoading != null) {
                layoutSplashLoading.setVisibility(View.VISIBLE);
            }

            splashHandler = new Handler(Looper.getMainLooper());

            if (tvSplashStatus != null) {
                tvSplashStatus.setText("Connecting to Campus Hub...");
                splashHandler.postDelayed(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        tvSplashStatus.setText("Loading feed & updates...");
                    }
                }, 1800);

                splashHandler.postDelayed(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        tvSplashStatus.setText("Welcome back!");
                    }
                }, 3800);
            }

            navigateRunnable = () -> {
                if (!isFinishing() && !isDestroyed()) {
                    Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            };
            splashHandler.postDelayed(navigateRunnable, SPLASH_LOADING_DURATION_MS);

        } else {
            // First-time or logged-out user: Show Get Started button
            if (layoutSplashLoading != null) {
                layoutSplashLoading.setVisibility(View.GONE);
            }
            if (btnGetStarted != null) {
                btnGetStarted.setVisibility(View.VISIBLE);
                btnGetStarted.setText(R.string.btn_get_started);
                btnGetStarted.setOnClickListener(v -> {
                    Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (splashHandler != null && navigateRunnable != null) {
            splashHandler.removeCallbacksAndMessages(null);
        }
    }
}
