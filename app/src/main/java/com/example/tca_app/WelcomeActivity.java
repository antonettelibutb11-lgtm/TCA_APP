package com.example.tca_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Welcome & Onboarding Activity:
 * Acts as the entry screen allowing users to get started and navigate to Login / Main feed.
 */
public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Auto-route to MainActivity if user is already authenticated (Move check to the very top before setContentView)
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_splash);

        TextView btnGetStarted = findViewById(R.id.btnGetStarted);
        if (btnGetStarted != null) {
            btnGetStarted.setText(R.string.btn_get_started);
            btnGetStarted.setOnClickListener(v -> {
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }
}
