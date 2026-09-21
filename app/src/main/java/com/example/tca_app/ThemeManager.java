package com.example.tca_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    private static final String PREF_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static final int THEME_SYSTEM_DEFAULT = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; // -1
    public static final int THEME_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;                      // 1
    public static final int THEME_DARK = AppCompatDelegate.MODE_NIGHT_YES;                     // 2

    /**
     * Applies the stored theme preference or defaults to System settings.
     */
    public static void applyTheme(Context context) {
        if (context == null) return;
        int mode = getThemeMode(context);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /**
     * Sets and applies a specific theme mode:
     * - THEME_SYSTEM_DEFAULT
     * - THEME_LIGHT
     * - THEME_DARK
     */
    public static void setThemeMode(Context context, int mode) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /**
     * Retrieves the stored theme mode, defaulting to System Default (MODE_NIGHT_FOLLOW_SYSTEM).
     */
    public static int getThemeMode(Context context) {
        if (context == null) return THEME_SYSTEM_DEFAULT;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM_DEFAULT);
    }

    /**
     * Cycles through theme modes: System Default -> Dark Mode -> Light Mode -> System Default
     */
    public static int toggleTheme(Context context) {
        if (context == null) return THEME_SYSTEM_DEFAULT;
        int current = getThemeMode(context);
        int next;
        if (current == THEME_SYSTEM_DEFAULT) {
            next = THEME_DARK;
        } else if (current == THEME_DARK) {
            next = THEME_LIGHT;
        } else {
            next = THEME_SYSTEM_DEFAULT;
        }
        setThemeMode(context, next);
        return next;
    }

    /**
     * Checks if dark mode is currently active (including when following system settings).
     */
    public static boolean isDarkMode(Context context) {
        if (context == null) return false;
        int mode = getThemeMode(context);
        if (mode == THEME_DARK) return true;
        if (mode == THEME_LIGHT) return false;

        // Follow system
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Returns a user-friendly label for current theme setting.
     */
    public static String getThemeLabel(Context context) {
        int mode = getThemeMode(context);
        if (mode == THEME_DARK) return "Dark Mode";
        if (mode == THEME_LIGHT) return "Light Mode";
        return "System Default";
    }
}
