package com.example.tca_app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CloudinaryUploader {
    private static final String TAG = "CloudinaryUploader";
    private static final java.util.concurrent.ExecutorService uploadExecutor = java.util.concurrent.Executors.newFixedThreadPool(3);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface CloudinaryUploadCallback {
        void onSuccess(String secureUrl);
        void onFailure(String error);
    }

    /**
     * Compresses an image InputStream to max 1280px dimension and JPEG 80% quality.
     * This reduces a typical 10MB camera photo down to ~250KB (97% storage savings),
     * preventing Cloudinary free tier quota exhaustion and speeding up uploads significantly.
     */
    public static InputStream compressImageStream(InputStream inputStream) {
        if (inputStream == null) return null;
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap != null) {
                int maxDim = 1280;
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();

                if (width > maxDim || height > maxDim) {
                    float ratio = Math.min((float) maxDim / width, (float) maxDim / height);
                    int newWidth = Math.round(ratio * width);
                    int newHeight = Math.round(ratio * height);
                    bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                return new ByteArrayInputStream(baos.toByteArray());
            }
        } catch (Throwable t) {
            Log.w(TAG, "Image compression skipped or failed: " + t.getMessage());
        }
        return inputStream;
    }

    public static void uploadImage(InputStream inputStream, CloudinaryUploadCallback callback) {
        uploadMedia(compressImageStream(inputStream), "upload.jpg", callback);
    }

    public static void uploadMedia(InputStream inputStream, String fileName, CloudinaryUploadCallback callback) {
        // Compress image files if not already compressed
        InputStream streamToUpload = inputStream;
        if (fileName != null && (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg") || fileName.toLowerCase().endsWith(".png"))) {
            streamToUpload = compressImageStream(inputStream);
        }

        final InputStream finalStream = streamToUpload;

        // Dynamically fetch config from Firestore (allows changing cloud name/preset without rebuilding app)
        FirebaseFirestore.getInstance().collection("system_config").document("cloudinary")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String cloudName = documentSnapshot.getString("cloudName");
                        String uploadPreset = documentSnapshot.getString("uploadPreset");
                        if (cloudName != null && !cloudName.isEmpty() && uploadPreset != null && !uploadPreset.isEmpty()) {
                            performUpload(finalStream, cloudName, uploadPreset, fileName, callback);
                            return;
                        }
                    }
                    performUpload(finalStream, "k5hxc5ct", "lh0lmbrs", fileName, callback);
                })
                .addOnFailureListener(e -> performUpload(finalStream, "k5hxc5ct", "lh0lmbrs", fileName, callback));
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
                if (responseStream != null) {
                    byte[] responseData = new byte[16384];
                    int responseRead;
                    while ((responseRead = responseStream.read(responseData, 0, responseData.length)) != -1) {
                        responseBuffer.write(responseData, 0, responseRead);
                    }
                    responseStream.close();
                }

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject jsonResponse = new JSONObject(responseBuffer.toString());
                    final String secureUrl = jsonResponse.getString("secure_url");
                    mainHandler.post(() -> callback.onSuccess(secureUrl));
                } else {
                    String errorMsg = "Upload failed: HTTP " + responseCode;
                    try {
                        JSONObject errJson = new JSONObject(responseBuffer.toString());
                        if (errJson.has("error")) {
                            JSONObject errObj = errJson.optJSONObject("error");
                            if (errObj != null && errObj.has("message")) {
                                errorMsg = errObj.getString("message");
                            }
                        }
                    } catch (Exception ignored) {}

                    final String finalError = errorMsg;
                    Log.e(TAG, "Cloudinary upload failed: " + finalError);
                    mainHandler.post(() -> callback.onFailure(finalError));
                }
            } catch (Exception e) {
                final String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown upload error";
                Log.e(TAG, "Cloudinary upload exception: " + errorMsg, e);
                mainHandler.post(() -> callback.onFailure(errorMsg));
            }
        });
    }
}
