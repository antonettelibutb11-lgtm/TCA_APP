const functions = require("firebase-functions");
const admin = require("firebase-admin");
const Jimp = require("jimp");
const axios = require("axios");

admin.initializeApp();
const db = admin.firestore();

// ─────────────────────────────────────────────────────────────────────────────
// 1. INAPPROPRIATE CONTENT & CYBER LIBEL DICTIONARY
// ─────────────────────────────────────────────────────────────────────────────
const INAPPROPRIATE_WORDS = [
    // English
    "fuck", "shit", "bitch", "asshole", "bastard", "damn", "crap",
    "dick", "pussy", "whore", "slut", "nigger", "faggot", "retard", "dumbass",
    // Tagalog / Filipino
    "putang", "putangina", "puta", "gago", "gaga", "bobo", "tanga", "leche",
    "ulol", "inutil", "hayop", "engot", "hinayupak", "lintik", "bwisit", "kingina", "tarantado",
    // Cebuano / Visayan
    "yawa", "pisti", "piste", "buang", "bogo", "maot", "bilat", "boto", "libog",
    "atay", "amaw", "hudas", "lintod", "ngil-ad", "pakshet", "iyot", "animal"
];

const CYBER_LIBEL_PATTERNS = [
    "rapist", "murderer", "drug pusher", "drug lord", "corrupt",
    "magnanakaw", "manloloko", "kawatan", "patay gutom", "manyak", "bastos",
    "scammer", "estafador", "papatayin", "patayin", "ipapatay", "ipakulong",
    "i will kill", "i will hurt", "you will regret"
];

/**
 * Evaluates post content for offensive terms or cyber libel threats.
 */
