package com.example.tca_app;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Centralized utility for checking user roles and membership status.
 */
public class AuthUtils {

    public interface AccessLevelCallback {
        /**
         * @param isApprovedMember true if isMember == true or role is ADMIN
         * @param isAdmin          true if role is ADMIN
         * @param role             "ADMIN" or "STUDENT"
         */
        void onResult(boolean isApprovedMember, boolean isAdmin, String role);
    }

    public interface UserDocCallback {
        void onResult(boolean exists, DocumentSnapshot document);
    }

    /**
     * Checks current user's role and membership status in Firestore.
     */
    public static void checkCurrentUserAccess(AccessLevelCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onResult(false, false, "GUEST");
            return;
        }

        checkUserAccess(user.getUid(), callback);
    }

    /**
     * Checks a specific user's role and membership status in Firestore.
     */
    public static void checkUserAccess(String uid, AccessLevelCallback callback) {
        if (uid == null || uid.trim().isEmpty()) {
            callback.onResult(false, false, "GUEST");
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentEmail = currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail().toLowerCase() : "";

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();
                        String role = doc.getString("role");
                        Boolean isMemberVal = doc.getBoolean("isMember");

                        boolean isAdmin = role != null && ("ADMIN".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role));
                        boolean isApprovedMember = Boolean.TRUE.equals(isMemberVal) || isAdmin;

                        callback.onResult(isApprovedMember, isAdmin, isAdmin ? "ADMIN" : (role != null ? role : "STUDENT"));
                    } else {
                        callback.onResult(false, false, "STUDENT");
                    }
                });
    }

    /**
     * Gets the full user document for the current user.
     */
    public static void getCurrentUserDoc(UserDocCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onResult(false, null);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        callback.onResult(task.getResult().exists(), task.getResult());
                    } else {
                        callback.onResult(false, null);
                    }
                });
    }
}
