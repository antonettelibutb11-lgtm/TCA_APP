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

    private static final ThreadLocal<SimpleDateFormat> CHAT_TIME_ONLY =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("h:mm a", Locale.US));

    private static final ThreadLocal<SimpleDateFormat> CHAT_DAY_TIME =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("EEEE h:mm a", Locale.US));

    private static final ThreadLocal<SimpleDateFormat> CHAT_DATE_SAME_YEAR =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("MMM d, h:mm a", Locale.US));

    private static final ThreadLocal<SimpleDateFormat> CHAT_DATE_DIFFERENT_YEAR =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.US));

    private static final ThreadLocal<SimpleDateFormat> INQUIRY_DAY_ONLY =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("EEEE", Locale.US));

    private static final ThreadLocal<SimpleDateFormat> INQUIRY_DATE_SAME_YEAR =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("MMM d", Locale.US));

    private static final ThreadLocal<SimpleDateFormat> INQUIRY_DATE_DIFF_YEAR =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("MMM d, yyyy", Locale.US));

    /**
     * Formats chat message timestamp dynamically according to requirements:
     * - Today / within same day: exact time ("3:55 AM", "1:26 PM")
     * - 1 day to 6 days ago: Day of week + time ("Yesterday 3:55 AM", "Monday 3:55 AM", "Tuesday 1:26 PM")
     * - > 1 week ago: full date + time ("Aug 27, 3:55 AM" or "Aug 27, 2025, 3:55 AM")
     */
    public static String getChatMessageTimeString(long timestamp) {
        if (timestamp <= 0) return "";

        long now = System.currentTimeMillis();
        Calendar msgCal = Calendar.getInstance();
        msgCal.setTimeInMillis(timestamp);

        Calendar nowCal = Calendar.getInstance();
        nowCal.setTimeInMillis(now);

        Date msgDate = new Date(timestamp);

        // Same day (Today): show exact time
        if (msgCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
                && msgCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)) {
            return CHAT_TIME_ONLY.get().format(msgDate);
        }

        long diffMillis = now - timestamp;
        long diffDays = diffMillis / (24 * 60 * 60 * 1000L);

        // Check if yesterday
        Calendar yesterdayCal = Calendar.getInstance();
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);
        boolean isYesterday = (msgCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR)
                && msgCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR));

        // Reached 1 day to 6 days ago (within 1 week): Show day name + time
        if (diffDays < 7 && diffMillis >= 0) {
            if (isYesterday) {
                return "Yesterday " + CHAT_TIME_ONLY.get().format(msgDate);
            }
            return CHAT_DAY_TIME.get().format(msgDate);
        }

        // More than 1 week ago: Show date + time
        if (msgCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) {
            return CHAT_DATE_SAME_YEAR.get().format(msgDate);
        } else {
            return CHAT_DATE_DIFFERENT_YEAR.get().format(msgDate);
        }
    }

    /**
     * Formats conversation inquiry list timestamp:
     * - Today: exact time ("1:26 PM")
     * - Reached 1 day (within 1 week): "Yesterday", "Monday", "Tuesday", etc.
     * - > 1 week: "Aug 27" or "Aug 27, 2025"
     */
    public static String getInquiryTimeString(long timestamp) {
        if (timestamp <= 0) return "";

        long now = System.currentTimeMillis();
        Calendar msgCal = Calendar.getInstance();
        msgCal.setTimeInMillis(timestamp);

        Calendar nowCal = Calendar.getInstance();
        nowCal.setTimeInMillis(now);

        Date msgDate = new Date(timestamp);

        if (msgCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
                && msgCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)) {
            return CHAT_TIME_ONLY.get().format(msgDate);
        }

        long diffMillis = now - timestamp;
        long diffDays = diffMillis / (24 * 60 * 60 * 1000L);

        Calendar yesterdayCal = Calendar.getInstance();
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);
        boolean isYesterday = (msgCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR)
                && msgCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR));

        if (diffDays < 7 && diffMillis >= 0) {
            if (isYesterday) return "Yesterday";
            return INQUIRY_DAY_ONLY.get().format(msgDate);
        }

        if (msgCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) {
            return INQUIRY_DATE_SAME_YEAR.get().format(msgDate);
        } else {
            return INQUIRY_DATE_DIFF_YEAR.get().format(msgDate);
        }
    }
}