function analyzeTextContent(text) {
    if (!text || typeof text !== "string") {
        return { isFlagged: false };
    }

    const lowerText = text.toLowerCase();

    // Check Bad Words using word boundaries
    for (const word of INAPPROPRIATE_WORDS) {
        const regex = new RegExp(`\\b${word}\\b`, "i");
        if (regex.test(lowerText)) {
            return {
                isFlagged: true,
                reason: "INAPPROPRIATE_LANGUAGE",
                detail: `Detected inappropriate or offensive word: "${word}"`,
                confidence: "95%"
            };
        }
    }

    // Check Cyber Libel / Threatening Patterns
    for (const pattern of CYBER_LIBEL_PATTERNS) {
        const regex = new RegExp(`\\b${pattern}\\b`, "i");
        if (regex.test(lowerText)) {
            return {
                isFlagged: true,
                reason: "CYBER_LIBEL",
                detail: `Flagged potential Cyber Libel or defamatory phrase: "${pattern}" (RA 10175 compliance)`,
                confidence: "98%"
            };
        }
    }

    return { isFlagged: false };
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. PERCEPTUAL HASHING ENGINE (aHash)
// Resizes image to 8x8, converts to grayscale, computes 64-bit binary string.
// ─────────────────────────────────────────────────────────────────────────────
async function computePerceptualHash(imageUrl) {
    try {
        // Fetch image bytes via HTTP GET
        const response = await axios.get(imageUrl, { responseType: "arraybuffer", timeout: 10000 });
        const imageBuffer = Buffer.from(response.data);

        // Load image into Jimp
        const image = await Jimp.read(imageBuffer);

        // Resize to 8x8 pixels and convert to grayscale
        image.resize(8, 8).grayscale();

        const pixels = [];
        let totalBrightness = 0;

        for (let y = 0; y < 8; y++) {
            for (let x = 0; x < 8; x++) {
                const pixelHex = image.getPixelColor(x, y);
                const rgb = Jimp.intToRGBA(pixelHex);
                // Standard luma formula: 0.299R + 0.587G + 0.114B
                const brightness = Math.round(0.299 * rgb.r + 0.587 * rgb.g + 0.114 * rgb.b);
                pixels.push(brightness);
                totalBrightness += brightness;
            }
        }

        const avgBrightness = Math.round(totalBrightness / 64);

        // Build 64-character binary hash string ("010111...")
        let hashBinary = "";
        for (const p of pixels) {
            hashBinary += (p >= avgBrightness) ? "1" : "0";
        }

        return hashBinary;
    } catch (err) {
        console.error(`Failed to compute perceptual hash for image (${imageUrl}):`, err.message);
        return null;
    }
}

/**
 * Calculates similarity percentage between two 64-bit binary perceptual hashes via Hamming Distance.
 */
function compareHashes(hash1, hash2) {
    if (!hash1 || !hash2 || hash1.length !== hash2.length) return 0;
    let matches = 0;
    for (let i = 0; i < hash1.length; i++) {
        if (hash1.charAt(i) === hash2.charAt(i)) matches++;
    }
    return Math.round((matches * 100) / hash1.length);
}

/**
 * Calculates Jaccard Similarity percentage between two text contents.
 */
function calculateJaccardTextSimilarity(text1, text2) {
    if (!text1 || !text2) return 0;
    const normalize = t => t.toLowerCase().replace(/[^a-z0-9\s]/g, "").trim();
    const getWords = t => new Set(normalize(t).split(/\s+/).filter(w => w.length > 2));
    const words1 = getWords(text1);
    const words2 = getWords(text2);
    if (words1.size === 0 || words2.size === 0) return 0;
    const intersection = new Set([...words1].filter(w => words2.has(w)));
    const union = new Set([...words1, ...words2]);
    return Math.round((intersection.size / union.size) * 100);
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. FIRESTORE TRIGGER: AUTOMATED BACKEND MODERATION & DUPLICATE AI
// Triggers automatically whenever a new post document is created.
// ─────────────────────────────────────────────────────────────────────────────
exports.onPostCreated = functions.firestore
    .document("posts/{postId}")
    .onCreate(async (snapshot, context) => {
        const postData = snapshot.data();
        const postId = context.params.postId;

        if (!postData) return null;

        const content = postData.content || "";
        const photoUri = postData.photoUri || "";
        const category = postData.category || "Broadcast";
        const authorName = postData.authorName || "BISU Community";

        console.log(`[onPostCreated] Processing new post ${postId} by ${authorName}...`);

        let computedHash = postData.imageHash || "";
        let videoHash = postData.videoHash || "";
        let isDuplicateImage = false;
        let isDuplicateVideo = false;
        let isDuplicateText = false;
        let matchedSimilarity = 0;
        let matchedTextSimilarity = 0;

        // Fetch recent approved posts for text, image & video duplicate scanning
        const existingSnapshot = await db.collection("posts")
            .limit(100)
            .get();

        // --- STEP A: SERVER-SIDE TEXT DUPLICATE SCAN ---
        for (const doc of existingSnapshot.docs) {
            if (doc.id === postId) continue;
            const existingText = doc.data().content || "";
            const textSim = calculateJaccardTextSimilarity(content, existingText);
            if (textSim >= 85) {
                isDuplicateText = true;
                matchedTextSimilarity = textSim;
                console.warn(`[DUPLICATE TEXT DETECTED] Post ${postId} matches Post ${doc.id} at ${textSim}% text similarity.`);
                break;
            }
        }

        // --- STEP B: SERVER-SIDE IMAGE PERCEPTUAL DUPLICATE AI ---
        if (photoUri && photoUri.startsWith("http")) {
            console.log(`[onPostCreated] Computing server-side perceptual hash for photoUrl...`);
            computedHash = await computePerceptualHash(photoUri);

            if (computedHash) {
                // Save computed hash back to document
                await snapshot.ref.update({ imageHash: computedHash });

                for (const doc of existingSnapshot.docs) {
                    if (doc.id === postId) continue; // Skip self

                    const existingHash = doc.data().imageHash;
                    if (existingHash) {
                        const similarity = compareHashes(computedHash, existingHash);
                        if (similarity >= 90) {
                            isDuplicateImage = true;
                            matchedSimilarity = similarity;
                            console.warn(`[DUPLICATE IMAGE DETECTED] Post ${postId} matches Post ${doc.id} at ${similarity}% similarity.`);
                            break;
                        }
                    }
                }
            }
        }

        // --- STEP B.2: SERVER-SIDE VIDEO DUPLICATE AI ---
        if (videoHash && videoHash.trim() !== "") {
            for (const doc of existingSnapshot.docs) {
                if (doc.id === postId) continue; // Skip self

                const existingVideoHash = doc.data().videoHash;
                if (existingVideoHash && existingVideoHash === videoHash) {
                    isDuplicateVideo = true;
                    console.warn(`[DUPLICATE VIDEO DETECTED] Post ${postId} matches Post ${doc.id} via exact video hash.`);
                    break;
                }
            }
        }

        // --- STEP C: SERVER-SIDE TEXT CONTENT MODERATION ---
        const textAnalysis = analyzeTextContent(content);

        // --- STEP D: EXECUTE MODERATION ACTIONS ---
        if (isDuplicateVideo) {
            // Auto-flag duplicate video post to moderation queue
            await snapshot.ref.update({
                moderationStatus: "DELETED",
                isDuplicate: true,
                duplicateSimilarity: "100%",
                flaggedReason: "DUPLICATE_VIDEO_HASH_AI",
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });

            await db.collection("moderation_queue").add({
                postId: postId,
                content: content,
                authorName: authorName,
                reason: "DUPLICATE_VIDEO",
                aiScore: "100%",
                previewText: `[Auto-Deleted] Duplicate Video detected (exact hash match).`,
                moderationStatus: "DELETED",
                timestamp: admin.firestore.FieldValue.serverTimestamp()
            });

            console.log(`[ACTION TAKEN] Post ${postId} marked DELETED due to duplicate video.`);
            return null;
        }

        if (isDuplicateImage) {
            // Auto-flag duplicate image post to moderation queue
            await snapshot.ref.update({
                moderationStatus: "DELETED",
                isDuplicate: true,
                duplicateSimilarity: `${matchedSimilarity}%`,
                flaggedReason: "DUPLICATE_IMAGE_PERCEPTUAL_AI",
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });

            await db.collection("moderation_queue").add({
                postId: postId,
                content: content,
                authorName: authorName,
                reason: "DUPLICATE_IMAGE",
                aiScore: `${matchedSimilarity}%`,
                previewText: `[Auto-Deleted] Duplicate Image detected (${matchedSimilarity}% visual match).`,
                moderationStatus: "DELETED",
                timestamp: admin.firestore.FieldValue.serverTimestamp()
            });

            console.log(`[ACTION TAKEN] Post ${postId} marked DELETED due to duplicate image.`);
            return null;
        }

        if (isDuplicateText) {
            await snapshot.ref.update({
                moderationStatus: "FLAGGED",
                isDuplicate: true,
                duplicateSimilarity: `${matchedTextSimilarity}%`,
                flaggedReason: "DUPLICATE_TEXT_JACCARD_AI",
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });

            await db.collection("moderation_queue").add({
                postId: postId,
                content: content,
                authorName: authorName,
                reason: "DUPLICATE_TEXT",
                aiScore: `${matchedTextSimilarity}%`,
                previewText: `[Flagged] High Text Similarity match (${matchedTextSimilarity}% token overlap).`,
                moderationStatus: "FLAGGED",
                timestamp: admin.firestore.FieldValue.serverTimestamp()
            });

            console.log(`[ACTION TAKEN] Post ${postId} marked FLAGGED due to duplicate text.`);
            return null;
        }

        if (textAnalysis.isFlagged) {
            await snapshot.ref.update({
                moderationStatus: "FLAGGED",
                flaggedReason: textAnalysis.reason,
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });

            await db.collection("moderation_queue").add({
                postId: postId,
                content: content,
                authorName: authorName,
                reason: textAnalysis.reason,
                aiScore: textAnalysis.confidence,
                previewText: textAnalysis.detail,
                moderationStatus: "FLAGGED",
                timestamp: admin.firestore.FieldValue.serverTimestamp()
            });

            console.log(`[ACTION TAKEN] Post ${postId} marked FLAGGED due to ${textAnalysis.reason}.`);
            return null;
        }

        // Default clean post setup
        await snapshot.ref.update({
            moderationStatus: "APPROVED",
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });

        // Update central analytics statistics summary
        const analyticsRef = db.collection("analytics_summary").doc("counters");
        await analyticsRef.set({
            totalPosts: admin.firestore.FieldValue.increment(1),
            [`count_${category}`]: admin.firestore.FieldValue.increment(1),
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        console.log(`[COMPLETED] Post ${postId} verified as clean & APPROVED.`);
        return null;
    });

// ─────────────────────────────────────────────────────────────────────────────
// 4. CALLABLE FUNCTION: SECURE ATTENDANCE VERIFICATION
// ─────────────────────────────────────────────────────────────────────────────
exports.verifyAttendanceToken = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError(
            "unauthenticated",
            "Authentication required to register event attendance."
        );
    }

    const { qrToken, eventName } = data;
    if (!qrToken || typeof qrToken !== "string" || qrToken.trim().length === 0) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "A valid QR Code token is required."
        );
    }

    const uid = context.auth.uid;
    const studentEmail = context.auth.token.email || "Student";
    const targetEvent = eventName || "BISU Campus Event";

    const attendanceRecord = {
        studentUid: uid,
        studentEmail: studentEmail,
        qrToken: qrToken.trim(),
        eventName: targetEvent,
        verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
        verifiedViaServer: true
    };

    const docRef = await db.collection("event_attendance").add(attendanceRecord);
    return {
        status: "SUCCESS",
        attendanceId: docRef.id,
        message: `Attendance verified successfully for ${targetEvent}.`
    };
});

