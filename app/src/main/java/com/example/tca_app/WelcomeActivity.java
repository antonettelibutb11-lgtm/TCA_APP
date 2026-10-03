package com.example.tca_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Welcome & Entry Splash Activity:
 * - Freshly installed on phone: Shows clean branding with immediate "Continue" button,
 *   going straight to Login without any loading delay or returning prompt.
 * - Returning users / app refresh: Displays dynamic connection sequence
 *   ("Connecting to The Campus Access Hub...", "Loading feed & campus updates..."),
 *   then displays "Welcome back!", after which the "Continue" button appears for them to tap.
 */
public class WelcomeActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "tca_launch_preferences";
    private static final String KEY_HAS_COMPLETED_FIRST_RUN = "has_completed_first_run";

    private Handler splashHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView btnGetStarted = findViewById(R.id.btnGetStarted);
        LinearLayout layoutSplashLoading = findViewById(R.id.layoutSplashLoading);
        ProgressBar pbSplash = findViewById(R.id.pbSplash);
        TextView tvSplashStatus = findViewById(R.id.tvSplashStatus);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean hasCompletedFirstRun = prefs.getBoolean(KEY_HAS_COMPLETED_FIRST_RUN, false);
        boolean isAuthenticated = FirebaseAuth.getInstance().getCurrentUser() != null;

        // Fresh install on phone:
        // No loading spinner, no delay, no "Welcome back!" -> Immediate "Continue" button
        if (!hasCompletedFirstRun && !isAuthenticated) {
            prefs.edit().putBoolean(KEY_HAS_COMPLETED_FIRST_RUN, true).apply();

            if (layoutSplashLoading != null) {
                layoutSplashLoading.setVisibility(View.GONE);
            }
            if (btnGetStarted != null) {
                btnGetStarted.setVisibility(View.VISIBLE);
                btnGetStarted.setAlpha(1f);
                btnGetStarted.setTranslationY(0f);
                btnGetStarted.setText(R.string.btn_continue);
                btnGetStarted.setOnClickListener(v -> {
                    Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                });
            }
            return;
        }

        // Returning user / Re-opening (Refresh):
        // 1. Hide Continue button initially
        // 2. Show loading spinner and status transitions
        if (btnGetStarted != null) {
            btnGetStarted.setVisibility(View.GONE);
        }
        if (layoutSplashLoading != null) {
            layoutSplashLoading.setVisibility(View.VISIBLE);
        }
        if (pbSplash != null) {
            pbSplash.setVisibility(View.VISIBLE);
        }

        splashHandler = new Handler(Looper.getMainLooper());

        if (tvSplashStatus != null) {
            tvSplashStatus.setText("Connecting to The Campus Access Hub...");

            splashHandler.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    tvSplashStatus.setText("Loading feed & campus updates...");
                }
            }, 1500);

            // Step 1: Spinner hides, "Welcome back!" appears
            splashHandler.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    if (pbSplash != null) {
                        pbSplash.setVisibility(View.GONE);
                    }
                    tvSplashStatus.setText("Welcome back!");
                    tvSplashStatus.setTextColor(android.graphics.Color.WHITE);
                }
            }, 3000);

            // Step 2: AFTER "Welcome back!" has been shown, reveal Continue button
            splashHandler.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    if (btnGetStarted != null) {
                        btnGetStarted.setText(R.string.btn_continue);
                        btnGetStarted.setVisibility(View.VISIBLE);
                        btnGetStarted.setAlpha(0f);
                        btnGetStarted.setTranslationY(30f);
                        btnGetStarted.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setDuration(450)
                                .start();
                        btnGetStarted.setOnClickListener(v -> {
                            Intent intent;
                            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                intent = new Intent(WelcomeActivity.this, MainActivity.class);
                            } else {
                                intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                            }
                            startActivity(intent);
                            finish();
                        });
                    }
                }
            }, 4400);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (splashHandler != null) {
            splashHandler.removeCallbacksAndMessages(null);
        }
    }
}
