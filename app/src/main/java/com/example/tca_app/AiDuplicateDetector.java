package com.example.tca_app;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

// Helper class for content moderation and duplicate detection
public class AiDuplicateDetector {

    // Result models

    public static class DuplicateResult {
        public boolean isDuplicate;
        public int matchPercentage;
        public String matchedPostTitle;

        public DuplicateResult(boolean isDuplicate, int matchPercentage, String matchedPostTitle) {
            this.isDuplicate = isDuplicate;
            this.matchPercentage = matchPercentage;
            this.matchedPostTitle = matchedPostTitle;
        }
    }

    public static class ModerationResult {
        public boolean isFlagged;
        public String reason;       // "DUPLICATE_TEXT", "DUPLICATE_IMAGE", "INAPPROPRIATE", "CYBER_LIBEL"
        public String detail;       // Human-readable explanation
        public int confidence;      // 0-100

        public ModerationResult(boolean isFlagged, String reason, String detail, int confidence) {
            this.isFlagged = isFlagged;
            this.reason = reason;
            this.detail = detail;
            this.confidence = confidence;
        }
    }

    // Inappropriate words and cyber libel phrases list

    private static final Set<String> dynamicInappropriateWords = Collections.synchronizedSet(new HashSet<>(Arrays.asList(
            "fuck", "shit", "bitch", "asshole", "bastard", "damn", "crap",
            "dick", "pussy", "ass", "whore", "slut", "nigger", "faggot",
            "retard", "idiot", "moron", "stupid", "dumbass", "motherfucker",
            "putang", "putangina", "puta", "gago", "gaga", "bobo",
            "tanga", "leche", "ulol", "inutil", "hayop", "engot",
            "hinayupak", "lintik", "bwisit", "bwiset", "kingina",
            "tarantado", "pakyu", "pakyo", "ungas", "mangmang",
            "yawa", "pisti", "piste", "buang", "bogo", "maot",
            "bilat", "boto", "libog", "atay", "iring", "amaw",
            "hudas", "lintod", "ngil-ad", "pakshet", "pakshit",
            "iyot", "animal", "uwaw"
    )));

    private static final List<String> dynamicCyberLibelPatterns = new CopyOnWriteArrayList<>(Arrays.asList(
            "rapist", "murderer", "drug pusher", "drug lord", "corrupt",
            "magnanakaw", "manloloko", "kawatan", "patay gutom",
            "manyak", "bastos", "mababa ang uri", "walang kwenta",
            "scammer", "estafador", "tigas ulo",
            "papatayin", "patayin", "ipapatay", "idamay",
            "ipakulong", "ipakulong kita", "ireklamo kita",
            "i will kill", "i will hurt", "you will regret"
    ));

    // Cached compiled patterns
    private static final List<Pattern> compiledInappropriatePatterns = new CopyOnWriteArrayList<>();
    private static final List<Pattern> compiledLibelPatterns = new CopyOnWriteArrayList<>();

    static {
        // Pre-compile the default patterns at class load time
        recompilePatterns();
    }

    /**
     * Pre-compiles all word and libel patterns into Pattern objects.
     * Must be called after dynamicInappropriateWords or dynamicCyberLibelPatterns are mutated.
     */
    private static void recompilePatterns() {
        List<Pattern> tempInappropriatePatterns = new java.util.ArrayList<>();
        synchronized (dynamicInappropriateWords) {
            for (String word : dynamicInappropriateWords) {
                tempInappropriatePatterns.add(
                        Pattern.compile("(?i)\\b" + Pattern.quote(word) + "\\b")
                );
            }
        }
        
        List<Pattern> tempLibelPatterns = new java.util.ArrayList<>();
        for (String pattern : dynamicCyberLibelPatterns) {
            tempLibelPatterns.add(
                    Pattern.compile("(?i)\\b" + Pattern.quote(pattern) + "\\b")
            );
        }
        
        compiledInappropriatePatterns.clear();
        compiledInappropriatePatterns.addAll(tempInappropriatePatterns);
        
        compiledLibelPatterns.clear();
        compiledLibelPatterns.addAll(tempLibelPatterns);
    }

