package com.example.tca_app;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    // Thread-safe formatters using ThreadLocal to eliminate synchronized lock contention
    private static final ThreadLocal<SimpleDateFormat> SAME_YEAR_FORMATTER =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("MMM d", Locale.US));

    private static final ThreadLocal<SimpleDateFormat> DIFFERENT_YEAR_FORMATTER =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("MMM d, yyyy", Locale.US));

    /**
     * Converts a millisecond timestamp into dynamic relative time:
     * - < 1 min: "Just now • Category"
     * - < 1 hr: "X mins ago • Category"
     * - < 24 hrs: "X hrs ago • Category"
     * - < 7 days: "X days ago • Category"
     * - >= 7 days: "MMM d • Category" (current year) or "MMM d, yyyy • Category" (previous year)
     *
     * Non-blocking, thread-safe, and localized using Context string resources.
     */
    public static String getRelativeTimeString(Context context, long timestamp, String category) {
        if (context == null) {
            return (timestamp <= 0 ? "Just now" : "") + " • " + (category != null ? category : "Events");
        }

        if (category == null || category.trim().isEmpty()) {
            category = context.getString(R.string.category_default_events);
        }

        String justNow = context.getString(R.string.time_just_now);

        if (timestamp <= 0) {
            return justNow + " • " + category;
        }

        long currentTime = System.currentTimeMillis();
        long diff = currentTime - timestamp;

        if (diff < 0) {
            return justNow + " • " + category;
        }

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        String timeAgo;

        if (seconds < 60) {
            timeAgo = justNow;
        } else if (minutes < 60) {
            timeAgo = minutes == 1
                    ? context.getString(R.string.time_min_ago)
                    : context.getString(R.string.time_mins_ago, minutes);
        } else if (hours < 24) {
            timeAgo = hours == 1
                    ? context.getString(R.string.time_hr_ago)
                    : context.getString(R.string.time_hrs_ago, hours);
        } else if (days < 7) {
            timeAgo = days == 1
                    ? context.getString(R.string.time_day_ago)
                    : context.getString(R.string.time_days_ago, days);
        } else {
            Calendar postCal = Calendar.getInstance();
            postCal.setTimeInMillis(timestamp);

            Calendar currentCal = Calendar.getInstance();
            currentCal.setTimeInMillis(currentTime);

            Date date = new Date(timestamp);
            if (postCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR)) {
                timeAgo = SAME_YEAR_FORMATTER.get().format(date);
            } else {
                timeAgo = DIFFERENT_YEAR_FORMATTER.get().format(date);
            }
        }

        return timeAgo + " • " + category;
    }
}
