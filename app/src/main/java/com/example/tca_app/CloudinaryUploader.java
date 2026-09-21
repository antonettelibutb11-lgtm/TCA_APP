package com.example.tca_app;

import android.net.Uri;
import android.util.Log;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.firebase.firestore.FirebaseFirestore;

import android.os.Handler;
import android.os.Looper;

public class CloudinaryUploader {
    private static final java.util.concurrent.ExecutorService uploadExecutor = java.util.concurrent.Executors.newFixedThreadPool(3);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface CloudinaryUploadCallback {
        void onSuccess(String secureUrl);
        void onFailure(String error);
    }

    public static void uploadImage(InputStream inputStream, CloudinaryUploadCallback callback) {
        uploadMedia(inputStream, "upload.jpg", callback);
    }

    public static void uploadMedia(InputStream inputStream, String fileName, CloudinaryUploadCallback callback) {
        // Dynamically fetch config from Firestore
        FirebaseFirestore.getInstance().collection("system_config").document("cloudinary")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String cloudName = documentSnapshot.getString("cloudName");
                        String uploadPreset = documentSnapshot.getString("uploadPreset");
                        if (cloudName != null && uploadPreset != null) {
                            performUpload(inputStream, cloudName, uploadPreset, fileName, callback);
                        } else {
                            performUpload(inputStream, "k5hxc5ct", "lh0lmbrs", fileName, callback);
                        }
                    } else {
                        performUpload(inputStream, "k5hxc5ct", "lh0lmbrs", fileName, callback);
                    }
                })
                .addOnFailureListener(e -> performUpload(inputStream, "k5hxc5ct", "lh0lmbrs", fileName, callback));
    }

    private static void performUpload(InputStream inputStream, String cloudName, String uploadPreset, String fileName, CloudinaryUploadCallback callback) {
        uploadExecutor.execute(() -> {
            try {
                String cloudinaryUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/auto/upload";
                String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
                URL url = new URL(cloudinaryUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(("--" + boundary + "\r\n").getBytes());
                    outputStream.write(("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n").getBytes());
                    outputStream.write((uploadPreset + "\r\n").getBytes());

                    outputStream.write(("--" + boundary + "\r\n").getBytes());
                    outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes());
                    outputStream.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes());
                    
                    byte[] data = new byte[16384];
                    int nRead;
                    while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                        outputStream.write(data, 0, nRead);
                    }
                    
                    outputStream.write(("\r\n--" + boundary + "--\r\n").getBytes());
                    outputStream.flush();
                }
                inputStream.close();

                int responseCode = connection.getResponseCode();
                InputStream responseStream = (responseCode >= 200 && responseCode < 300) ? connection.getInputStream() : connection.getErrorStream();

                ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
                byte[] responseData = new byte[16384];
                int responseRead;
                while ((responseRead = responseStream.read(responseData, 0, responseData.length)) != -1) {
                    responseBuffer.write(responseData, 0, responseRead);
                }
                
                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject jsonResponse = new JSONObject(responseBuffer.toString());
                    final String secureUrl = jsonResponse.getString("secure_url");
                    mainHandler.post(() -> callback.onSuccess(secureUrl));
                } else {
                    final String errorMsg = "Upload failed: " + responseCode;
                    mainHandler.post(() -> callback.onFailure(errorMsg));
                }
            } catch (Exception e) {
                final String errorMsg = e.getMessage();
                mainHandler.post(() -> callback.onFailure(errorMsg));
            }
        });
    }
}