    /**
     * Dynamically fetches moderation rules from Firestore:
     * collection: system_config / document: moderation_rules
     */
    public static void fetchRemoteModerationRules(FirebaseFirestore db, Runnable onComplete) {
        if (db == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        db.collection("system_config").document("moderation_rules")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();
                        Object rawBadWords = doc.get("inappropriateWords");
                        if (rawBadWords instanceof List<?>) {
                            dynamicInappropriateWords.clear();
                            for (Object item : (List<?>) rawBadWords) {
                                if (item != null) {
                                    String w = item.toString().trim().toLowerCase();
                                    if (!w.isEmpty()) {
                                        dynamicInappropriateWords.add(w);
                                    }
                                }
                            }
                        }

                        Object rawLibel = doc.get("cyberLibelPatterns");
                        if (rawLibel instanceof List<?>) {
                            dynamicCyberLibelPatterns.clear();
                            for (Object item : (List<?>) rawLibel) {
                                if (item != null) {
                                    String p = item.toString().trim().toLowerCase();
                                    if (!p.isEmpty()) {
                                        dynamicCyberLibelPatterns.add(p);
                                    }
                                }
                            }
                        }
                        // PERFORMANCE FIX: Re-compile all patterns once after remote rules are loaded.
                        // This ensures checkInappropriateContent() never calls Pattern.compile() in a loop.
                        recompilePatterns();
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }

    public static Set<String> getInappropriateWords() {
        return Collections.unmodifiableSet(dynamicInappropriateWords);
    }

    public static List<String> getCyberLibelPatterns() {
        return Collections.unmodifiableList(dynamicCyberLibelPatterns);
    }

    // Check duplicate text using Jaccard similarity

    public static DuplicateResult checkForDuplicates(String newContent, List<Post> existingPosts) {
        if (newContent == null || newContent.trim().isEmpty() || existingPosts == null || existingPosts.isEmpty()) {
            return new DuplicateResult(false, 0, null);
        }

        String normalizedNew = normalizeText(newContent);
        Set<String> newWords = extractWords(normalizedNew);

        for (Post post : existingPosts) {
            String existingText = normalizeText(post.getContent());
            Set<String> existingWords = extractWords(existingText);
            int similarity = calculateJaccardSimilarity(newWords, existingWords);

            if (similarity >= 60) {
                return new DuplicateResult(true, similarity, post.getContent());
            }
        }
        return new DuplicateResult(false, 0, null);
    }

    // Check for inappropriate words and cyber libel phrases
    public static ModerationResult checkInappropriateContent(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ModerationResult(false, null, "Content is clean.", 0);
        }

        // Check bad words
        for (Pattern pattern : compiledInappropriatePatterns) {
            java.util.regex.Matcher m = pattern.matcher(text);
            if (m.find()) {
                // Extract the matched word from the pattern for the error message
                String matchedWord = m.group();
                return new ModerationResult(
                        true,
                        "INAPPROPRIATE",
                        "⚠️ Flagged inappropriate or offensive language: \"" + matchedWord + "\"",
                        95
                );
            }
        }

        // Check for cyber libel patterns using pre-compiled patterns
        for (Pattern pattern : compiledLibelPatterns) {
            java.util.regex.Matcher m = pattern.matcher(text);
            if (m.find()) {
                String matchedPhrase = m.group();
                return new ModerationResult(
                        true,
                        "CYBER_LIBEL",
                        "🚨 Flagged potential Cyber Libel or threatening language: \"" + matchedPhrase + "\"\n\nThis may violate RA 10175 (Cybercrime Prevention Act).",
                        98
                );
            }
        }

        return new ModerationResult(false, null, "Content is clean.", 0);
    }

    // Image duplicate detection using average hash (aHash)
    public static String computeImageHash(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return "";

        Bitmap small = null;
        try {
            // Resize to 8x8 and convert to grayscale
            small = Bitmap.createScaledBitmap(bitmap, 8, 8, false);

            int[] grayPixels = new int[64];
            int total = 0;
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    int pixel = small.getPixel(x, y);
                    int gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3;
                    grayPixels[y * 8 + x] = gray;
                    total += gray;
                }
            }

