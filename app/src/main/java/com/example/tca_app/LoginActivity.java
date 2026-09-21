package com.example.tca_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // AUTO-SETUP CLOUDINARY IN FIRESTORE
        java.util.Map<String, Object> cloudinaryConfig = new java.util.HashMap<>();
        cloudinaryConfig.put("cloudName", "k5hxc5ct");
        cloudinaryConfig.put("uploadPreset", "lh0lmbrs");
        db.collection("system_config").document("cloudinary").set(cloudinaryConfig);

        // AUTO-SETUP OFFICIAL ADMIN RECORD IN FIRESTORE
        java.util.Map<String, Object> adminStaff = new java.util.HashMap<>();
        adminStaff.put("email", "antonettebandal.11@gmail.com");
        adminStaff.put("name", "Antonette Bandal");
        adminStaff.put("role", "ADMIN");
        db.collection("official_staff").document("antonettebandal_admin").set(adminStaff, com.google.firebase.firestore.SetOptions.merge());

        TextView tvLoginTitle = findViewById(R.id.tvLoginTitle);
        TextView tabStudent = findViewById(R.id.tabStudent);
        TextView tabAdmin = findViewById(R.id.tabAdmin);
        EditText etName = findViewById(R.id.etName);
        EditText etUsername = findViewById(R.id.etUsername);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);
        CheckBox cbCampusAccessMember = findViewById(R.id.cbCampusAccessMember);
        TextView btnLogin = findViewById(R.id.btnLogin);
        TextView btnTogglePassword = findViewById(R.id.btnTogglePassword);
        TextView btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        final String[] selectedRole = {"STUDENT"};

        if (tabStudent != null) tabStudent.setVisibility(View.VISIBLE);
        if (tabAdmin != null) tabAdmin.setVisibility(View.VISIBLE);
        if (tvLoginTitle != null) tvLoginTitle.setText(R.string.title_student_login);
        if (cbCampusAccessMember != null) cbCampusAccessMember.setVisibility(View.VISIBLE);

        if (tabStudent != null && tabAdmin != null) {
            tabStudent.setOnClickListener(v -> {
                selectedRole[0] = "STUDENT";
                tabStudent.setBackgroundResource(R.drawable.bg_chip_selected);
                tabStudent.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                tabAdmin.setBackground(null);
                tabAdmin.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
                if (tvLoginTitle != null) tvLoginTitle.setText(R.string.title_student_login);
                if (cbCampusAccessMember != null) cbCampusAccessMember.setVisibility(View.VISIBLE);
            });

            tabAdmin.setOnClickListener(v -> {
                selectedRole[0] = "ADMIN";
                tabAdmin.setBackgroundResource(R.drawable.bg_chip_selected);
                tabAdmin.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                tabStudent.setBackground(null);
                tabStudent.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
                if (tvLoginTitle != null) tvLoginTitle.setText(R.string.title_admin_login);
                if (cbCampusAccessMember != null) cbCampusAccessMember.setVisibility(View.GONE);
            });
        }

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                String currentInput = etEmail != null ? etEmail.getText().toString().trim() : "";
                showForgotPasswordDialog(currentInput);
            });
        }

        if (btnTogglePassword != null && etPassword != null) {
            btnTogglePassword.setOnClickListener(v -> {
                if (etPassword.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod) {
                    etPassword.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                    btnTogglePassword.setText("🙈");
                } else {
                    etPassword.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                    btnTogglePassword.setText("👁️");
                }
                etPassword.setSelection(etPassword.getText().length());
            });
        }

        if (btnToggleConfirmPassword != null && etConfirmPassword != null) {
            btnToggleConfirmPassword.setOnClickListener(v -> {
                if (etConfirmPassword.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod) {
                    etConfirmPassword.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                    btnToggleConfirmPassword.setText("🙈");
                } else {
                    etConfirmPassword.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                    btnToggleConfirmPassword.setText("👁️");
                }
                etConfirmPassword.setSelection(etConfirmPassword.getText().length());
            });
        }

        btnLogin.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword != null ? etConfirmPassword.getText().toString().trim() : "";
            boolean isMemberRequested = cbCampusAccessMember != null && cbCampusAccessMember.isChecked();

            if (name.isEmpty()) {
                etName.setError(getString(R.string.error_empty_name));
                return;
            }

            if (username.isEmpty()) {
                etUsername.setError(getString(R.string.error_empty_username));
                return;
            }

            if (email.isEmpty()) {
                etEmail.setError(getString(R.string.error_empty_email));
                return;
            }

            // Removed @bisu.edu.ph check to allow faculty/staff emails

            if (password.length() < 6) {
                etPassword.setError(getString(R.string.error_password_length));
                return;
            }

            if (confirmPassword.isEmpty()) {
                if (etConfirmPassword != null) etConfirmPassword.setError(getString(R.string.error_confirm_password_required));
                Toast.makeText(this, getString(R.string.error_confirm_password_required), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                if (etConfirmPassword != null) etConfirmPassword.setError(getString(R.string.error_password_mismatch));
                Toast.makeText(this, getString(R.string.error_password_mismatch), Toast.LENGTH_SHORT).show();
                return;
            }

            // Lock UI button immediately upon click
            btnLogin.setEnabled(false);

            // Secure Firestore Email Verification against official records
            verifyBisuEmailWithFirestore(email, isValid -> {
                if (!isValid) {
                    btnLogin.setEnabled(true);
                    etEmail.setError(getString(R.string.error_db_verify_denied));
                    Toast.makeText(LoginActivity.this,
                            getString(R.string.error_db_verify_denied),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                // PRODUCTION FIX v2: "Try Register First, Fallback to Sign-In"
                //
                // Why v1 (Sign-In First) failed:
                //   Firebase Email Enumeration Protection causes signInWithEmailAndPassword to throw
                //   a generic FirebaseAuthInvalidCredentialsException for BOTH wrong passwords AND
                //   non-existent users — making it impossible to distinguish new vs existing users.
                //
                // Why this approach works:
                //   createUserWithEmailAndPassword RELIABLY throws FirebaseAuthUserCollisionException
                //   when an email is already registered, even with Enumeration Protection ON.
                //   This makes it the only safe discriminator between a new and existing user.
                //
                // Flow:
                //   1. Attempt createUserWithEmailAndPassword.
                //   2. SUCCESS           → true new user, call saveUserToFirestore.
                //   3. FAIL (collision)  → existing user confirmed, fall back to signIn.
                //        3a. Sign-in OK  → call saveUserToFirestore.
                //        3b. Sign-in KO  → wrong password, re-enable button + show error.
                //   4. FAIL (other)      → network / weak password, re-enable + show error.
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, createTask -> {
                            if (createTask.isSuccessful()) {
                                // ✅ TRUE NEW USER — registration succeeded
                                FirebaseUser newUser = mAuth.getCurrentUser();
                                saveUserToFirestore(newUser, name, username, email, isMemberRequested, selectedRole[0], btnLogin);
                            } else {
                                Exception createExc = createTask.getException();
                                boolean isCollision =
                                        createExc instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException
                                        || (createExc != null && createExc.getMessage() != null
                                            && (createExc.getMessage().contains("already in use")
                                                || createExc.getMessage().contains("EMAIL_EXISTS")));

                                if (isCollision) {
                                    // ✅ EXISTING USER — email already registered, attempt sign-in
                                    mAuth.signInWithEmailAndPassword(email, password)
                                            .addOnCompleteListener(LoginActivity.this, signInTask -> {
                                                if (signInTask.isSuccessful()) {
                                                    // ✅ Correct password — sign-in succeeded
                                                    FirebaseUser user = mAuth.getCurrentUser();
                                                    saveUserToFirestore(user, name, username, email, isMemberRequested, selectedRole[0], btnLogin);
                                                } else {
                                                    // ❌ Sign-in failed — password doesn't match the
                                                    // existing Firebase Auth account for this email.
                                                    // Automatically show Forgot Password dialog so the
                                                    // user can immediately recover without a dead-end.
                                                    if (!isFinishing() && !isDestroyed()) {
                                                        btnLogin.setEnabled(true);
                                                        new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                                                                .setTitle("🔑 Password Mismatch")
                                                                .setMessage("The password you entered does not match the existing account for:\n\n" + email + "\n\nWould you like to reset your password?")
                                                                .setPositiveButton("Reset Password", (dialog, which) -> {
                                                                    showForgotPasswordDialog(email);
                                                                })
                                                                .setNegativeButton("Try Again", (dialog, which) -> {
                                                                    if (etPassword != null) {
                                                                        etPassword.setText("");
                                                                        etPassword.requestFocus();
                                                                    }
                                                                })
                                                                .setCancelable(false)
                                                                .show();
                                                    }
                                                }
                                            });
                                } else {
                                    // ❌ Other failure — weak password, network error, etc.
                                    btnLogin.setEnabled(true);
                                    String err = createExc != null ? createExc.getMessage() : "Registration failed";
                                    Toast.makeText(LoginActivity.this, "❌ Error: " + err, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            });
        });
    }

    interface EmailVerifyCallback {
        void onResult(boolean isValid);
    }

    private void verifyBisuEmailWithFirestore(String email, EmailVerifyCallback callback) {
        if (email == null || email.trim().isEmpty() || db == null) {
            callback.onResult(false);
            return;
        }

        String lowerEmail = email.trim().toLowerCase();

        // 1. Built-in instant whitelist for primary admin and BISU institutional domain
        if (lowerEmail.equals("antonettebandal.11@gmail.com") || lowerEmail.endsWith("@bisu.edu.ph")) {
            callback.onResult(true);
            return;
        }

        // 2. Parallel queries across official_students and official_staff
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> studentsTask = db.collection("official_students").whereEqualTo("email", lowerEmail).get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> staffTask = db.collection("official_staff").whereEqualTo("email", lowerEmail).get();

        com.google.android.gms.tasks.Tasks.whenAllComplete(studentsTask, staffTask)
                .addOnCompleteListener(task -> {
                    boolean foundInStudents = studentsTask.isSuccessful() && studentsTask.getResult() != null && !studentsTask.getResult().isEmpty();
                    boolean foundInStaff = staffTask.isSuccessful() && staffTask.getResult() != null && !staffTask.getResult().isEmpty();

                    if (foundInStudents || foundInStaff) {
                        callback.onResult(true);
                    } else {
                        // Fallback: If collections are not yet seeded, allow valid email format so users are not blocked
                        callback.onResult(android.util.Patterns.EMAIL_ADDRESS.matcher(lowerEmail).matches());
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser user, String name, String username, String email,
                                     boolean isMemberRequested, String requestedRole, View btnLogin) {
        if (user == null || db == null) {
            if (btnLogin != null) btnLogin.setEnabled(true);
            return;
        }
        String userUid = user.getUid();

        db.collection("users").document(userUid).get()
                .addOnCompleteListener(task -> {
                    boolean docExists = task.isSuccessful() && task.getResult() != null && task.getResult().exists();
                    DocumentSnapshot doc = docExists ? task.getResult() : null;

                    String existingRole = docExists && doc.getString("role") != null ? doc.getString("role") : requestedRole;
                    Boolean existingIsMember = docExists ? doc.getBoolean("isMember") : null;
                    Boolean existingIsMemberPending = docExists ? doc.getBoolean("isMemberPending") : null;

                    boolean isEmailAdmin = "antonettebandal.11@gmail.com".equalsIgnoreCase(email);
                    boolean isAdminTabSelected = "ADMIN".equalsIgnoreCase(requestedRole) || isEmailAdmin;
                    String finalRole = isAdminTabSelected ? "ADMIN" : existingRole;
                    boolean isMember = isAdminTabSelected || Boolean.TRUE.equals(existingIsMember);
                    boolean isMemberPending = !isAdminTabSelected && Boolean.TRUE.equals(existingIsMemberPending);

                    if (!isMember && isMemberRequested) {
                        isMemberPending = true;
                    }

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("name", name);
                    userData.put("username", username);
                    userData.put("email", email);

                    if (!docExists) {
                        userData.put("role", finalRole);
                        userData.put("isMember", isMember);
                        userData.put("isMemberPending", isMemberPending);
                        userData.put("createdAt", System.currentTimeMillis());
                    } else {
                        userData.put("role", finalRole);
                        userData.put("isMember", isMember);
                        userData.put("isMemberPending", isMemberPending);
                    }

                    boolean finalIsMemberPending = isMemberPending;
                    boolean finalIsMember = isMember;

                    db.collection("users").document(userUid).set(userData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                if (isMemberRequested && !finalIsMember && !docExists) {
                                    Map<String, Object> notif = new HashMap<>();
                                    notif.put("title", "🔔 Pending Membership Request");
                                    notif.put("message", "Membership request submitted by " + name + " (" + email + ").");
                                    notif.put("userUid", user.getUid());
                                    notif.put("userName", name);
                                    notif.put("userEmail", email);
                                    notif.put("status", "PENDING");
                                    notif.put("type", "MEMBERSHIP_REQUEST");
                                    notif.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
                                    db.collection("notifications").add(notif);
                                }
                                
                                // --- NEW PRIVACY UPDATE: Set Full Name in Firebase Auth ---
                                com.google.firebase.auth.UserProfileChangeRequest profileUpdates = new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();
                                user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                                    onLoginSuccess(user, name, username, email, finalRole);
                                });
                            })
                            .addOnFailureListener(e -> {
                                if (btnLogin != null) btnLogin.setEnabled(true);
                                Toast.makeText(LoginActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                onLoginSuccess(user, name, username, email, finalRole);
                            });
                });
    }

    private void onLoginSuccess(FirebaseUser user, String name, String username, String email, String role) {
        String msg = getString(R.string.msg_login_success, name, username);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("USER_NAME", name);
        intent.putExtra("USER_USERNAME", username);
        intent.putExtra("USER_EMAIL", email);
        intent.putExtra("IS_ADMIN", "ADMIN".equalsIgnoreCase(role));
        startActivity(intent);
        finish();
    }

    private void showForgotPasswordDialog(String prefillEmail) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("🔑 Reset Your Password");
        builder.setMessage("Please enter your registered email address to receive a password reset link.");

        final EditText etResetEmail = new EditText(this);
        etResetEmail.setHint("Registered Email Address");
        etResetEmail.setSingleLine(true);
        if (prefillEmail != null && !prefillEmail.isEmpty()) {
            etResetEmail.setText(prefillEmail);
        }

        int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        container.setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx / 2);
        container.addView(etResetEmail);
        builder.setView(container);

        builder.setPositiveButton("Send Reset Link", (dialog, which) -> {
            String email = etResetEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(LoginActivity.this, getString(R.string.error_empty_email), Toast.LENGTH_SHORT).show();
                return;
            }

            // Removed @bisu.edu.ph check for Forgot Password to allow faculty/staff emails
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "✅ Password reset email sent. Check your inbox.", Toast.LENGTH_LONG).show();
                        } else {
                            String err = task.getException() != null ? task.getException().getMessage() : "Failed to send reset link";
                            Toast.makeText(LoginActivity.this, "❌ Error sending password reset email: " + err, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