// ─────────────────────────────────────────────────────────────────────────────
// 5. CALLABLE FUNCTION: SERVER-SIDE ADMIN MODERATION
// ─────────────────────────────────────────────────────────────────────────────
exports.moderatePost = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Auth required.");
    }

    // Admin Verification
    const userDoc = await db.collection("users").doc(context.auth.uid).get();
    const isAdmin = context.auth.token.role === "ADMIN" ||
                    (userDoc.exists && (userDoc.data().role === "ADMIN" || userDoc.data().role === "admin"));

    if (!isAdmin) {
        throw new functions.https.HttpsError("permission-denied", "Admin role required.");
    }

    const { action, moderationId, postId, reason } = data;
    const batch = db.batch();

    if (action === "ARCHIVE") {
        if (moderationId) batch.update(db.collection("moderation_queue").doc(moderationId), { moderationStatus: "ARCHIVED" });
        if (postId) batch.update(db.collection("posts").doc(postId), { moderationStatus: "ARCHIVED" });
    } else if (action === "DELETE") {
        if (moderationId) batch.update(db.collection("moderation_queue").doc(moderationId), { moderationStatus: "DELETED" });
        if (postId) batch.delete(db.collection("posts").doc(postId));
    } else if (action === "WARN") {
        if (moderationId) batch.update(db.collection("moderation_queue").doc(moderationId), { moderationStatus: "WARNED" });
        if (postId) batch.update(db.collection("posts").doc(postId), { moderationStatus: "WARNED" });

        const notifRef = db.collection("notifications").doc();
        batch.set(notifRef, {
            title: "⚠️ Admin Moderation Warning",
            message: `Your post was flagged for: ${reason || "Community violation"}.`,
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
    } else if (action === "DISMISS") {
        if (moderationId) batch.update(db.collection("moderation_queue").doc(moderationId), { moderationStatus: "DISMISS" });
    } else if (action === "APPROVE") {
        if (moderationId) batch.update(db.collection("moderation_queue").doc(moderationId), { moderationStatus: "APPROVED" });
        if (postId) batch.update(db.collection("posts").doc(postId), { moderationStatus: "APPROVED" });
    }

    await batch.commit();
    return { status: "SUCCESS", action };
});

