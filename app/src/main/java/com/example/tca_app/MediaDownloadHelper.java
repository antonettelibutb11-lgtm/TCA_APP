package com.example.tca_app;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

public class MediaDownloadHelper {

    public static void downloadFile(Context context, String url, String defaultFileName, String mimeType) {
        if (context == null || url == null || url.trim().isEmpty()) {
            if (context != null) Toast.makeText(context, "Cannot download: Invalid media link.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri downloadUri = Uri.parse(url);

            String fileName = defaultFileName;
            if (fileName == null || fileName.isEmpty()) {
                String path = downloadUri.getLastPathSegment();
                if (path != null && !path.isEmpty()) {
                    fileName = path;
                } else {
                    fileName = "TCA_Media_" + System.currentTimeMillis();
                }
            }

            // Ensure valid extension
            if (!fileName.contains(".")) {
                if (mimeType != null && mimeType.contains("video")) {
                    fileName += ".mp4";
                } else {
                    fileName += ".jpg";
                }
            }

            DownloadManager.Request request = new DownloadManager.Request(downloadUri);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setTitle("Downloading " + fileName);
            request.setDescription("The Campus Access media file");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            if (mimeType != null && !mimeType.isEmpty()) {
                request.setMimeType(mimeType);
            }

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                Toast.makeText(context, "📥 Downloading " + fileName + "... Saved to Downloads.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Download service unavailable.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Download error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void downloadPostMedia(Context context, Post post) {
        if (context == null || post == null) return;

        List<String> mediaList = post.getMediaUris();
        if (mediaList == null || mediaList.isEmpty()) {
            mediaList = new java.util.ArrayList<>();
            if (post.getVideoUri() != null && !post.getVideoUri().isEmpty()) {
                mediaList.add(post.getVideoUri());
            } else if (post.getPhotoUri() != null && !post.getPhotoUri().isEmpty()) {
                mediaList.add(post.getPhotoUri());
            }
        }

        if (mediaList.isEmpty()) {
            copyPostText(context, post);
            return;
        }

        if (mediaList.size() == 1) {
            String url = mediaList.get(0);
            boolean isVideo = (post.getVideoUri() != null && !post.getVideoUri().isEmpty()) || url.contains(".mp4") || url.contains("video");
            String fileName = (isVideo ? "TCA_Video_" : "TCA_Photo_") + System.currentTimeMillis() + (isVideo ? ".mp4" : ".jpg");
            String mime = isVideo ? "video/mp4" : "image/jpeg";
            downloadFile(context, url, fileName, mime);
        } else {
            final List<String> finalMediaList = mediaList;
            String[] options = new String[finalMediaList.size() + 1];
            options[0] = "📥 Download All (" + finalMediaList.size() + " items)";
            for (int i = 0; i < finalMediaList.size(); i++) {
                String u = finalMediaList.get(i);
                boolean isVideo = u.contains(".mp4") || u.contains("video");
                options[i + 1] = (isVideo ? "🎥 Video " : "🖼️ Photo ") + (i + 1);
            }

            new AlertDialog.Builder(context)
                    .setTitle("Download Post Media")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            for (int i = 0; i < finalMediaList.size(); i++) {
                                String u = finalMediaList.get(i);
                                boolean isVideo = u.contains(".mp4") || u.contains("video");
                                String fileName = (isVideo ? "TCA_Video_" : "TCA_Photo_") + System.currentTimeMillis() + "_" + (i + 1) + (isVideo ? ".mp4" : ".jpg");
                                String mime = isVideo ? "video/mp4" : "image/jpeg";
                                downloadFile(context, u, fileName, mime);
                            }
                        } else {
                            int itemIndex = which - 1;
                            String u = finalMediaList.get(itemIndex);
                            boolean isVideo = u.contains(".mp4") || u.contains("video");
                            String fileName = (isVideo ? "TCA_Video_" : "TCA_Photo_") + System.currentTimeMillis() + "_" + (itemIndex + 1) + (isVideo ? ".mp4" : ".jpg");
                            String mime = isVideo ? "video/mp4" : "image/jpeg";
                            downloadFile(context, u, fileName, mime);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private static void copyPostText(Context context, Post post) {
        String text = post.getContent();
        if (text == null || text.trim().isEmpty()) {
            Toast.makeText(context, "No content or media to download.", Toast.LENGTH_SHORT).show();
            return;
        }

        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            android.content.ClipData clip = android.content.ClipData.newPlainText("Post Content", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "📋 Post text copied to clipboard!", Toast.LENGTH_SHORT).show();
        }
    }
}
