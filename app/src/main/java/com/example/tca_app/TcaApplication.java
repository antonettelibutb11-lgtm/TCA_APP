package com.example.tca_app;

import android.app.Application;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

// Application class for initializing theme and offline database settings
public class TcaApplication extends Application {

    private static final String TAG = "TcaApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Apply user or system theme preference globally once
        ThemeManager.applyTheme(this);

        // 2. Initialize Firestore offline persistence
        initFirestoreSettings();
    }

    private void initFirestoreSettings() {
        try {
            FirebaseFirestore.getInstance().setFirestoreSettings(
                    new com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                            .setPersistenceEnabled(true)
                            .build()
            );
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error configuring Firestore settings: " + e.getMessage(), e);
        }
    }
}
