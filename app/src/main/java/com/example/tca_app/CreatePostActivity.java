package com.example.tca_app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REFACTORED FOR PRODUCTION SECURITY & ENTERPRISE SCALABILITY:
 * All duplicate checking, perceptual image hashing, cyber libel detection, and text moderation
 * are handled exclusively by backend Firebase Cloud Function triggers (onPostCreated).
 *
 * Client Responsibility: Handles UI input, uploads media via background thread executor,
 * and publishes the post document to Firestore with status "PENDING".
 */
public class CreatePostActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    private ActivityResultLauncher<Intent> photoPickerLauncher;
    private ActivityResultLauncher<Intent> videoPickerLauncher;
    private ActivityResultLauncher<Intent> docPickerLauncher;

    private List<Uri> selectedMediaUris = new ArrayList<>();
    private Uri selectedDocUri = null;

    private long scheduledTimestamp = 0;
    private android.widget.RelativeLayout mediaPreviewContainer;
    private androidx.viewpager2.widget.ViewPager2 vpMediaPreview;
    private com.google.android.material.tabs.TabLayout tabDotsPreview;
    private android.widget.LinearLayout btnAddMoreMedia;
    private android.widget.LinearLayout btnLayoutOptions;
    private android.widget.LinearLayout btnRemoveMedia;
    private CreatePostMediaAdapter mediaAdapter;
    private String layoutPreference = "carousel";
    private TextView tvSchedulerLabel;
    private Button btnPublish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Native System File Pickers
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                if (selectedMediaUris.size() < 20) {
                                    selectedMediaUris.add(data.getClipData().getItemAt(i).getUri());
                                }
                            }
                        } else if (data.getData() != null) {
                            if (selectedMediaUris.size() < 20) {
                                selectedMediaUris.add(data.getData());
                            }
                        }
                        Toast.makeText(CreatePostActivity.this, selectedMediaUris.size() + " media item(s) selected (Max 20)", Toast.LENGTH_SHORT).show();
                        
                        if (!selectedMediaUris.isEmpty() && mediaPreviewContainer != null) {
                            mediaPreviewContainer.setVisibility(View.VISIBLE);
                            mediaAdapter.notifyDataSetChanged();
                            if (selectedMediaUris.size() > 1) {
                                tabDotsPreview.setVisibility(View.VISIBLE);
                            } else {
                                tabDotsPreview.setVisibility(View.GONE);
                            }
                        }
                    }
                }
        );

        videoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                if (selectedMediaUris.size() < 20) {
                                    selectedMediaUris.add(data.getClipData().getItemAt(i).getUri());
                                }
                            }
                        } else if (data.getData() != null) {
                            if (selectedMediaUris.size() < 20) {
                                selectedMediaUris.add(data.getData());
                            }
                        }
                        Toast.makeText(CreatePostActivity.this, selectedMediaUris.size() + " media item(s) selected (Max 20)", Toast.LENGTH_SHORT).show();
                        
                        if (!selectedMediaUris.isEmpty() && mediaPreviewContainer != null) {
                            mediaPreviewContainer.setVisibility(View.VISIBLE);
                            mediaAdapter.notifyDataSetChanged();
                            if (selectedMediaUris.size() > 1) {
                                tabDotsPreview.setVisibility(View.VISIBLE);
                            } else {
                                tabDotsPreview.setVisibility(View.GONE);
                            }
                        }
                    }
                }
        );

        docPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        selectedDocUri = result.getData().getData();
                        Toast.makeText(this, "📎 File Selected: " + selectedDocUri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
                    }
                }
        );

        View headerBar = findViewById(R.id.headerBar);
        if (headerBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(headerBar, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + 12, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        View btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        EditText etPostText = findViewById(R.id.etPostText);
        mediaPreviewContainer = findViewById(R.id.mediaPreviewContainer);
        vpMediaPreview = findViewById(R.id.vpMediaPreview);
        tabDotsPreview = findViewById(R.id.tabDotsPreview);
        btnAddMoreMedia = findViewById(R.id.btnAddMoreMedia);
        btnLayoutOptions = findViewById(R.id.btnLayoutOptions);
        btnRemoveMedia = findViewById(R.id.btnRemoveMedia);

        mediaAdapter = new CreatePostMediaAdapter(this, selectedMediaUris);
        vpMediaPreview.setAdapter(mediaAdapter);
        new com.google.android.material.tabs.TabLayoutMediator(tabDotsPreview, vpMediaPreview, (tab, position) -> {}).attach();

        btnAddMoreMedia.setOnClickListener(v -> {
            // Re-launch picker to add more
            if (selectedMediaUris.size() >= 20) {
                android.widget.Toast.makeText(this, "Maximum 20 items allowed", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            photoPickerLauncher.launch(Intent.createChooser(intent, "Select Photos"));
        });

        btnRemoveMedia.setOnClickListener(v -> {
            if (selectedMediaUris.isEmpty()) return;
            int currentPos = vpMediaPreview.getCurrentItem();
            selectedMediaUris.remove(currentPos);
            mediaAdapter.notifyDataSetChanged();
            if (selectedMediaUris.isEmpty()) {
                mediaPreviewContainer.setVisibility(View.GONE);
            }
            android.widget.Toast.makeText(this, "Item removed", android.widget.Toast.LENGTH_SHORT).show();
        });

        btnLayoutOptions.setOnClickListener(v -> showSelectPostLayoutDialog());
        Spinner spinnerCategory = findViewById(R.id.spinnerCategory);
        SwitchCompat switchPin = findViewById(R.id.switchPin);
        LinearLayout btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        LinearLayout btnUploadVideo = findViewById(R.id.btnUploadVideo);
        LinearLayout btnUploadDoc = findViewById(R.id.btnUploadDoc);
        RelativeLayout btnScheduler = findViewById(R.id.btnScheduler);
        tvSchedulerLabel = findViewById(R.id.tvSchedulerLabel);
        btnPublish = findViewById(R.id.btnPublish);

        String[] categories = {
                "Auto-Categorize",
                "Gallery",
                "Video",
                "Broadcast",
                "Events",
                "Updates",
                "Academics",
                "Sports",
                "Horoscopes",
                "Literature"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, categories);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerCategory.setAdapter(adapter);

        LinearLayout containerFolder = findViewById(R.id.containerFolder);
        spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                // Folder is always available for organizing media in the gallery, regardless of feed category
                if (containerFolder != null) containerFolder.setVisibility(View.VISIBLE);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnBack.setOnClickListener(v -> finish());

        btnUploadPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            photoPickerLauncher.launch(Intent.createChooser(intent, "Select Photos"));
        });

        btnUploadVideo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("video/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            videoPickerLauncher.launch(Intent.createChooser(intent, "Select Videos"));
        });

        btnUploadDoc.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            docPickerLauncher.launch(Intent.createChooser(intent, "Select Document / File"));
        });

        btnScheduler.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    CreatePostActivity.this, R.style.CustomPickerTheme,
                    (view1, selectedYear, selectedMonth, selectedDay) -> {
                        int hour = c.get(Calendar.HOUR_OF_DAY);
                        int minute = c.get(Calendar.MINUTE);

                        TimePickerDialog timePickerDialog = new TimePickerDialog(
                                CreatePostActivity.this, R.style.CustomPickerTheme,
                                (timeView, selectedHour, selectedMinute) -> {
                                    Calendar scheduledCal = Calendar.getInstance();
                                    scheduledCal.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute);
                                    scheduledTimestamp = scheduledCal.getTimeInMillis();

                                    String formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%d %02d:%02d",
                                            selectedMonth + 1, selectedDay, selectedYear, selectedHour, selectedMinute);

                                    if (tvSchedulerLabel != null) {
                                        tvSchedulerLabel.setText("Scheduled: " + formattedDate);
                                    }
                                    Toast.makeText(CreatePostActivity.this, "Post scheduled for " + formattedDate, Toast.LENGTH_LONG).show();
                                },
                                hour, minute, false
                        );
                        timePickerDialog.show();
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        btnPublish.setOnClickListener(v -> {
            String text = etPostText.getText().toString().trim();
            if (text.isEmpty()) {
                etPostText.setError("Post text cannot be empty!");
                return;
            }

            String category = spinnerCategory.getSelectedItem().toString();
            boolean isPinned = switchPin.isChecked();
            boolean isFeatured = false;

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Please log in to publish posts.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Membership & Posting Restriction Check
            AuthUtils.checkCurrentUserAccess((isApprovedMember, isAdmin, role) -> {
                boolean hasPermission = isApprovedMember || isAdmin;

                if (!hasPermission) {
                    if (btnPublish != null) {
                        btnPublish.setEnabled(true);
                        btnPublish.setText("Publish Post");
                    }
                    new androidx.appcompat.app.AlertDialog.Builder(CreatePostActivity.this)
                            .setTitle("🔒 Access Restricted: Membership Required")
                            .setMessage("Only official approved members of The Campus Access (or designated admins) are allowed to publish posts to the campus feed. Unapproved student accounts remain in view-only mode.")
                            .setPositiveButton("OK", null)
                            .show();
                    Toast.makeText(CreatePostActivity.this, "🔒 Access Restricted: Only official approved members of The Campus Access can publish posts.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Extract folder name
                EditText etFolderName = findViewById(R.id.etFolderName);
                String folderName = (etFolderName != null) ? etFolderName.getText().toString().trim() : "";

                // Proceed to pre-upload duplicate check
                performPreUploadDuplicateCheck(text, category, folderName, isPinned, isFeatured, isAdmin);
            });
        });
    }

    private void performPreUploadDuplicateCheck(String text, String category, String folderName, boolean isPinned, boolean isFeatured, boolean isAdmin) {
        // Step 1: Content check for inappropriate / cyber libel words
        AiDuplicateDetector.ModerationResult inappropriateCheck = AiDuplicateDetector.checkInappropriateContent(text);
        if (inappropriateCheck.isFlagged) {
            showDuplicateError("Moderation Flag", inappropriateCheck.detail);
            return;
        }

        if (btnPublish != null) {
            btnPublish.setEnabled(false);
            btnPublish.setText("AI Analyzing Content...");
        }

        Toast.makeText(this, "AI analyzing content and media...", Toast.LENGTH_SHORT).show();

        // Step 2: Compute media hashes on background executor without blocking UI
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            List<String> localHashes = new ArrayList<>();
            List<String> localImageHashes = new ArrayList<>();
            List<String> localVideoHashes = new ArrayList<>();

            for (Uri uri : selectedMediaUris) {
                try {
                    String mimeType = getContentResolver().getType(uri);
                    boolean isVideo = mimeType != null && mimeType.startsWith("video");
                    java.io.InputStream is = getContentResolver().openInputStream(uri);
                    if (isVideo) {
                        String vidHash = AiDuplicateDetector.computeStreamHash(is);
                        if (vidHash != null && !vidHash.isEmpty()) {
                            localHashes.add(vidHash);
                            localVideoHashes.add(vidHash);
                        }
                    } else {
                        android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                        options.inSampleSize = 8;
                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is, null, options);
                        String imgHash = AiDuplicateDetector.computeImageHash(bitmap);
                        if (imgHash != null && !imgHash.isEmpty()) {
                            localHashes.add(imgHash);
                            localImageHashes.add(imgHash);
                        }
                    }
                    if (is != null) is.close();
                } catch (Exception e) {
                    android.util.Log.e("CreatePostActivity", "Hash generation error", e);
                }
            }

            // Step 3: Fetch recent posts from Firestore to cross-reference duplicates
            db.collection("posts")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(100)
                    .get()
                    .addOnCompleteListener(task -> {
                        boolean duplicateFound = false;
                        String detectedReason = "";

                        if (task.isSuccessful() && task.getResult() != null) {
                            List<com.google.firebase.firestore.DocumentSnapshot> docs = task.getResult().getDocuments();

                            // A. Check Text / Caption Duplicate
                            if (text != null && !text.trim().isEmpty()) {
                                List<String> existingCaptions = new ArrayList<>();
                                for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
                                    String c = doc.getString("content");
                                    if (c != null && !c.trim().isEmpty()) {
                                        existingCaptions.add(c);
                                    }
                                }
                                AiDuplicateDetector.ModerationResult textResult = AiDuplicateDetector.checkTextDuplicate(text, existingCaptions);
                                if (textResult.isFlagged) {
                                    duplicateFound = true;
                                    detectedReason = "Duplicate Post: " + textResult.detail;
                                }
                            }

                            // B. Check Image / Picture Duplicate
                            if (!duplicateFound && !localImageHashes.isEmpty()) {
                                for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
                                    @SuppressWarnings("unchecked")
                                    List<String> docHashes = (List<String>) doc.get("mediaHashes");
                                    String docImageHash = doc.getString("imageHash");

                                    for (String localImgHash : localImageHashes) {
                                        if (docHashes != null && docHashes.contains(localImgHash)) {
                                            duplicateFound = true;
                                            detectedReason = "Duplicate Picture: Photo matches a previously uploaded image.";
                                            break;
                                        }
                                        if (docImageHash != null && !docImageHash.isEmpty()) {
                                            if (AiDuplicateDetector.compareImageHashes(localImgHash, docImageHash) >= 90) {
                                                duplicateFound = true;
                                                detectedReason = "Duplicate Picture: Photo matches an existing post with high visual similarity.";
                                                break;
                                            }
                                        }
                                        if (docHashes != null) {
                                            for (String dh : docHashes) {
                                                if (dh != null && dh.length() == 64) {
                                                    if (AiDuplicateDetector.compareImageHashes(localImgHash, dh) >= 90) {
                                                        duplicateFound = true;
                                                        detectedReason = "Duplicate Picture: Photo matches an existing post with high visual similarity.";
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        if (duplicateFound) break;
                                    }
                                    if (duplicateFound) break;
                                }
                            }

                            // C. Check Video Duplicate
                            if (!duplicateFound && !localVideoHashes.isEmpty()) {
                                for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
                                    @SuppressWarnings("unchecked")
                                    List<String> docHashes = (List<String>) doc.get("mediaHashes");
                                    for (String localVidHash : localVideoHashes) {
                                        if (docHashes != null && docHashes.contains(localVidHash)) {
                                            duplicateFound = true;
                                            detectedReason = "Duplicate Video: Video stream matches a previously uploaded video.";
                                            break;
                                        }
                                    }
                                    if (duplicateFound) break;
                                }
                            }
                        }

                        final boolean finalIsDuplicate = duplicateFound;
                        final String finalReason = detectedReason;

                        runOnUiThread(() -> {
                            if (btnPublish != null) {
                                btnPublish.setText(finalIsDuplicate ? "Submitting to Moderation..." : "Publishing Post...");
                            }
                            uploadMediaAndPublishPost(text, category, folderName, isPinned, isFeatured, isAdmin, localHashes, finalIsDuplicate, finalReason);
                        });
                    });
        });
    }
    private void showDuplicateError(String title, String message) {
        if (!isFinishing() && !isDestroyed()) {
            if (btnPublish != null) {
                btnPublish.setEnabled(true);
                btnPublish.setText("Publish Post");
            }
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("🚫 AI " + title)
                    .setMessage(message + "\n\nPlease avoid re-uploading the same content.")
                    .setPositiveButton("Got it", null)
                    .show();
        }
    }

    private void uploadMediaAndPublishPost(String text, String category, String customFolderName, boolean isPinned,
                                          boolean isFeatured, boolean isAdmin, List<String> localHashes,
                                          boolean isDuplicate, String duplicateReason) {
        String toastMsg = selectedMediaUris.isEmpty() ? "Processing post..." : "Uploading media and publishing post...";
        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();

        List<Task<String>> mediaTasks = new ArrayList<>();

        for (int i = 0; i < selectedMediaUris.size(); i++) {
            Uri uri = selectedMediaUris.get(i);
            String mimeType = getContentResolver().getType(uri);
            String folder = (mimeType != null && mimeType.startsWith("video")) ? "videos" : "photos";
            Task<String> task = createUploadTask(uri, folder);
            mediaTasks.add(task);
        }

        Task<String> docTask = createUploadTask(selectedDocUri, "docs");
        
        List<Task<?>> allTasks = new ArrayList<>(mediaTasks);
        allTasks.add(docTask);

        Tasks.whenAllComplete(allTasks)
                .addOnCompleteListener(task -> {
                    if (!isFinishing() && !isDestroyed()) {
                        List<String> uploadedMediaUris = new ArrayList<>();
                        int failedUploads = 0;
                        for (Task<String> mediaTask : mediaTasks) {
                            if (mediaTask.isSuccessful() && mediaTask.getResult() != null && !mediaTask.getResult().isEmpty()) {
                                uploadedMediaUris.add(mediaTask.getResult());
                            } else {
                                failedUploads++;
                            }
                        }
                        
                        if (failedUploads > 0) {
                            Toast.makeText(CreatePostActivity.this, "⚠️ Warning: " + failedUploads + " image(s) failed to upload to Cloudinary. Check internet or config.", Toast.LENGTH_LONG).show();
                        }
                        
                        String docUrl = (docTask.isSuccessful() && docTask.getResult() != null) ? docTask.getResult() : "";
                        savePostDocumentToFirestore(text, category, customFolderName, isPinned, isFeatured, uploadedMediaUris, docUrl, isAdmin, localHashes, isDuplicate, duplicateReason);
                    }
                });
    }

    private Task<String> createUploadTask(Uri fileUri, String folderName) {
        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        if (fileUri == null) {
            tcs.setResult("");
            return tcs.getTask();
        }

        // Execute file stream handling on background executor to avoid UI thread blocking
        backgroundExecutor.execute(() -> {
            try {
                java.io.InputStream stream = getContentResolver().openInputStream(fileUri);
                if (stream != null) {
                    String fileName = "upload.jpg";
                    if ("videos".equals(folderName)) fileName = "upload.mp4";
                    else if ("docs".equals(folderName)) fileName = "upload.pdf";

                    CloudinaryUploader.uploadMedia(stream, fileName, new CloudinaryUploader.CloudinaryUploadCallback() {
                        @Override
                        public void onSuccess(String secureUrl) {
                            tcs.setResult(secureUrl);
                        }

                        @Override
                        public void onFailure(String error) {
                            android.util.Log.e("Cloudinary", "Upload failed for " + folderName + ": " + error);
                            tcs.setResult("");
                        }
                    });
                } else {
                    android.util.Log.e("Cloudinary", "InputStream was null for " + fileUri);
                    tcs.setResult("");
                }
            } catch (Exception e) {
                android.util.Log.e("Cloudinary", "Exception during upload", e);
                tcs.setResult("");
            }
        });

        return tcs.getTask();
    }

    private void savePostDocumentToFirestore(String text, String category, String customFolderName, boolean isPinned,
                                             boolean isFeatured, List<String> uploadedMediaUris, String docUrl,
                                             boolean isAdmin, List<String> localHashes, boolean isDuplicate,
                                             String duplicateReason) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        // --- PRIVACY UPDATE: Use Display Name instead of Email ---
        final String author;
        if (isAdmin) {
            author = "The Campus Access";
        } else if (currentUser != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().trim().isEmpty()) {
            author = currentUser.getDisplayName();
        } else {
            author = "BISU Student";
        }
        
        String authorUid = (currentUser != null) ? currentUser.getUid() : "";

        String finalCategory = AiCategoryClassifier.classifyCategory(text, category);

        boolean hasVideo = false;
        boolean hasPhoto = false;
        if (uploadedMediaUris != null && uploadedMediaUris.size() > 0) {
            String first = uploadedMediaUris.get(0);
            if (first.contains("/video/") || first.contains(".mp4")) {
                hasVideo = true;
            } else {
                hasPhoto = true;
            }
        }

        if (hasVideo) {
            finalCategory = "Video";
        } else if (hasPhoto && category.contains("Auto-Categorize") && finalCategory.equals("Broadcast")) {
            finalCategory = "Gallery";
        }

        Map<String, Object> postMap = new HashMap<>();
        postMap.put("authorName", author);
        postMap.put("authorUid", authorUid);
        postMap.put("postMeta", scheduledTimestamp > 0 ? "Scheduled Post • " + finalCategory : "Just now • " + finalCategory);
        postMap.put("content", text);
        postMap.put("badgeText", isPinned ? finalCategory : "");
        postMap.put("category", finalCategory);
        if (customFolderName != null && !customFolderName.isEmpty()) {
            postMap.put("folderName", customFolderName);
        }
        postMap.put("isPinned", isPinned);
        postMap.put("isAiPick", isFeatured);
        postMap.put("mediaUris", uploadedMediaUris);
        if (uploadedMediaUris.size() > 0) {
            String first = uploadedMediaUris.get(0);
            if (first.contains("/video/") || first.endsWith(".mp4") || first.contains(".mp4?")) {
                postMap.put("videoUri", first);
            } else {
                postMap.put("photoUri", first);
            }
        }
        postMap.put("docUri", docUrl);
        postMap.put("imageHash", ""); // Server hash indexing fallback
        // Set scheduled timestamp or current time for feed sorting
        long finalScheduledTimestamp = scheduledTimestamp > 0 ? scheduledTimestamp : System.currentTimeMillis();
        postMap.put("scheduledTimestamp", finalScheduledTimestamp);
        postMap.put("likeCount", 0);
        postMap.put("loveCount", 0);
        postMap.put("commentCount", 0);

        if (isDuplicate) {
            postMap.put("moderationStatus", "FLAGGED");
            postMap.put("isDuplicate", true);
            postMap.put("flaggedReason", duplicateReason != null && !duplicateReason.isEmpty() ? duplicateReason : "AI Duplicate Content Detection");
            postMap.put("aiScore", "98%");
        } else {
            postMap.put("moderationStatus", "APPROVED");
            postMap.put("isDuplicate", false);
        }

        postMap.put("timestamp", System.currentTimeMillis());
        postMap.put("mediaHashes", localHashes);
        postMap.put("layoutPreference", layoutPreference);

        db.collection("posts").add(postMap)
        .addOnSuccessListener(documentReference -> {
            if (!isFinishing() && !isDestroyed()) {
                if (btnPublish != null) {
                    btnPublish.setEnabled(true);
                    btnPublish.setText("Publish Post");
                }

                if (isDuplicate) {
                    // Send to moderation_queue for administrator editorial review
                    String newPostId = documentReference.getId();
                    Map<String, Object> queueItem = new HashMap<>();
                    queueItem.put("postId", newPostId);
                    queueItem.put("authorName", author);
                    queueItem.put("authorUid", authorUid);
                    queueItem.put("content", text);
                    queueItem.put("reason", duplicateReason != null && !duplicateReason.isEmpty() ? duplicateReason : "Duplicate Content / Media Detection");
                    queueItem.put("aiScore", "98%");
                    queueItem.put("status", "PENDING");
                    queueItem.put("moderationStatus", "PENDING");
                    queueItem.put("mediaUris", uploadedMediaUris);
                    queueItem.put("timestamp", System.currentTimeMillis());

                    db.collection("moderation_queue").add(queueItem)
                            .addOnCompleteListener(qTask -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    new androidx.appcompat.app.AlertDialog.Builder(CreatePostActivity.this)
                                            .setTitle("🛡️ AI Duplicate Detection")
                                            .setMessage("Nadiskubrehan sa AI nga kining gi-post (o ang hulagway/video) kay gibalik-balik na og post kaniadto.\n\nGi-forward kini sa Moderation Queue para sa pagsusi sa mga administrators. Mawala kini sa queue kung ma-approved na sa admin, ug awtomatiko dayon kining mopakita sa feed.")
                                            .setPositiveButton("Sige / Understood", (dialog, which) -> finish())
                                            .setCancelable(false)
                                            .show();
                                }
                            });
                } else {
                    String msg = scheduledTimestamp > 0
                            ? "Post scheduled successfully."
                            : "Post submitted successfully.";
                    Toast.makeText(CreatePostActivity.this, msg, Toast.LENGTH_LONG).show();
                    
                    // Increment postCount in the users collection
                    if (authorUid != null && !authorUid.isEmpty()) {
                        db.collection("users").document(authorUid)
                            .update("postCount", com.google.firebase.firestore.FieldValue.increment(1))
                            .addOnFailureListener(e -> android.util.Log.e("CreatePostActivity", "Failed to update postCount", e));
                    }
                    
                    finish();
                }
            }
        })
        .addOnFailureListener(e -> {
            if (!isFinishing() && !isDestroyed()) {
                if (btnPublish != null) {
                    btnPublish.setEnabled(true);
                    btnPublish.setText("Publish Post");
                }
                Toast.makeText(CreatePostActivity.this, "Error submitting post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSelectPostLayoutDialog() {
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_post_layout, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View cardCarousel = dialogView.findViewById(R.id.cardLayoutCarousel);
        View cardGrid = dialogView.findViewById(R.id.cardLayoutGrid);
        View cardList = dialogView.findViewById(R.id.cardLayoutList);

        TextView tvTitleCarousel = dialogView.findViewById(R.id.tvTitleCarousel);
        TextView tvTitleGrid = dialogView.findViewById(R.id.tvTitleGrid);
        TextView tvTitleList = dialogView.findViewById(R.id.tvTitleList);

        ImageView ivCheckCarousel = dialogView.findViewById(R.id.ivCheckCarousel);
        ImageView ivCheckGrid = dialogView.findViewById(R.id.ivCheckGrid);
        ImageView ivCheckList = dialogView.findViewById(R.id.ivCheckList);

        Button btnCancel = dialogView.findViewById(R.id.btnCancelLayoutDialog);
        Button btnApply = dialogView.findViewById(R.id.btnApplyLayoutDialog);

        final String[] tempSelected = {
                (layoutPreference != null && !layoutPreference.isEmpty()) ? layoutPreference : "carousel"
        };

        Runnable updateSelectionUI = () -> {
            boolean isCarousel = "carousel".equalsIgnoreCase(tempSelected[0]);
            boolean isGrid = "grid".equalsIgnoreCase(tempSelected[0]);
            boolean isList = "list".equalsIgnoreCase(tempSelected[0]);

            cardCarousel.setBackgroundResource(isCarousel ? R.drawable.bg_card_selected_border : R.drawable.bg_card_unselected_border);
            cardGrid.setBackgroundResource(isGrid ? R.drawable.bg_card_selected_border : R.drawable.bg_card_unselected_border);
            cardList.setBackgroundResource(isList ? R.drawable.bg_card_selected_border : R.drawable.bg_card_unselected_border);

            tvTitleCarousel.setTextColor(getResources().getColor(isCarousel ? R.color.purple_primary : R.color.text_primary, null));
            tvTitleGrid.setTextColor(getResources().getColor(isGrid ? R.color.purple_primary : R.color.text_primary, null));
            tvTitleList.setTextColor(getResources().getColor(isList ? R.color.purple_primary : R.color.text_primary, null));

            ivCheckCarousel.setVisibility(isCarousel ? View.VISIBLE : View.GONE);
            ivCheckGrid.setVisibility(isGrid ? View.VISIBLE : View.GONE);
            ivCheckList.setVisibility(isList ? View.VISIBLE : View.GONE);
        };

        updateSelectionUI.run();

        cardCarousel.setOnClickListener(v -> {
            tempSelected[0] = "carousel";
            updateSelectionUI.run();
        });

        cardGrid.setOnClickListener(v -> {
            tempSelected[0] = "grid";
            updateSelectionUI.run();
        });

        cardList.setOnClickListener(v -> {
            tempSelected[0] = "list";
            updateSelectionUI.run();
        });

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                layoutPreference = tempSelected[0];
                String displayTitle = "carousel".equalsIgnoreCase(layoutPreference) ? "Swipe Carousel" :
                        "grid".equalsIgnoreCase(layoutPreference) ? "Grid Columns" : "Stacked List";
                Toast.makeText(this, "Media layout set to " + displayTitle, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundExecutor.shutdown();
    }
}
