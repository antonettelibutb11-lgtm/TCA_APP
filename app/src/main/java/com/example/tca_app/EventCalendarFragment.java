package com.example.tca_app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventCalendarFragment extends Fragment {

    private Map<Integer, TextView> dateTextViewMap = new HashMap<>();
    private LinearLayout layoutEventsListContainer;
    private LinearLayout layoutNoEvents;
    private TextView tvSelectedEventNotice;
    private Button btnAddEvent;
    private LinearLayout btnScanQr;

    private List<EventItem> allEvents = new ArrayList<>();
    private List<EventItem> activeUpcomingEvents = new ArrayList<>();
    private EventItem selectedEvent = null;

    private ListenerRegistration eventsListenerRegistration;

    private Calendar getCurrentCalendar() {
        return Calendar.getInstance();
    }

    private int getCurrentDay() { return getCurrentCalendar().get(Calendar.DAY_OF_MONTH); }
    private int getCurrentMonth() { return getCurrentCalendar().get(Calendar.MONTH) + 1; }
    private int getCurrentYear() { return getCurrentCalendar().get(Calendar.YEAR); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_calendar, container, false);

        layoutEventsListContainer = view.findViewById(R.id.layoutEventsListContainer);
        layoutNoEvents = view.findViewById(R.id.layoutNoEvents);
        tvSelectedEventNotice = view.findViewById(R.id.tvSelectedEventNotice);
        btnAddEvent = view.findViewById(R.id.btnAddEvent);
        btnScanQr = view.findViewById(R.id.btnScanQr);
        if (btnAddEvent != null) {
            btnAddEvent.setVisibility(View.GONE); // Default GONE for security
        }

        // Enforce Campus Access Membership & Admin Access Control
        checkUserMembershipAndAdminAccess();

        // Dynamically update the Month/Year header and Today badge
        TextView tvMonthYear = view.findViewById(R.id.tvMonthYear);
        TextView tvTodayBadge = view.findViewById(R.id.tvTodayBadge);
        Calendar cal = getCurrentCalendar();
        String[] monthNames = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        String[] shortMonthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        int monthIdx = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        if (tvMonthYear != null) {
            tvMonthYear.setText(monthNames[monthIdx] + " " + year);
        }
        if (tvTodayBadge != null) {
            tvTodayBadge.setText("Today: " + shortMonthNames[monthIdx] + " " + day);
        }

        // Bind Calendar Date Grid TextViews (Day 1 to Day 31)
        bindDateViews(view);

        // Fetch events dynamically from Firestore events collection
        fetchEventsFromFirestore();

        if (btnAddEvent != null) {
            btnAddEvent.setOnClickListener(v -> showAddEventDialog());
        }

        if (btnScanQr != null) {
            btnScanQr.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), QRScannerActivity.class);
                if (selectedEvent != null) {
                    intent.putExtra("EVENT_ID", selectedEvent.getId());
                    intent.putExtra("EVENT_NAME", selectedEvent.getTitle());
                    intent.putExtra("EVENT_DATE", selectedEvent.getDateFormatted());
                } else {
                    intent.putExtra("EVENT_ID", "");
                    intent.putExtra("EVENT_NAME", "Campus Event Check-in");
                    intent.putExtra("EVENT_DATE", "Today");
                }
                startActivity(intent);
            });
        }

        // Filter past events and render upcoming event cards
        refreshEventsUI();

        return view;
    }

    private void bindDateViews(View view) {
        int[] resIds = new int[]{
                R.id.tvDate1, R.id.tvDate2, R.id.tvDate3, R.id.tvDate4, R.id.tvDate5,
                R.id.tvDate6, R.id.tvDate7, R.id.tvDate8, R.id.tvDate9, R.id.tvDate10,
                R.id.tvDate11, R.id.tvDate12, R.id.tvDate13, R.id.tvDate14, R.id.tvDate15,
                R.id.tvDate16, R.id.tvDate17, R.id.tvDate18, R.id.tvDate19, R.id.tvDate20,
                R.id.tvDate21, R.id.tvDate22, R.id.tvDate23, R.id.tvDate24, R.id.tvDate25,
                R.id.tvDate26, R.id.tvDate27, R.id.tvDate28, R.id.tvDate29, R.id.tvDate30,
                R.id.tvDate31
        };

        for (int i = 0; i < resIds.length; i++) {
            TextView tv = view.findViewById(resIds[i]);
            if (tv != null) {
                dateTextViewMap.put(i + 1, tv);
            }
        }
    }

    private void refreshEventsUI() {
        if (!isAdded() || getContext() == null) return;

        activeUpcomingEvents.clear();

        // Auto-filter: Keep only current or future events
        for (EventItem event : allEvents) {
            if (!isPastEvent(event)) {
                activeUpcomingEvents.add(event);
            }
        }

        // Sort events chronologically by day
        Collections.sort(activeUpcomingEvents, Comparator.comparingInt(EventItem::getDay));

        if (layoutEventsListContainer == null) return;
        layoutEventsListContainer.removeAllViews();

        if (activeUpcomingEvents.isEmpty()) {
            if (layoutNoEvents != null) layoutNoEvents.setVisibility(View.VISIBLE);
            selectedEvent = null;
            resetAllDateChipStyles();
            return;
        }

        if (layoutNoEvents != null) layoutNoEvents.setVisibility(View.GONE);

        // Auto-select first upcoming event if none selected or if previous selected event expired
        if (selectedEvent == null || isPastEvent(selectedEvent)) {
            selectedEvent = activeUpcomingEvents.get(0);
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (EventItem event : activeUpcomingEvents) {
            View cardView = inflater.inflate(R.layout.item_event_card, layoutEventsListContainer, false);

            TextView tvBadge = cardView.findViewById(R.id.tvEventBadge);
            TextView tvTitle = cardView.findViewById(R.id.tvEventTitle);
            TextView tvDesc = cardView.findViewById(R.id.tvEventDesc);
            TextView tvCheckStatus = cardView.findViewById(R.id.tvCheckStatus);

            tvBadge.setText("🗓️ Date: " + event.getDateFormatted());
            tvTitle.setText(event.getTitle());
            tvDesc.setText(event.getDescription());

            boolean isSelected = (selectedEvent != null && selectedEvent.getId().equals(event.getId()));

            if (isSelected) {
                cardView.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_selected));
                tvCheckStatus.setText("✓ Selected");
                tvCheckStatus.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f);
                tvCheckStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                tvBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold_accent));
            } else {
                cardView.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_white));
                tvCheckStatus.setText("›");
                tvCheckStatus.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24f);
                tvCheckStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                tvBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            }

            cardView.setOnClickListener(v -> {
                selectedEvent = event;
                refreshEventsUI();
            });

            // Long click to view Event QR Code (for attendance check-in display)
            cardView.setOnLongClickListener(v -> {
                showEventQrCodeDialog(event);
                return true;
            });

            layoutEventsListContainer.addView(cardView);
        }

        // Update top calendar date highlights & selected notice text
        updateCalendarAndNotice();
    }

    private boolean isPastEvent(EventItem event) {
        Calendar curCal = getCurrentCalendar();
        int curYear = curCal.get(Calendar.YEAR);
        int curMonth = curCal.get(Calendar.MONTH) + 1;
        int curDay = curCal.get(Calendar.DAY_OF_MONTH);

        if (event.getYear() < curYear) return true;
        if (event.getYear() == curYear && event.getMonth() < curMonth) return true;
        if (event.getYear() == curYear && event.getMonth() == curMonth && event.getDay() < curDay) return true;
        return false;
    }

    private void updateCalendarAndNotice() {
        if (!isAdded() || getContext() == null) return;
        resetAllDateChipStyles(); // This now highlights TODAY's date

        if (selectedEvent != null && tvSelectedEventNotice != null) {
            tvSelectedEventNotice.setText("Selected: " + selectedEvent.getTitle() + " (" + selectedEvent.getDateFormatted() + ")");
        }
    }

    private void resetAllDateChipStyles() {
        if (!isAdded() || getContext() == null) return;
        int today = getCurrentDay();
        for (Map.Entry<Integer, TextView> entry : dateTextViewMap.entrySet()) {
            TextView tv = entry.getValue();
            if (tv != null) {
                if (entry.getKey() == today) {
                    // Highlight TODAY with solid yellow
                    tv.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_calendar_today_gold));
                    tv.setTextColor(Color.BLACK);
                } else {
                    // Normal day
                    tv.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_calendar_date));
                    tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.calendar_date_text));
                }
            }
        }
    }

    /**
     * Modal dialog to add a new event dynamically
     */
    private void showAddEventDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_event, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        EditText etEventTitle = dialogView.findViewById(R.id.etEventTitle);
        TextView etEventDay = dialogView.findViewById(R.id.etEventDay);
        TextView etEventTime = dialogView.findViewById(R.id.etEventTime);
        EditText etEventDesc = dialogView.findViewById(R.id.etEventDesc);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancelAddEvent);
        TextView btnSave = dialogView.findViewById(R.id.btnSaveNewEvent);

        final int[] selectedDay = {getCurrentDay()};
        final int[] selectedMonth = {getCurrentMonth()};
        final int[] selectedYear = {getCurrentYear()};

        final boolean[] isDateSelected = {false};

        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        etEventDay.setFocusable(false);
        etEventDay.setClickable(true);
        etEventDay.setOnClickListener(v -> {
            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                    requireContext(),
                    R.style.CustomDatePickerDialog,
                    (view1, year, month, dayOfMonth) -> {
                        selectedYear[0] = year;
                        selectedMonth[0] = month + 1;
                        selectedDay[0] = dayOfMonth;
                        isDateSelected[0] = true;
                        String monthStr = (month >= 0 && month < 12) ? monthNames[month] : "Aug";
                        etEventDay.setText(monthStr + " " + dayOfMonth + ", " + year);
                        etEventDay.setError(null);
                    },
                    selectedYear[0], selectedMonth[0] - 1, selectedDay[0]
            );
            datePickerDialog.show();
        });

        etEventTime.setFocusable(false);
        etEventTime.setClickable(true);
        etEventTime.setOnClickListener(v -> {
            Calendar cal = getCurrentCalendar();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);

            android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                    requireContext(),
                    R.style.CustomDatePickerDialog,
                    (view12, hourOfDay, min) -> {
                        String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                        int hour12 = (hourOfDay % 12 == 0) ? 12 : (hourOfDay % 12);
                        String formattedTime = String.format(Locale.US, "%d:%02d %s", hour12, min, amPm);
                        etEventTime.setText(formattedTime);
                        etEventTime.setError(null);
                    },
                    hour, minute, false
            );
            timePickerDialog.show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etEventTitle.getText().toString().trim();
            String timeStr = etEventTime.getText().toString().trim();
            String descStr = etEventDesc != null ? etEventDesc.getText().toString().trim() : "";

            if (title.isEmpty()) {
                etEventTitle.setError("Please enter event title");
                return;
            }

            if (!isDateSelected[0] && etEventDay.getText().toString().trim().isEmpty()) {
                etEventDay.setError("Please select a date");
                return;
            }

            btnSave.setEnabled(false); // Lock button against spam-clicking

            int day = selectedDay[0];

            final String finalTime = timeStr.isEmpty() ? "9:00 AM" : timeStr;
            final String finalDesc = descStr.isEmpty() ? "(Scan QR Code for check-in)" : descStr;

            String newId = String.valueOf(System.currentTimeMillis());
            EventItem newEvent = new EventItem(newId, title, day, selectedMonth[0], selectedYear[0], finalTime, finalDesc);

            // Zero-Trust Security: Enforced via firestore.rules rather than Cloud Functions
            Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("title", title);
            eventMap.put("day", day);
            eventMap.put("month", selectedMonth[0]);
            eventMap.put("year", selectedYear[0]);
            eventMap.put("time", finalTime);
            eventMap.put("description", finalDesc);

            FirebaseFirestore.getInstance().collection("events").add(eventMap)
                    .addOnSuccessListener(result -> {
                        if (!isAdded() || getContext() == null || getView() == null) return;
                        btnSave.setEnabled(true);
                        dialog.dismiss();

                        String eventDocId = result.getId();
                        newEvent.setId(eventDocId);

                        // Show QR Code Dialog immediately with the "POST TO TIMELINE & FYP" button
                        showEventQrCodeDialog(newEvent);

                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "✅ Event created! Previewing Attendance QR Code...", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded() || getContext() == null || getView() == null) return;
                        btnSave.setEnabled(true);
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "❌ Failed to create event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        dialog.show();
    }

    private void fetchEventsFromFirestore() {
        if (eventsListenerRegistration != null) {
            eventsListenerRegistration.remove();
            eventsListenerRegistration = null;
        }

        // COST FIX: .limit(50) caps the events fetched. Past events are already filtered
        // client-side via isPastEvent(), so fetching hundreds of old events would be wasteful.
        eventsListenerRegistration = FirebaseFirestore.getInstance()
                .collection("events")
                .orderBy("year", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .limit(50)
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded() || getContext() == null || getView() == null) return;
                    if (snapshots != null && !snapshots.isEmpty()) {
                        allEvents.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String title = doc.getString("title");
                            Long day = doc.getLong("day");
                            Long month = doc.getLong("month");
                            Long year = doc.getLong("year");
                            String time = doc.getString("time");
                            String desc = doc.getString("description");

                            EventItem item = new EventItem(
                                    doc.getId(),
                                    title != null ? title : "Campus Event",
                                    day != null ? day.intValue() : getCurrentDay(),
                                    month != null ? month.intValue() : getCurrentMonth(),
                                    year != null ? year.intValue() : getCurrentYear(),
                                    time != null ? time : "8:00 AM",
                                    desc != null ? desc : "(Scan QR Code for check-in)"
                            );
                            allEvents.add(item);
                        }
                        refreshEventsUI();
                    }
                });
    }

    private void checkUserMembershipAndAdminAccess() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (btnAddEvent != null) btnAddEvent.setVisibility(View.GONE);
            if (btnScanQr != null) btnScanQr.setVisibility(View.VISIBLE);
            return;
        }

        String email = currentUser.getEmail() != null ? currentUser.getEmail().toLowerCase() : "";
        if ("antonettebandal.11@gmail.com".equalsIgnoreCase(email)) {
            if (btnAddEvent != null) btnAddEvent.setVisibility(View.VISIBLE);
            if (btnScanQr != null) btnScanQr.setVisibility(View.GONE); // Admin generates/shows QR, does not scan
            return;
        }

        AuthUtils.checkCurrentUserAccess((isApprovedMember, isAdmin, role) -> {
            if (!isAdded() || getContext() == null) return;
            if (isAdmin) {
                if (btnAddEvent != null) btnAddEvent.setVisibility(View.VISIBLE);
                if (btnScanQr != null) btnScanQr.setVisibility(View.GONE); // Hidden for Admin
            } else {
                if (btnAddEvent != null) btnAddEvent.setVisibility(View.GONE);
                if (btnScanQr != null) btnScanQr.setVisibility(View.VISIBLE); // Visible for Students
            }
        });
    }

    /**
     * Displays a popup dialog containing the Event's Attendance QR Code.
     * Organizers / Admins can display this QR code on screen for students to scan.
     */
    public void showEventQrCodeDialog(EventItem event) {
        if (!isAdded() || getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 36);
        layout.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        layout.setBackgroundColor(Color.parseColor("#130F22"));

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(event.getTitle());
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(19f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvTitle);

        TextView tvSubtitle = new TextView(requireContext());
        tvSubtitle.setText("🗓️ " + event.getDateFormatted());
        tvSubtitle.setTextColor(Color.parseColor("#F59E0B"));
        tvSubtitle.setTextSize(13.5f);
        tvSubtitle.setGravity(android.view.Gravity.CENTER);
        tvSubtitle.setPadding(0, 8, 0, 20);
        layout.addView(tvSubtitle);

        // QR Payload format: TCA-EVENT:<eventId>:<eventName>
        String qrPayload = "TCA-EVENT:" + event.getId() + ":" + event.getTitle();

        ImageView ivQr = new ImageView(requireContext());
        int qrSize = (int) (220 * getResources().getDisplayMetrics().density);
        Bitmap qrBitmap = generateQrCodeBitmap(qrPayload, 512, 512);
        if (qrBitmap != null) {
            ivQr.setImageBitmap(qrBitmap);
        }
        ivQr.setPadding(16, 16, 16, 16);
        ivQr.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(qrSize, qrSize);
        qrParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        ivQr.setLayoutParams(qrParams);
        layout.addView(ivQr);

        TextView tvHint = new TextView(requireContext());
        tvHint.setText("📷 Students can scan this QR code to check in directly to attendance.");
        tvHint.setTextColor(Color.parseColor("#9F80C4"));
        tvHint.setTextSize(12f);
        tvHint.setGravity(android.view.Gravity.CENTER);
        tvHint.setPadding(0, 18, 0, 20);
        layout.addView(tvHint);

        // 1. Post Button (Primary Action)
        TextView btnPostToTimeline = new TextView(requireContext());
        btnPostToTimeline.setText("Post");
        btnPostToTimeline.setTextColor(Color.WHITE);
        btnPostToTimeline.setTextSize(15f);
        btnPostToTimeline.setTypeface(null, android.graphics.Typeface.BOLD);
        btnPostToTimeline.setGravity(android.view.Gravity.CENTER);
        btnPostToTimeline.setBackgroundResource(R.drawable.bg_purple_button);
        btnPostToTimeline.setPadding(32, 28, 32, 28);
        btnPostToTimeline.setClickable(true);
        btnPostToTimeline.setFocusable(true);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, 8, 0, 12);
        btnPostToTimeline.setLayoutParams(btnParams);
        layout.addView(btnPostToTimeline);

        // 2. Close Button (Secondary Action)
        TextView btnCloseDialog = new TextView(requireContext());
        btnCloseDialog.setText("Close");
        btnCloseDialog.setTextColor(Color.parseColor("#B0A8C0"));
        btnCloseDialog.setTextSize(13.5f);
        btnCloseDialog.setGravity(android.view.Gravity.CENTER);
        btnCloseDialog.setPadding(24, 16, 24, 16);
        btnCloseDialog.setClickable(true);
        btnCloseDialog.setFocusable(true);
        layout.addView(btnCloseDialog);

        builder.setView(layout);
        AlertDialog dialog = builder.create();

        btnPostToTimeline.setOnClickListener(v -> {
            btnPostToTimeline.setEnabled(false);
            btnPostToTimeline.setText("Posting...");

            autoPostEventToFyp(
                    event.getTitle(),
                    event.getDateFormatted(),
                    event.getTime(),
                    event.getDescription(),
                    qrBitmap,
                    event.getId(),
                    () -> {
                        dialog.dismiss();
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "Event & QR Code posted successfully!", Toast.LENGTH_SHORT).show();
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).navigateToTab(0); // Switch to Timeline / Home Feed
                            }
                        }
                    }
            );
        });

        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void autoPostEventToFyp(String title, String dateFormatted, String timeStr, String descStr, Bitmap qrBitmap, String eventDocId, Runnable onComplete) {
        if (qrBitmap == null) {
            publishEventFeedPost(title, dateFormatted, timeStr, descStr, "", onComplete);
            return;
        }

        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            java.io.InputStream stream = new java.io.ByteArrayInputStream(baos.toByteArray());

            CloudinaryUploader.uploadImage(stream, new CloudinaryUploader.CloudinaryUploadCallback() {
                @Override
                public void onSuccess(String qrImageUrl) {
                    publishEventFeedPost(title, dateFormatted, timeStr, descStr, qrImageUrl, onComplete);
                }

                @Override
                public void onFailure(String error) {
                    publishEventFeedPost(title, dateFormatted, timeStr, descStr, "", onComplete);
                }
            });
        } catch (Exception e) {
            publishEventFeedPost(title, dateFormatted, timeStr, descStr, "", onComplete);
        }
    }

    private void publishEventFeedPost(String title, String dateFormatted, String timeStr, String descStr, String qrImageUrl, Runnable onComplete) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String authorName = "The Campus Access Editorial Desk";
        String authorUid = (currentUser != null && currentUser.getUid() != null) ? currentUser.getUid() : "admin_tca";

        Map<String, Object> postMap = new HashMap<>();
        postMap.put("authorName", authorName);
        postMap.put("authorUid", authorUid);
        postMap.put("postMeta", "Campus Events • " + timeStr + " • BISU Balilihan");
        postMap.put("badgeText", "📅 Event Announcement");
        postMap.put("category", "Events");
        postMap.put("content", "📢 NEW CAMPUS EVENT: " + title + "\n\n🗓️ Date: " + dateFormatted + "\n⏰ Time: " + timeStr + "\n📝 Description: " + descStr + "\n\n📷 ATTENDANCE QR CODE:\nStudents can scan the QR code attached below for official attendance check-in!");
        postMap.put("photoUri", qrImageUrl != null ? qrImageUrl : "");
        postMap.put("isPinned", false);
        postMap.put("isAiPick", true);
        postMap.put("likeCount", 0);
        postMap.put("loveCount", 0);
        postMap.put("commentCount", 0);
        postMap.put("timestamp", System.currentTimeMillis());
        postMap.put("moderationStatus", "APPROVED");

        FirebaseFirestore.getInstance().collection("posts").add(postMap)
                .addOnCompleteListener(task -> {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }

    private Bitmap generateQrCodeBitmap(String text, int width, int height) {
        try {
            com.google.zxing.qrcode.QRCodeWriter writer = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (eventsListenerRegistration != null) {
            eventsListenerRegistration.remove();
            eventsListenerRegistration = null;
        }
    }
}
