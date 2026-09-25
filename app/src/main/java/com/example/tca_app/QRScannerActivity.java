package com.example.tca_app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class QRScannerActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CAMERA = 101;

    // QR Payload Prefixes
    private static final String PREFIX_EVENT    = "TCA-EVENT:";
    private static final String PREFIX_EVENT_ALT = "EVENT:";
    private static final String PREFIX_VOTE     = "TCA-VOTE:";

    private PreviewView previewView;
    private View scanLaserLine;
    private ImageView btnTorch;
    private View btnBackQr;
    private LinearLayout cardAttendanceVerified;
    private TextView tvHeaderTitle;
    private TextView tvHeaderSubtitle;
    private TextView tvAttendanceStatus;
    private TextView tvEventTitleCard;
    private TextView tvAttendanceTimestamp;
    private TextView tvStudentDetails;
    private ProgressBar pbSaving;
    private AppCompatButton btnDone;
    private AppCompatButton btnScanAnother;

    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private CameraControl cameraControl;
    private boolean isTorchOn = false;

    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private final AtomicBoolean isProcessingQr = new AtomicBoolean(false);

    private String currentEventId = "";
    private String currentEventName = "BISU Campus Event";
    private String currentEventDate = "Today";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        View qrTopBar = findViewById(R.id.qrTopBar);
        if (qrTopBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(qrTopBar, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + 12, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        // Initialize UI Views
        previewView     = findViewById(R.id.previewView);
        scanLaserLine   = findViewById(R.id.scanLaserLine);
        btnTorch        = findViewById(R.id.btnTorch);
        btnBackQr       = findViewById(R.id.btnBackQr);
        cardAttendanceVerified = findViewById(R.id.cardAttendanceVerified);
        tvHeaderTitle   = findViewById(R.id.tvHeaderTitle);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);
        tvAttendanceStatus  = findViewById(R.id.tvAttendanceStatus);
        tvEventTitleCard    = findViewById(R.id.tvEventTitleCard);
        tvAttendanceTimestamp = findViewById(R.id.tvAttendanceTimestamp);
        tvStudentDetails    = findViewById(R.id.tvStudentDetails);
        pbSaving        = findViewById(R.id.pbSaving);
        btnDone         = findViewById(R.id.btnDone);
        btnScanAnother  = findViewById(R.id.btnScanAnother);

        // Get Event info passed from Calendar
        String passedEventId   = getIntent().getStringExtra("EVENT_ID");
        String passedEventName = getIntent().getStringExtra("EVENT_NAME");
        String passedEventDate = getIntent().getStringExtra("EVENT_DATE");

        if (passedEventId   != null && !passedEventId.trim().isEmpty())   currentEventId   = passedEventId.trim();
        if (passedEventName != null && !passedEventName.trim().isEmpty()) currentEventName = passedEventName.trim();
        if (passedEventDate != null && !passedEventDate.trim().isEmpty()) currentEventDate = passedEventDate.trim();

        if (tvHeaderTitle != null)    tvHeaderTitle.setText("Scan Event QR Code");
        if (tvHeaderSubtitle != null) tvHeaderSubtitle.setText("Align QR Code within the frame for " + currentEventName);

        if (btnBackQr     != null) btnBackQr.setOnClickListener(v -> finish());
        if (btnTorch      != null) btnTorch.setOnClickListener(v -> toggleTorch());
        if (btnDone       != null) btnDone.setOnClickListener(v -> finish());
        if (btnScanAnother != null) btnScanAnother.setOnClickListener(v -> resetScanner());

        // Setup ML Kit Barcode Scanner
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_ALL_FORMATS)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        cameraExecutor = Executors.newSingleThreadExecutor();
        startScanAnimation();
        checkCameraPermissionAndStart();
    }

    // ──────────────────────────── Camera & Animation ────────────────────────────

    private void startScanAnimation() {
        if (scanLaserLine == null) return;
        scanLaserLine.setVisibility(View.VISIBLE);
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.85f
        );
        animation.setDuration(2200);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        scanLaserLine.startAnimation(animation);
    }

    private void stopScanAnimation() {
        if (scanLaserLine != null) {
            scanLaserLine.clearAnimation();
            scanLaserLine.setVisibility(View.GONE);
        }
    }

    private void toggleTorch() {
        if (cameraControl != null) {
            isTorchOn = !isTorchOn;
            cameraControl.enableTorch(isTorchOn);
            if (btnTorch != null) btnTorch.setAlpha(isTorchOn ? 1.0f : 0.6f);
        }
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("📷 Camera Permission Required")
                        .setMessage("Camera permission is required to scan event QR codes.")
                        .setPositiveButton("Grant Permission", (d, w) -> checkCameraPermissionAndStart())
                        .setNegativeButton("Cancel", (d, w) -> finish())
                        .setCancelable(false)
                        .show();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to initialize camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null || isFinishing() || isDestroyed()) return;
        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            @OptIn(markerClass = ExperimentalGetImage.class)
            public void analyze(@NonNull ImageProxy imageProxy) {
                if (isProcessingQr.get()) { imageProxy.close(); return; }

                android.media.Image mediaImage = imageProxy.getImage();
                if (mediaImage != null) {
                    InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                    barcodeScanner.process(image)
                            .addOnSuccessListener(barcodes -> {
                                if (!barcodes.isEmpty() && !isProcessingQr.get()) {
                                    for (Barcode barcode : barcodes) {
                                        String rawValue = barcode.getRawValue();
                                        if (rawValue != null && !rawValue.trim().isEmpty()) {
                                            if (isProcessingQr.compareAndSet(false, true)) {
                                                mainHandler.post(() -> onQrCodeDetected(rawValue.trim()));
                                                break;
                                            }
                                        }
                                    }
                                }
                            })
                            .addOnFailureListener(e -> { /* silent */ })
                            .addOnCompleteListener(task -> imageProxy.close());
                } else {
                    imageProxy.close();
                }
            }
        });

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            cameraControl = camera.getCameraControl();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Camera binding error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ──────────────────────────── QR Detection Router ───────────────────────────

    /**
     * Triggered when a QR code is detected by the live scanner.
     * Routes to either the Voting flow or the Attendance flow based on the QR prefix.
     */
    private void onQrCodeDetected(String qrContent) {
        triggerHapticFeedback();
        stopScanAnimation();

        // ── VOTING QR ──
        // Format: TCA-VOTE:<pollId>:<pollQuestion>
        if (qrContent.startsWith(PREFIX_VOTE)) {
            String[] parts = qrContent.split(":", 3);
            String pollId       = parts.length >= 2 ? parts[1] : "";
            String pollQuestion = parts.length >= 3 ? parts[2] : "Voting Poll";
            handleVotingQr(pollId, pollQuestion);
            return;
        }

        // ── ATTENDANCE QR ──
        String detectedEventName = currentEventName;
        String detectedEventId   = currentEventId;

        try {
            if (qrContent.startsWith("{") && qrContent.endsWith("}")) {
                JSONObject json = new JSONObject(qrContent);
                if (json.has("eventName")) detectedEventName = json.getString("eventName");
                if (json.has("title"))     detectedEventName = json.getString("title");
                if (json.has("eventId"))   detectedEventId   = json.getString("eventId");
                if (json.has("id"))        detectedEventId   = json.getString("id");
            } else if (qrContent.startsWith(PREFIX_EVENT) || qrContent.startsWith(PREFIX_EVENT_ALT)) {
                String[] parts = qrContent.split(":", 3);
                if (parts.length >= 2) detectedEventId   = parts[1];
                if (parts.length >= 3) detectedEventName = parts[2];
            } else if (!qrContent.isEmpty()) {
                if (currentEventName.equals("BISU Campus Event") || currentEventName.isEmpty()) {
                    detectedEventName = qrContent;
                }
            }
        } catch (Exception e) {
            // Fallback gracefully to default/current event
        }

        saveAttendanceDirectToDatabase(qrContent, detectedEventId, detectedEventName);
    }

    // ──────────────────────────── VOTING FLOW ───────────────────────────────────

    /**
     * Fetches the voting poll from Firestore and shows the vote-casting dialog.
     */
    private void handleVotingQr(String pollId, String pollQuestion) {
        if (pollId.isEmpty()) {
            Toast.makeText(this, "⚠️ Invalid voting QR code.", Toast.LENGTH_SHORT).show();
            resetScanner();
            return;
        }

        if (pbSaving != null) pbSaving.setVisibility(View.VISIBLE);
        if (tvAttendanceStatus != null) tvAttendanceStatus.setText("⏳ Loading poll...");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "❌ Please log in to vote.", Toast.LENGTH_LONG).show();
            resetScanner();
            return;
        }

        final String uid = user.getUid();
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Check if user already voted
        db.collection("voting_polls").document(pollId)
                .get()
                .addOnSuccessListener(pollDoc -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (pbSaving != null) pbSaving.setVisibility(View.GONE);

                    if (!pollDoc.exists()) {
                        Toast.makeText(this, "⚠️ Poll not found.", Toast.LENGTH_SHORT).show();
                        resetScanner();
                        return;
                    }

                    // Check if user already voted in this poll
                    @SuppressWarnings("unchecked")
                    List<String> voters = (List<String>) pollDoc.get("voterUids");
                    if (voters != null && voters.contains(uid)) {
                        showAlreadyVotedDialog(pollDoc.getString("question") != null
                                ? pollDoc.getString("question") : pollQuestion);
                        return;
                    }

                    // Check poll is still active
                    Boolean active = pollDoc.getBoolean("active");
                    if (active != null && !active) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("🔒 Poll Closed")
                                .setMessage("This voting poll is no longer accepting votes.")
                                .setPositiveButton("OK", (d, w) -> { d.dismiss(); resetScanner(); })
                                .show();
                        return;
                    }

                    // 2. Show vote casting dialog
                    @SuppressWarnings("unchecked")
                    List<String> options = (List<String>) pollDoc.get("options");
                    String question = pollDoc.getString("question");
                    if (question == null || question.isEmpty()) question = pollQuestion;

                    showCastVoteDialog(pollId, question, options, uid, db);
                })
                .addOnFailureListener(e -> {
                    if (pbSaving != null) pbSaving.setVisibility(View.GONE);
                    Toast.makeText(this, "❌ Failed to load poll: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetScanner();
                });
    }

    /**
     * Displays a radio-button dialog so the student can pick a candidate/option to vote for.
     */
    private void showCastVoteDialog(String pollId, String question, List<String> options,
                                     String uid, FirebaseFirestore db) {
        if (options == null || options.isEmpty()) {
            Toast.makeText(this, "⚠️ This poll has no options.", Toast.LENGTH_SHORT).show();
            resetScanner();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cast_vote, null);

        TextView tvPollQuestion = dialogView.findViewById(R.id.tvPollQuestion);
        RadioGroup rgOptions    = dialogView.findViewById(R.id.rgVoteOptions);
        TextView btnCancelVote  = dialogView.findViewById(R.id.btnCancelVote);
        TextView btnSubmitVote  = dialogView.findViewById(R.id.btnSubmitVote);

        if (tvPollQuestion != null) tvPollQuestion.setText(question);

        // Dynamically add radio buttons for each option
        if (rgOptions != null) {
            for (int i = 0; i < options.size(); i++) {
                RadioButton rb = new RadioButton(this);
                rb.setId(View.generateViewId());
                rb.setText(options.get(i));
                rb.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                rb.setTextSize(14f);
                rb.setPadding(8, 16, 8, 16);
                rb.setTag(options.get(i));
                rgOptions.addView(rb);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (btnCancelVote != null) {
            btnCancelVote.setOnClickListener(v -> {
                dialog.dismiss();
                resetScanner();
            });
        }

        if (btnSubmitVote != null) {
            btnSubmitVote.setOnClickListener(v -> {
                if (rgOptions == null) return;
                int selectedId = rgOptions.getCheckedRadioButtonId();
                if (selectedId == -1) {
                    Toast.makeText(this, "⚠️ Please select an option first.", Toast.LENGTH_SHORT).show();
                    return;
                }
                RadioButton selectedRb = dialogView.findViewById(selectedId);
                String selectedOption = (String) selectedRb.getTag();

                btnSubmitVote.setEnabled(false);
                btnSubmitVote.setText("Submitting...");

                submitVote(pollId, selectedOption, uid, db, dialog);
            });
        }

        dialog.show();
    }

    /**
     * Writes the vote to Firestore atomically:
     *  - Increments votesCount.<option>
     *  - Increments totalVotes
     *  - Adds uid to voterUids (prevents double-voting)
     */
    private void submitVote(String pollId, String selectedOption, String uid,
                             FirebaseFirestore db, AlertDialog dialog) {

        // Fetch user profile first for richer vote record
        db.collection("users").document(uid).get().addOnCompleteListener(userTask -> {
            String studentName = "";
            String studentId   = "";
            String dept        = "";

            if (userTask.isSuccessful() && userTask.getResult() != null && userTask.getResult().exists()) {
                DocumentSnapshot doc = userTask.getResult();
                studentName = doc.getString("name");
                if (studentName == null || studentName.isEmpty()) studentName = doc.getString("fullName");
                if (studentName == null) studentName = "";
                studentId = doc.getString("studentId");
                if (studentId == null) studentId = "";
                dept = doc.getString("department");
                if (dept == null) dept = "";
            }

            final String finalName = studentName;
            final String finalStudentId = studentId;
            final String finalDept = dept;

            // Atomic vote increment using FieldValue
            Map<String, Object> voteUpdate = new HashMap<>();
            voteUpdate.put("votesCount." + selectedOption, FieldValue.increment(1));
            voteUpdate.put("totalVotes", FieldValue.increment(1));
            voteUpdate.put("voterUids", FieldValue.arrayUnion(uid));

            db.collection("voting_polls").document(pollId)
                    .update(voteUpdate)
                    .addOnSuccessListener(aVoid -> {
                        // Also log detailed vote record
                        Map<String, Object> voteRecord = new HashMap<>();
                        voteRecord.put("pollId", pollId);
                        voteRecord.put("voterUid", uid);
                        voteRecord.put("voterName", finalName);
                        voteRecord.put("studentId", finalStudentId);
                        voteRecord.put("department", finalDept);
                        voteRecord.put("selectedOption", selectedOption);
                        voteRecord.put("votedAt", FieldValue.serverTimestamp());

                        db.collection("vote_records").add(voteRecord);

                        if (isFinishing() || isDestroyed()) return;
                        dialog.dismiss();
                        showVoteSuccessUI(selectedOption);
                    })
                    .addOnFailureListener(e -> {
                        if (isFinishing() || isDestroyed()) return;
                        dialog.dismiss();
                        Toast.makeText(this, "❌ Vote failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        resetScanner();
                    });
        });
    }

    private void showAlreadyVotedDialog(String question) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("🗳️ Already Voted")
                .setMessage("You have already cast your vote for:\n\n\"" + question + "\"")
                .setPositiveButton("OK", (d, w) -> { d.dismiss(); resetScanner(); })
                .show();
    }

    /**
     * Shows the success confirmation card after a successful vote submission.
     */
    private void showVoteSuccessUI(String chosenOption) {
        if (cardAttendanceVerified != null) cardAttendanceVerified.setVisibility(View.VISIBLE);

        if (tvAttendanceStatus != null)   tvAttendanceStatus.setText("✅ Vote Cast Successfully!");
        if (tvEventTitleCard != null)     tvEventTitleCard.setText("Your vote: " + chosenOption);

        String currentTime = new SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()).format(new Date());
        if (tvAttendanceTimestamp != null) tvAttendanceTimestamp.setText("Voted at: " + currentTime);

        if (tvStudentDetails != null) {
            tvStudentDetails.setVisibility(View.VISIBLE);
            tvStudentDetails.setText("🎉 Thank you for participating!");
        }

        if (btnDone != null)       btnDone.setVisibility(View.VISIBLE);
        if (btnScanAnother != null) btnScanAnother.setVisibility(View.GONE); // No re-scan after voting

        Toast.makeText(this, "🗳️ Your vote has been recorded!", Toast.LENGTH_SHORT).show();
    }

    // ──────────────────────────── ATTENDANCE FLOW ───────────────────────────────

    /**
     * Directly writes the attendance record to Firebase Firestore.
     */
    private void saveAttendanceDirectToDatabase(String rawQr, String eventId, String eventName) {
        if (pbSaving != null) pbSaving.setVisibility(View.VISIBLE);
        if (tvAttendanceStatus != null) tvAttendanceStatus.setText("⏳ Saving attendance to database...");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "❌ Error: User not authenticated.", Toast.LENGTH_LONG).show();
            resetScanner();
            return;
        }

        String uid       = user.getUid();
        String userEmail = user.getEmail() != null ? user.getEmail() : "Student";
        String userName  = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                ? user.getDisplayName() : userEmail;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).get().addOnCompleteListener(userTask -> {
            String finalStudentName = userName;
            String studentIdNumber  = "";
            String department       = "";

            if (userTask.isSuccessful() && userTask.getResult() != null && userTask.getResult().exists()) {
                DocumentSnapshot doc = userTask.getResult();
                String dbName = doc.getString("name");
                if (dbName == null || dbName.isEmpty()) dbName = doc.getString("fullName");
                if (dbName != null && !dbName.isEmpty()) finalStudentName = dbName;
                studentIdNumber = doc.getString("studentId");
                department      = doc.getString("department");
            }

            String cleanEventName = eventName.replaceAll("[^a-zA-Z0-9]", "");
            String docId = uid + "_" + cleanEventName;

            Map<String, Object> attendanceData = new HashMap<>();
            attendanceData.put("studentUid",    uid);
            attendanceData.put("studentEmail",  userEmail);
            attendanceData.put("studentName",   finalStudentName);
            if (studentIdNumber != null && !studentIdNumber.isEmpty())
                attendanceData.put("studentIdNumber", studentIdNumber);
            if (department != null && !department.isEmpty())
                attendanceData.put("department", department);
            attendanceData.put("eventId",          eventId != null ? eventId : "");
            attendanceData.put("eventName",         eventName);
            attendanceData.put("qrCode",            rawQr);
            attendanceData.put("verificationType",  "QR_CODE");
            attendanceData.put("status",            "Present");
            attendanceData.put("verifiedAt",        FieldValue.serverTimestamp());
            attendanceData.put("verifiedViaClient", true);

            final String finalDisplayName        = finalStudentName;
            final String finalResolvedEventName  = eventName;

            db.collection("event_attendance")
                    .document(docId)
                    .set(attendanceData)
                    .addOnSuccessListener(aVoid -> {
                        if (isFinishing() || isDestroyed()) return;
                        if (pbSaving != null) pbSaving.setVisibility(View.GONE);
                        showAttendanceSuccessUI(finalResolvedEventName, finalDisplayName, userEmail, rawQr);
                    })
                    .addOnFailureListener(e -> {
                        if (isFinishing() || isDestroyed()) return;
                        if (pbSaving != null) pbSaving.setVisibility(View.GONE);
                        Toast.makeText(QRScannerActivity.this, "❌ Database Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        resetScanner();
                    });
        });
    }

    private void showAttendanceSuccessUI(String eventName, String studentName, String email, String rawQr) {
        if (cardAttendanceVerified != null) cardAttendanceVerified.setVisibility(View.VISIBLE);

        if (tvAttendanceStatus != null)   tvAttendanceStatus.setText("✅ Attendance Verified & Recorded!");
        if (tvEventTitleCard != null)     tvEventTitleCard.setText("Event: " + eventName);

        String currentTime = new SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()).format(new Date());
        if (tvAttendanceTimestamp != null) tvAttendanceTimestamp.setText("Time: " + currentTime + " (Direct Sync)");

        if (tvStudentDetails != null) {
            tvStudentDetails.setVisibility(View.VISIBLE);
            tvStudentDetails.setText("Student: " + studentName + " (" + email + ")");
        }

        if (btnDone != null)       btnDone.setVisibility(View.VISIBLE);
        if (btnScanAnother != null) btnScanAnother.setVisibility(View.VISIBLE);

        Toast.makeText(this, "🎉 Attendance recorded directly in database!", Toast.LENGTH_SHORT).show();
    }

    // ──────────────────────────── Shared Utilities ──────────────────────────────

    private void resetScanner() {
        isProcessingQr.set(false);
        if (cardAttendanceVerified != null) cardAttendanceVerified.setVisibility(View.GONE);
        if (btnDone        != null) btnDone.setVisibility(View.GONE);
        if (btnScanAnother != null) btnScanAnother.setVisibility(View.GONE);
        if (pbSaving       != null) pbSaving.setVisibility(View.GONE);
        startScanAnimation();
    }

    private void triggerHapticFeedback() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //noinspection deprecation
                    vibrator.vibrate(120);
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor  != null) cameraExecutor.shutdown();
        if (barcodeScanner  != null) barcodeScanner.close();
    }
}
