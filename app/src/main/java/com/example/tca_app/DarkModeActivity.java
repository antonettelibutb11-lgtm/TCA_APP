package com.example.tca_app;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import androidx.appcompat.app.AppCompatActivity;

public class DarkModeActivity extends AppCompatActivity {

    private RadioButton rbOn, rbOff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        setContentView(R.layout.activity_dark_mode);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        View rowOn = findViewById(R.id.rowOn);
        View rowOff = findViewById(R.id.rowOff);

        rbOn = findViewById(R.id.rbOn);
        rbOff = findViewById(R.id.rbOff);

        // Set current selection
        int currentMode = ThemeManager.getThemeMode(this);
        if (currentMode == ThemeManager.THEME_DARK) {
            rbOn.setChecked(true);
        } else {
            rbOff.setChecked(true);
        }

        // Setup click listeners for rows
        rowOn.setOnClickListener(v -> setMode(ThemeManager.THEME_DARK));
        rowOff.setOnClickListener(v -> setMode(ThemeManager.THEME_LIGHT));
    }

    private void setMode(int mode) {
        if (ThemeManager.getThemeMode(this) == mode) return; // No change

        // Update radio buttons
        rbOn.setChecked(mode == ThemeManager.THEME_DARK);
        rbOff.setChecked(mode == ThemeManager.THEME_LIGHT);

        // Apply theme
        ThemeManager.setThemeMode(this, mode);
        recreate(); // Recreate immediately to apply theme
    }
}
