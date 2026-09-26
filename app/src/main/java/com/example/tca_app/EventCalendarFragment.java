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
import com.google.firebase.firestore.FieldValue;
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

        // Check user access
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
        long nowMillis = System.currentTimeMillis();

        // If an exact end time is stored, use it for precise auto-expiry
        if (event.hasEndTime()) {
            return nowMillis > event.getEndTimeMillis();
        }

        // Fallback: day-level comparison (event expires end of its start day)
        Calendar curCal = getCurrentCalendar();
        int curYear  = curCal.get(Calendar.YEAR);
        int curMonth = curCal.get(Calendar.MONTH) + 1;
        int curDay   = curCal.get(Calendar.DAY_OF_MONTH);

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
        TextView etEventDay   = dialogView.findViewById(R.id.etEventDay);
        TextView etEventTime  = dialogView.findViewById(R.id.etEventTime);
        TextView etEventEndTime = dialogView.findViewById(R.id.etEventEndTime);
        EditText etEventDesc  = dialogView.findViewById(R.id.etEventDesc);
        TextView btnCancel    = dialogView.findViewById(R.id.btnCancelAddEvent);
        TextView btnSave      = dialogView.findViewById(R.id.btnSaveNewEvent);

        final int[] selectedDay   = {getCurrentDay()};
        final int[] selectedMonth = {getCurrentMonth()};
        final int[] selectedYear  = {getCurrentYear()};

        // Store picked hour/minute for start and end so we can build exact epoch ms later
        final int[] startHour   = {9};  final int[] startMin   = {0};
        final int[] endHour     = {-1}; final int[] endMin     = {-1};

        final boolean[] isDateSelected = {false};

        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        // Date picker
        etEventDay.setFocusable(false);
        etEventDay.setClickable(true);
        etEventDay.setOnClickListener(v -> {
            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                    requireContext(),
                    R.style.CustomDatePickerDialog,
                    (view1, year, month, dayOfMonth) -> {
                        selectedYear[0]  = year;
                        selectedMonth[0] = month + 1;
                        selectedDay[0]   = dayOfMonth;
                        isDateSelected[0] = true;
                        String monthStr = (month >= 0 && month < 12) ? monthNames[month] : "Aug";
                        etEventDay.setText(monthStr + " " + dayOfMonth + ", " + year);
                        etEventDay.setError(null);
                    },
                    selectedYear[0], selectedMonth[0] - 1, selectedDay[0]
            );
            datePickerDialog.show();
        });

        // Start time picker
        etEventTime.setFocusable(false);
        etEventTime.setClickable(true);
        etEventTime.setOnClickListener(v -> {
            Calendar cal = getCurrentCalendar();
            android.app.TimePickerDialog tp = new android.app.TimePickerDialog(
                    requireContext(),
                    R.style.CustomDatePickerDialog,
                    (view12, hourOfDay, min) -> {
                        startHour[0] = hourOfDay;
                        startMin[0]  = min;
                        String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                        int h12 = (hourOfDay % 12 == 0) ? 12 : (hourOfDay % 12);
                        etEventTime.setText(String.format(Locale.US, "%d:%02d %s", h12, min, amPm));
                        etEventTime.setError(null);
                    },
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false
            );
            tp.show();
        });

        // End time picker
        etEventEndTime.setFocusable(false);
        etEventEndTime.setClickable(true);
        etEventEndTime.setOnClickListener(v -> {
            int defHour = (startHour[0] >= 0) ? startHour[0] + 1 : getCurrentCalendar().get(Calendar.HOUR_OF_DAY);
            android.app.TimePickerDialog tp = new android.app.TimePickerDialog(
                    requireContext(),
                    R.style.CustomDatePickerDialog,
                    (view12, hourOfDay, min) -> {
                        endHour[0] = hourOfDay;
                        endMin[0]  = min;
                        String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                        int h12 = (hourOfDay % 12 == 0) ? 12 : (hourOfDay % 12);
                        etEventEndTime.setText(String.format(Locale.US, "%d:%02d %s", h12, min, amPm));
                    },
                    defHour, 0, false
            );
            tp.show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title   = etEventTitle.getText().toString().trim();
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

            btnSave.setEnabled(false);

            int day = selectedDay[0];
            final String finalTime = timeStr.isEmpty() ? "9:00 AM" : timeStr;
            final String finalDesc = descStr.isEmpty() ? "(Scan QR Code for check-in)" : descStr;

            // Build end time string and epoch ms for auto-expiry
            final String finalEndTime;
            final long finalEndTimeMillis;
            if (endHour[0] >= 0) {
                String endAmPm = (endHour[0] >= 12) ? "PM" : "AM";
                int endH12 = (endHour[0] % 12 == 0) ? 12 : (endHour[0] % 12);
                finalEndTime = String.format(Locale.US, "%d:%02d %s", endH12, endMin[0], endAmPm);
                // Build exact epoch for the end date+time
                Calendar endCal = Calendar.getInstance();
                endCal.set(selectedYear[0], selectedMonth[0] - 1, day, endHour[0], endMin[0], 0);
                endCal.set(Calendar.MILLISECOND, 0);
                finalEndTimeMillis = endCal.getTimeInMillis();
            } else {
                finalEndTime = "";
                finalEndTimeMillis = 0L;
            }

            String newId = String.valueOf(System.currentTimeMillis());
            EventItem newEvent = new EventItem(newId, title, day, selectedMonth[0], selectedYear[0],
                    finalTime, finalEndTime, finalEndTimeMillis, finalDesc);

            Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("title",          title);
            eventMap.put("day",            day);
            eventMap.put("month",          selectedMonth[0]);
            eventMap.put("year",           selectedYear[0]);
            eventMap.put("time",           finalTime);
            eventMap.put("endTime",        finalEndTime);
            eventMap.put("endTimeMillis",  finalEndTimeMillis);
            eventMap.put("description",    finalDesc);

            FirebaseFirestore.getInstance().collection("events").add(eventMap)
                    .addOnSuccessListener(result -> {
                        if (!isAdded() || getContext() == null || getView() == null) return;
                        btnSave.setEnabled(true);
                        dialog.dismiss();

                        String eventDocId = result.getId();
                        newEvent.setId(eventDocId);

                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "✅ Event created!", Toast.LENGTH_SHORT).show();
                        }

                        showPostEventOptionsDialog(newEvent);
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

        // Fetch upcoming events from Firestore
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

                            String endTimeStr  = doc.getString("endTime");
                            Long endTimeMillis = doc.getLong("endTimeMillis");

                            EventItem item = new EventItem(
                                    doc.getId(),
                                    title != null ? title : "Campus Event",
                                    day != null ? day.intValue() : getCurrentDay(),
                                    month != null ? month.intValue() : getCurrentMonth(),
                                    year != null ? year.intValue() : getCurrentYear(),
                                    time != null ? time : "8:00 AM",
                                    endTimeStr != null ? endTimeStr : "",
                                    endTimeMillis != null ? endTimeMillis : 0L,
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
        tvHint.setText("📷 Students scan this QR code to vote at this event.");
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

    // Post-event options dialog (Attendance QR or Voting Poll)
    private void showPostEventOptionsDialog(EventItem event) {
        if (!isAdded() || getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("What would you like to do next?")
                .setMessage("Event \"" + event.getTitle() + "\" has been created.")
                .setPositiveButton("🗳️ Create Voting Poll", (d, w) -> {
                    d.dismiss();
                    showCreateVotingPollDialog(event);
                })
                .setNegativeButton("📷 Show Attendance QR", (d, w) -> {
                    d.dismiss();
                    showEventQrCodeDialog(event);
                })
                .setNeutralButton("Skip", (d, w) -> d.dismiss())
                .show();
    }

    // Dialog for creating a voting poll linked to this event
    private void showCreateVotingPollDialog(EventItem event) {
        if (!isAdded() || getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_voting_poll, null);

        TextView tvEventSubtitle       = dialogView.findViewById(R.id.tvPollEventSubtitle);
        EditText etQuestion            = dialogView.findViewById(R.id.etPollQuestion);
        LinearLayout layoutOptions     = dialogView.findViewById(R.id.layoutPollOptionsContainer);
        TextView btnAddOption          = dialogView.findViewById(R.id.btnAddPollOption);
        TextView btnCancel             = dialogView.findViewById(R.id.btnCancelCreatePoll);
        TextView btnGenerate           = dialogView.findViewById(R.id.btnGenerateVotingQr);

        if (tvEventSubtitle != null) tvEventSubtitle.setText("Event: " + event.getTitle());

        // Start with 2 default option input fields
        if (layoutOptions != null) {
            addOptionField(layoutOptions, "Option 1");
            addOptionField(layoutOptions, "Option 2");
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (btnAddOption != null) {
            btnAddOption.setOnClickListener(v -> {
                if (layoutOptions != null) {
                    int count = layoutOptions.getChildCount() + 1;
                    addOptionField(layoutOptions, "Option " + count);
                }
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnGenerate != null) {
            btnGenerate.setOnClickListener(v -> {
                if (etQuestion == null) return;
                String question = etQuestion.getText().toString().trim();
                if (question.isEmpty()) {
                    etQuestion.setError("Enter a poll question");
                    return;
                }

                List<String> options = new ArrayList<>();
                if (layoutOptions != null) {
                    for (int i = 0; i < layoutOptions.getChildCount(); i++) {
                        View child = layoutOptions.getChildAt(i);
                        if (child instanceof EditText) {
                            String opt = ((EditText) child).getText().toString().trim();
                            if (!opt.isEmpty()) options.add(opt);
                        }
                    }
                }

                if (options.size() < 2) {
                    Toast.makeText(requireContext(), "Please add at least 2 options.", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnGenerate.setEnabled(false);
                btnGenerate.setText("Creating...");

                savePollAndGenerateQr(event, question, options, dialog);
            });
        }

        dialog.show();
    }

    /** Adds a single EditText row to the options container. */
    private void addOptionField(LinearLayout container, String hint) {
        EditText et = new EditText(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 0);
        et.setLayoutParams(params);
        et.setHint(hint);
        et.setTextSize(14f);
        et.setSingleLine(true);
        et.setBackgroundResource(R.drawable.bg_input_field);
        et.setPadding(28, 24, 28, 24);
        try {
            et.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            et.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        } catch (Exception ignored) {}
        container.addView(et);
    }

    /**
     * Saves the VotingPoll document to Firestore, then generates the
     * TCA-VOTE QR bitmap and posts it to the timeline.
     */
    private void savePollAndGenerateQr(EventItem event, String question, List<String> options, AlertDialog dialog) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> initialVotes = new HashMap<>();
        for (String opt : options) initialVotes.put(opt, 0L);

        Map<String, Object> pollData = new HashMap<>();
        pollData.put("eventId",    event.getId());
        pollData.put("eventName",  event.getTitle());
        pollData.put("question",   question);
        pollData.put("options",    options);
        pollData.put("votesCount", initialVotes);
        pollData.put("totalVotes", 0L);
        pollData.put("voterUids",  new ArrayList<String>());
        pollData.put("active",     true);
        pollData.put("createdAt",  FieldValue.serverTimestamp());

        db.collection("voting_polls").add(pollData)
                .addOnSuccessListener(ref -> {
                    if (!isAdded() || getContext() == null) return;

                    String pollId = ref.getId();
                    // QR payload: TCA-VOTE:<pollId>:<question>
                    String qrPayload = "TCA-VOTE:" + pollId + ":" + question;
                    Bitmap qrBitmap = generateQrCodeBitmap(qrPayload, 512, 512);

                    dialog.dismiss();
                    Toast.makeText(requireContext(), "✅ Poll created! Posting QR code...", Toast.LENGTH_SHORT).show();

                    // Show the QR for the admin, then offer to post
                    showVotingQrDialog(event, question, pollId, qrBitmap);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    Toast.makeText(requireContext(), "❌ Failed to create poll: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                });
    }

    /**
     * Shows a preview of the Voting QR code to the admin with a "Post to Feed" button.
     */
    private void showVotingQrDialog(EventItem event, String question, String pollId, Bitmap qrBitmap) {
        if (!isAdded() || getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 36);
        layout.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        layout.setBackgroundColor(Color.parseColor("#130F22"));

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("🗳️ Voting Poll QR Code");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvTitle);

        TextView tvQuestion = new TextView(requireContext());
        tvQuestion.setText(question);
        tvQuestion.setTextColor(Color.parseColor("#F59E0B"));
        tvQuestion.setTextSize(13.5f);
        tvQuestion.setGravity(android.view.Gravity.CENTER);
        tvQuestion.setPadding(0, 8, 0, 20);
        layout.addView(tvQuestion);

        ImageView ivQr = new ImageView(requireContext());
        int qrSize = (int) (220 * getResources().getDisplayMetrics().density);
        if (qrBitmap != null) ivQr.setImageBitmap(qrBitmap);
        ivQr.setPadding(16, 16, 16, 16);
        ivQr.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(qrSize, qrSize);
        qrParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        ivQr.setLayoutParams(qrParams);
        layout.addView(ivQr);

        TextView tvHint = new TextView(requireContext());
        tvHint.setText("📲 Post this QR code so students can scan it and cast their vote.");
        tvHint.setTextColor(Color.parseColor("#9F80C4"));
        tvHint.setTextSize(12f);
        tvHint.setGravity(android.view.Gravity.CENTER);
        tvHint.setPadding(0, 18, 0, 20);
        layout.addView(tvHint);

        // Post button
        TextView btnPost = new TextView(requireContext());
        btnPost.setText("📢 Post Voting QR to Feed");
        btnPost.setTextColor(Color.WHITE);
        btnPost.setTextSize(15f);
        btnPost.setTypeface(null, android.graphics.Typeface.BOLD);
        btnPost.setGravity(android.view.Gravity.CENTER);
        btnPost.setBackgroundResource(R.drawable.bg_purple_button);
        btnPost.setPadding(32, 28, 32, 28);
        btnPost.setClickable(true);
        btnPost.setFocusable(true);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, 8, 0, 12);
        btnPost.setLayoutParams(btnParams);
        layout.addView(btnPost);

        TextView btnClose = new TextView(requireContext());
        btnClose.setText("Close");
        btnClose.setTextColor(Color.parseColor("#B0A8C0"));
        btnClose.setTextSize(13.5f);
        btnClose.setGravity(android.view.Gravity.CENTER);
        btnClose.setPadding(24, 16, 24, 16);
        btnClose.setClickable(true);
        btnClose.setFocusable(true);
        layout.addView(btnClose);

        builder.setView(layout);
        AlertDialog votingQrDialog = builder.create();

        btnPost.setOnClickListener(v -> {
            btnPost.setEnabled(false);
            btnPost.setText("Posting...");
            autoPostVotingQrToFyp(event.getTitle(), event.getDateFormatted(), question, qrBitmap, () -> {
                votingQrDialog.dismiss();
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "🗳️ Voting QR posted to feed!", Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToTab(0);
                    }
                }
            });
        });

        btnClose.setOnClickListener(v -> votingQrDialog.dismiss());
        votingQrDialog.show();
    }

    private void autoPostVotingQrToFyp(String title, String dateFormatted, String question, Bitmap qrBitmap, Runnable onComplete) {
        if (qrBitmap == null) {
            publishVotingFeedPost(title, dateFormatted, question, "", onComplete);
            return;
        }
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            java.io.InputStream stream = new java.io.ByteArrayInputStream(baos.toByteArray());
            CloudinaryUploader.uploadImage(stream, new CloudinaryUploader.CloudinaryUploadCallback() {
                @Override public void onSuccess(String url)    { publishVotingFeedPost(title, dateFormatted, question, url, onComplete); }
                @Override public void onFailure(String error)  { publishVotingFeedPost(title, dateFormatted, question, "", onComplete); }
            });
        } catch (Exception e) {
            publishVotingFeedPost(title, dateFormatted, question, "", onComplete);
        }
    }

    private void publishVotingFeedPost(String title, String dateFormatted, String question, String qrImageUrl, Runnable onComplete) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String authorUid = (currentUser != null && currentUser.getUid() != null) ? currentUser.getUid() : "admin_tca";

        Map<String, Object> postMap = new HashMap<>();
        postMap.put("authorName",      "The Campus Access Editorial Desk");
        postMap.put("authorUid",       authorUid);
        postMap.put("postMeta",        "Campus Voting • " + dateFormatted + " • BISU Balilihan");
        postMap.put("badgeText",       "🗳️ Voting Poll Open");
        postMap.put("category",        "Voting");
        postMap.put("content",         "🗳️ CAMPUS VOTE: " + title + "\n\n❓ Question: " + question
                + "\n\n📲 Scan the QR code below to cast your vote now!");
        postMap.put("photoUri",        qrImageUrl != null ? qrImageUrl : "");
        postMap.put("isPinned",        true);
        postMap.put("isAiPick",        true);
        postMap.put("likeCount",       0);
        postMap.put("loveCount",       0);
        postMap.put("commentCount",    0);
        postMap.put("timestamp",       System.currentTimeMillis());
        postMap.put("moderationStatus", "APPROVED");

        FirebaseFirestore.getInstance().collection("posts").add(postMap)
                .addOnCompleteListener(task -> { if (onComplete != null) onComplete.run(); });
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
