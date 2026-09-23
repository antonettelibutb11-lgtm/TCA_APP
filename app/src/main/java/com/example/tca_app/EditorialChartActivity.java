package com.example.tca_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditorialChartActivity extends AppCompatActivity {

    private static final int CAMERA_REQ_CODE = 201;

    private RecyclerView rvEditorialMembers;
    private EditorialMemberAdapter adapter;
    private final List<EditorialMember> allMembersList = new ArrayList<>();
    private final List<EditorialMember> displayedMembersList = new ArrayList<>();

    private ProgressBar pbMembersLoading;
    private TextView tvTotalMemberCount;
    private EditText etSearchMember;
    private ImageView ivClearMemberSearch;

    private TextView tabDeptAll, tabDeptEditorial, tabDeptWriting, tabDeptCreative, tabDeptBroadcasting;

    private String selectedDepartment = "All";
    private String currentSearchQuery = "";
    private boolean isUserAdmin = false;

    private View btnHeaderAddMember;
    private View fabAddMember;

    private boolean isPickingForNewMember = false;
    private Uri newMemberPhotoUri = null;
    private File newMemberPhotoFile = null;
    private ImageView newMemberPhotoPreview = null;
    private TextView newMemberInitialsPreview = null;

    private EditorialMember selectedMemberForPhoto = null;
    private File tempCameraFile;
    private Uri tempCameraUri;

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    if (isPickingForNewMember) {
                        newMemberPhotoUri = uri;
                        newMemberPhotoFile = null;
                        if (newMemberPhotoPreview != null) {
                            newMemberPhotoPreview.setVisibility(View.VISIBLE);
                            newMemberPhotoPreview.setImageURI(uri);
                        }
                        if (newMemberInitialsPreview != null) {
                            newMemberInitialsPreview.setVisibility(View.GONE);
                        }
                    } else if (selectedMemberForPhoto != null) {
                        uploadMemberPhoto(uri, null);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (Boolean.TRUE.equals(success) && tempCameraFile != null && tempCameraFile.exists()) {
                    if (isPickingForNewMember) {
                        newMemberPhotoUri = tempCameraUri;
                        newMemberPhotoFile = tempCameraFile;
                        if (newMemberPhotoPreview != null) {
                            newMemberPhotoPreview.setVisibility(View.VISIBLE);
                            newMemberPhotoPreview.setImageURI(tempCameraUri);
                        }
                        if (newMemberInitialsPreview != null) {
                            newMemberInitialsPreview.setVisibility(View.GONE);
                        }
                    } else if (selectedMemberForPhoto != null) {
                        uploadMemberPhoto(tempCameraUri, tempCameraFile);
                    }
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editorial_chart);

        ImageView btnBackChart = findViewById(R.id.btnBackChart);
        if (btnBackChart != null) {
            btnBackChart.setOnClickListener(v -> finish());
        }

        tvTotalMemberCount = findViewById(R.id.tvTotalMemberCount);
        pbMembersLoading = findViewById(R.id.pbMembersLoading);
        etSearchMember = findViewById(R.id.etSearchMember);
        ivClearMemberSearch = findViewById(R.id.ivClearMemberSearch);

        btnHeaderAddMember = findViewById(R.id.btnHeaderAddMember);
        fabAddMember = findViewById(R.id.fabAddMember);

        if (btnHeaderAddMember != null) {
            btnHeaderAddMember.setOnClickListener(v -> showAddMemberDialog());
        }
        if (fabAddMember != null) {
            fabAddMember.setOnClickListener(v -> showAddMemberDialog());
        }

        tabDeptAll = findViewById(R.id.tabDeptAll);
        tabDeptEditorial = findViewById(R.id.tabDeptEditorial);
        tabDeptWriting = findViewById(R.id.tabDeptWriting);
        tabDeptCreative = findViewById(R.id.tabDeptCreative);
        tabDeptBroadcasting = findViewById(R.id.tabDeptBroadcasting);

        rvEditorialMembers = findViewById(R.id.rvEditorialMembers);
        rvEditorialMembers.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EditorialMemberAdapter(this, displayedMembersList, this::showPhotoChangeDialog);
        rvEditorialMembers.setAdapter(adapter);

        // Security check: Only Admin can update member pictures or add members
        AuthUtils.checkCurrentUserAccess((isApprovedMember, isAdmin, role) -> {
            if (isFinishing() || isDestroyed()) return;
            isUserAdmin = isAdmin;
            if (adapter != null) {
                adapter.setAdmin(isAdmin);
            }
            if (btnHeaderAddMember != null) {
                btnHeaderAddMember.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            }
            if (fabAddMember != null) {
                fabAddMember.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            }
            if (isAdmin && getIntent().getBooleanExtra("OPEN_ADD_MEMBER_DIALOG", false)) {
                getIntent().removeExtra("OPEN_ADD_MEMBER_DIALOG");
                showAddMemberDialog();
            }
        });

        setupTabs();
        setupSearch();
        loadAllMembers();
    }

    private void setupTabs() {
        tabDeptAll.setOnClickListener(v -> selectDepartment("All"));
        tabDeptEditorial.setOnClickListener(v -> selectDepartment("Editorial Board"));
        tabDeptWriting.setOnClickListener(v -> selectDepartment("Writing Department"));
        tabDeptCreative.setOnClickListener(v -> selectDepartment("Creative Department"));
        tabDeptBroadcasting.setOnClickListener(v -> selectDepartment("Broadcasting Department"));
    }

    private void selectDepartment(String dept) {
        selectedDepartment = dept;
        updateTabStyles();
        filterMembers();
    }

    private void updateTabStyles() {
        TextView[] tabs = new TextView[]{tabDeptAll, tabDeptEditorial, tabDeptWriting, tabDeptCreative, tabDeptBroadcasting};
        String[] depts = new String[]{"All", "Editorial Board", "Writing Department", "Creative Department", "Broadcasting Department"};

        for (int i = 0; i < tabs.length; i++) {
            TextView tab = tabs[i];
            if (tab == null) continue;
            if (depts[i].equalsIgnoreCase(selectedDepartment)) {
                tab.setBackgroundResource(R.drawable.bg_chip_selected);
                tab.setTextColor(getResources().getColor(R.color.white, null));
            } else {
                tab.setBackgroundResource(R.drawable.bg_chip_unselected);
                tab.setTextColor(getResources().getColor(R.color.text_primary, null));
            }
        }
    }

    private void setupSearch() {
        if (etSearchMember != null) {
            etSearchMember.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s != null ? s.toString().trim().toLowerCase(Locale.getDefault()) : "";
                    if (ivClearMemberSearch != null) {
                        ivClearMemberSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    filterMembers();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (ivClearMemberSearch != null) {
            ivClearMemberSearch.setOnClickListener(v -> {
                if (etSearchMember != null) {
                    etSearchMember.setText("");
                }
            });
        }
    }

    private void loadAllMembers() {
        if (pbMembersLoading != null) pbMembersLoading.setVisibility(View.VISIBLE);

        EditorialMemberRepository.loadMembers(this, members -> {
            runOnUiThread(() -> {
                if (pbMembersLoading != null) pbMembersLoading.setVisibility(View.GONE);
                allMembersList.clear();
                allMembersList.addAll(members);
                if (tvTotalMemberCount != null) {
                    tvTotalMemberCount.setText("The Campus Access • " + allMembersList.size() + " Staff Members");
                }
                filterMembers();
            });
        });
    }

    private void filterMembers() {
        displayedMembersList.clear();

        for (EditorialMember m : allMembersList) {
            // Department filter
            if (!"All".equalsIgnoreCase(selectedDepartment) && !m.getDepartment().equalsIgnoreCase(selectedDepartment)) {
                continue;
            }

            // Search filter
            if (!currentSearchQuery.isEmpty()) {
                String name = m.getName().toLowerCase(Locale.getDefault());
                String role = m.getRole().toLowerCase(Locale.getDefault());
                String dept = m.getDepartment().toLowerCase(Locale.getDefault());

                if (!name.contains(currentSearchQuery) && !role.contains(currentSearchQuery) && !dept.contains(currentSearchQuery)) {
                    continue;
                }
            }

            displayedMembersList.add(m);
        }

        adapter.notifyDataSetChanged();
    }

    private void showPhotoChangeDialog(EditorialMember member) {
        if (!isUserAdmin) return;
        selectedMemberForPhoto = member;
        isPickingForNewMember = false;

        String[] options = new String[]{"Choose from Gallery", "Take a Photo with Camera"};
        new AlertDialog.Builder(this)
                .setTitle("Update Photo: " + member.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        galleryLauncher.launch("image/*");
                    } else {
                        launchCamera();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddMemberDialog() {
        if (!isUserAdmin) {
            Toast.makeText(this, "Only administrators can add members.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_editorial_member, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        newMemberPhotoUri = null;
        newMemberPhotoFile = null;

        View btnPickNewMemberPhoto = dialogView.findViewById(R.id.btnPickNewMemberPhoto);
        newMemberPhotoPreview = dialogView.findViewById(R.id.ivNewMemberPhotoPreview);
        newMemberInitialsPreview = dialogView.findViewById(R.id.tvNewMemberInitialsPreview);
        EditText etNewMemberName = dialogView.findViewById(R.id.etNewMemberName);
        android.widget.Spinner spinnerNewMemberDept = dialogView.findViewById(R.id.spinnerNewMemberDept);
        EditText etNewMemberRole = dialogView.findViewById(R.id.etNewMemberRole);
        ProgressBar pbAddMemberLoading = dialogView.findViewById(R.id.pbAddMemberLoading);
        TextView btnCancelAddMember = dialogView.findViewById(R.id.btnCancelAddMember);
        TextView btnSubmitAddMember = dialogView.findViewById(R.id.btnSubmitAddMember);

        // Spinner setup
        String[] departments = new String[]{
                "Editorial Board",
                "Writing Department",
                "Creative Department",
                "Broadcasting Department"
        };
        android.widget.ArrayAdapter<String> spinnerAdapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                departments
        );
        spinnerNewMemberDept.setAdapter(spinnerAdapter);

        // Pre-select current tab department if not "All"
        if (!"All".equalsIgnoreCase(selectedDepartment)) {
            for (int i = 0; i < departments.length; i++) {
                if (departments[i].equalsIgnoreCase(selectedDepartment)) {
                    spinnerNewMemberDept.setSelection(i);
                    break;
                }
            }
        }

        btnPickNewMemberPhoto.setOnClickListener(v -> {
            isPickingForNewMember = true;
            String[] options = new String[]{"Choose from Gallery", "Take a Photo with Camera"};
            new AlertDialog.Builder(this)
                    .setTitle("Select Member Photo")
                    .setItems(options, (d, which) -> {
                        if (which == 0) {
                            galleryLauncher.launch("image/*");
                        } else {
                            launchCamera();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnCancelAddMember.setOnClickListener(v -> dialog.dismiss());

        btnSubmitAddMember.setOnClickListener(v -> {
            String name = etNewMemberName.getText() != null ? etNewMemberName.getText().toString().trim() : "";
            String dept = spinnerNewMemberDept.getSelectedItem() != null ? spinnerNewMemberDept.getSelectedItem().toString() : "Editorial Board";
            String role = etNewMemberRole.getText() != null ? etNewMemberRole.getText().toString().trim() : "";

            if (name.isEmpty()) {
                etNewMemberName.setError("Member name is required");
                etNewMemberName.requestFocus();
                return;
            }

            if (role.isEmpty()) {
                etNewMemberRole.setError("Role/Position is required");
                etNewMemberRole.requestFocus();
                return;
            }

            pbAddMemberLoading.setVisibility(View.VISIBLE);
            btnSubmitAddMember.setEnabled(false);
            btnCancelAddMember.setEnabled(false);

            String newId = "member_" + System.currentTimeMillis();
            int newOrder = allMembersList.size() + 1;

            if (newMemberPhotoUri != null || (newMemberPhotoFile != null && newMemberPhotoFile.exists())) {
                backgroundExecutor.execute(() -> {
                    try {
                        InputStream stream = (newMemberPhotoFile != null && newMemberPhotoFile.exists())
                                ? new FileInputStream(newMemberPhotoFile)
                                : getContentResolver().openInputStream(newMemberPhotoUri);

                        if (stream == null) {
                            saveMemberAndFinish(new EditorialMember(newId, name, dept, role, "", newOrder), dialog, pbAddMemberLoading, btnSubmitAddMember, btnCancelAddMember);
                            return;
                        }

                        CloudinaryUploader.uploadImage(stream, new CloudinaryUploader.CloudinaryUploadCallback() {
                            @Override
                            public void onSuccess(String secureUrl) {
                                saveMemberAndFinish(new EditorialMember(newId, name, dept, role, secureUrl, newOrder), dialog, pbAddMemberLoading, btnSubmitAddMember, btnCancelAddMember);
                            }

                            @Override
                            public void onFailure(String error) {
                                runOnUiThread(() -> {
                                    Toast.makeText(EditorialChartActivity.this, "Photo upload failed: " + error + ". Saving member without photo...", Toast.LENGTH_SHORT).show();
                                    saveMemberAndFinish(new EditorialMember(newId, name, dept, role, "", newOrder), dialog, pbAddMemberLoading, btnSubmitAddMember, btnCancelAddMember);
                                });
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            saveMemberAndFinish(new EditorialMember(newId, name, dept, role, "", newOrder), dialog, pbAddMemberLoading, btnSubmitAddMember, btnCancelAddMember);
                        });
                    }
                });
            } else {
                saveMemberAndFinish(new EditorialMember(newId, name, dept, role, "", newOrder), dialog, pbAddMemberLoading, btnSubmitAddMember, btnCancelAddMember);
            }
        });

        dialog.show();
    }

    private void saveMemberAndFinish(EditorialMember member, AlertDialog dialog, ProgressBar pb, View btnSubmit, View btnCancel) {
        EditorialMemberRepository.addMember(this, member, new EditorialMemberRepository.MemberUpdateCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    allMembersList.add(member);
                    if (tvTotalMemberCount != null) {
                        tvTotalMemberCount.setText("The Campus Access • " + allMembersList.size() + " Staff Members");
                    }
                    filterMembers();
                    if (pb != null) pb.setVisibility(View.GONE);
                    if (dialog != null && dialog.isShowing()) dialog.dismiss();
                    Toast.makeText(EditorialChartActivity.this, "Member added successfully: " + member.getName(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    if (pb != null) pb.setVisibility(View.GONE);
                    if (btnSubmit != null) btnSubmit.setEnabled(true);
                    if (btnCancel != null) btnCancel.setEnabled(true);
                    Toast.makeText(EditorialChartActivity.this, "Failed to add member: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQ_CODE);
            return;
        }

        try {
            tempCameraFile = File.createTempFile("member_avatar_" + System.currentTimeMillis(), ".jpg",
                    getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES));
            tempCameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempCameraFile);
            cameraLauncher.launch(tempCameraUri);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission required.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadMemberPhoto(Uri uri, File file) {
        if (selectedMemberForPhoto == null) return;
        final EditorialMember target = selectedMemberForPhoto;

        Toast.makeText(this, "Uploading photo for " + target.getName() + "...", Toast.LENGTH_SHORT).show();

        backgroundExecutor.execute(() -> {
            try {
                InputStream stream = (file != null && file.exists())
                        ? new FileInputStream(file)
                        : getContentResolver().openInputStream(uri);

                if (stream == null) return;

                CloudinaryUploader.uploadImage(stream, new CloudinaryUploader.CloudinaryUploadCallback() {
                    @Override
                    public void onSuccess(String secureUrl) {
                        runOnUiThread(() -> {
                            EditorialMemberRepository.updateMemberPhoto(EditorialChartActivity.this, target.getId(), secureUrl, new EditorialMemberRepository.MemberUpdateCallback() {
                                @Override
                                public void onSuccess() {
                                    target.setPhotoUrl(secureUrl);
                                    adapter.notifyDataSetChanged();
                                    Toast.makeText(EditorialChartActivity.this, "Photo updated for " + target.getName() + "!", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(String error) {
                                    Toast.makeText(EditorialChartActivity.this, "Save error: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> Toast.makeText(EditorialChartActivity.this, "Upload failed: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(EditorialChartActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundExecutor.shutdown();
    }
}