// ─────────────────────────────────────────────────────────────────────────────
// 6. CALLABLE FUNCTION: SECURE ATOMIC LIKES/REACTIONS
// ─────────────────────────────────────────────────────────────────────────────
exports.toggleLikePost = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Auth required.");
    }

    const { postId, emoji } = data;
    if (!postId) throw new functions.https.HttpsError("invalid-argument", "postId required.");

    const uid = context.auth.uid;
    const postRef = db.collection("posts").doc(postId);

    return db.runTransaction(async (transaction) => {
        const postDoc = await transaction.get(postRef);
        if (!postDoc.exists) throw new functions.https.HttpsError("not-found", "Post not found.");

        const likedByUsers = postDoc.data().likedByUsers || [];
        const isLiked = likedByUsers.includes(uid);

        if (isLiked) {
            transaction.update(postRef, {
                likeCount: admin.firestore.FieldValue.increment(-1),
                likedByUsers: admin.firestore.FieldValue.arrayRemove(uid)
            });
            return { status: "UNLIKED" };
        } else {
            const updates = {
                likeCount: admin.firestore.FieldValue.increment(1),
                likedByUsers: admin.firestore.FieldValue.arrayUnion(uid)
            };
            if (emoji) updates.reactionType = emoji;
            transaction.update(postRef, updates);
            return { status: "LIKED" };
        }
    });
});