            // Calculate average brightness and generate binary hash
            int avg = total / 64;
            StringBuilder hash = new StringBuilder();
            for (int p : grayPixels) {
                hash.append(p >= avg ? "1" : "0");
            }

            return hash.toString();
        } catch (Throwable e) {
            return "";
        } finally {
            if (small != null && !small.isRecycled() && small != bitmap) {
                small.recycle();
            }
        }
    }

    /**
     * Memory-safe image hash computation with dynamic downsampling for raw file paths.
     */
    public static String computeImageHash(String imagePath, int inSampleSize) {
        if (imagePath == null || imagePath.isEmpty()) return "";
        try {
            // Decode boundaries first to calculate optimal scaling without loading full image
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(imagePath, options);

            int targetSize = 16;
            int scale = 1;
            while (options.outWidth / scale / 2 >= targetSize &&
                   options.outHeight / scale / 2 >= targetSize) {
                scale *= 2;
            }

            // Now load the heavily downsampled bitmap
            options.inJustDecodeBounds = false;
            options.inSampleSize = Math.max(scale, inSampleSize);
            
            Bitmap bmp = android.graphics.BitmapFactory.decodeFile(imagePath, options);
            if (bmp == null) return "";
            
            String hash = computeImageHash(bmp);
            bmp.recycle();
            
            return hash;
        } catch (Throwable e) {
            return "";
        }
    }

    /**
     * Compare two image hashes using Hamming distance.
     * Returns similarity percentage (0–100).
     */
    public static int compareImageHashes(String hash1, String hash2) {
        if (hash1 == null || hash2 == null || hash1.length() != hash2.length()) return 0;

        int matches = 0;
        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) == hash2.charAt(i)) matches++;
        }
        return (matches * 100) / hash1.length();
    }

    /**
     * Checks if a new image hash duplicates any existing post's image hash.
     */
    public static ModerationResult checkImageDuplicate(String newImageHash, List<String> existingHashes) {
        if (newImageHash == null || newImageHash.isEmpty() || existingHashes == null) {
            return new ModerationResult(false, null, "Image is unique.", 0);
        }

        for (String existingHash : existingHashes) {
            int similarity = compareImageHashes(newImageHash, existingHash);
            if (similarity >= 90) {
                return new ModerationResult(
                        true,
                        "DUPLICATE_IMAGE",
                        "🖼️ Detected duplicate image!\n\nThis picture matches an existing post at " + similarity + "% similarity.",
                        similarity
                );
            }
        }
        return new ModerationResult(false, null, "Image is unique.", 0);
    }

    // Video duplicate detection using SHA-256 hash

    /**
     * Computes SHA-256 hash of an InputStream (e.g., video file).
     */
    public static String computeStreamHash(java.io.InputStream inputStream) {
        if (inputStream == null) return "";
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static ModerationResult checkVideoDuplicate(String newVideoHashOrUri, List<String> existingVideoHashesOrUris) {
        if (newVideoHashOrUri == null || newVideoHashOrUri.isEmpty() || existingVideoHashesOrUris == null) {
            return new ModerationResult(false, null, "Video is unique.", 0);
        }

        String normalizedNew = newVideoHashOrUri.toLowerCase().trim();
        for (String existing : existingVideoHashesOrUris) {
            if (existing != null && !existing.isEmpty()) {
                String normalizedExisting = existing.toLowerCase().trim();
                if (normalizedNew.equals(normalizedExisting)) {
                    return new ModerationResult(
                            true,
                            "DUPLICATE_VIDEO",
                            "🎥 Duplicate video byte stream matches a previously uploaded video.",
                            100
                    );
                }
            }
        }
        return new ModerationResult(false, null, "Video is unique.", 0);
    }

    // Helper string functions

    private static String normalizeText(String text) {
        if (text == null) return "";
        return text.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").trim();
    }

    private static Set<String> extractWords(String text) {
        Set<String> set = new HashSet<>();
        String[] tokens = text.split("\\s+");
        for (String token : tokens) {
            if (token.length() > 2) set.add(token);
        }
        return set;
    }

    private static int calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() || set2.isEmpty()) return 0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return 0;
        return (int) Math.round(((double) intersection.size() / union.size()) * 100.0);
    }
}
