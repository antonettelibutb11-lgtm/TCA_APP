package com.example.tca_app;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.widget.VideoView;
import android.net.Uri;
import android.widget.FrameLayout;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private void openGallery(android.content.Context ctx, java.util.List<String> uris, int startIndex) {
        android.content.Intent intent = new android.content.Intent(ctx, MediaViewerActivity.class);
        intent.putStringArrayListExtra("media_uris", new java.util.ArrayList<>(uris));
        intent.putExtra("start_index", startIndex);
        ctx.startActivity(intent);
    }

    private List<Post> postList;
    private boolean isAdmin = false;

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
    }

    public void setAdmin(boolean isAdmin) {
        if (this.isAdmin != isAdmin) {
            this.isAdmin = isAdmin;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.tvAuthorName.setText(post.getAuthorName());
        holder.tvPostMeta.setText(post.getPostMeta());

        // Handle post caption with "See more" / "See less" expansion
        String content = post.getContent();
        if (content == null || content.trim().isEmpty()) {
            holder.tvPostContent.setVisibility(View.GONE);
            if (holder.tvSeeMore != null) {
                holder.tvSeeMore.setVisibility(View.GONE);
            }
        } else {
            holder.tvPostContent.setVisibility(View.VISIBLE);
            holder.tvPostContent.setText(content);

            if (holder.tvSeeMore != null) {
                if (post.isExpanded()) {
                    holder.tvPostContent.setMaxLines(Integer.MAX_VALUE);
                    holder.tvPostContent.setEllipsize(null);
                    holder.tvSeeMore.setText("See less");
                    holder.tvSeeMore.setVisibility(View.VISIBLE);
                } else {
                    holder.tvPostContent.setMaxLines(3);
                    holder.tvPostContent.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    holder.tvSeeMore.setVisibility(View.GONE);

                    holder.tvPostContent.post(() -> {
                        int currentPos = holder.getAdapterPosition();
                        if (currentPos != RecyclerView.NO_POSITION && currentPos < postList.size() && postList.get(currentPos) == post) {
                            if (!post.isExpanded()) {
                                android.text.Layout layout = holder.tvPostContent.getLayout();
                                if (layout != null) {
                                    int lineCount = layout.getLineCount();
                                    boolean hasOverflow = false;
                                    if (lineCount > 0) {
                                        if (layout.getEllipsisCount(lineCount - 1) > 0) {
                                            hasOverflow = true;
                                        } else if (lineCount >= 3) {
                                            int endCharIndex = layout.getLineEnd(lineCount - 1);
                                            if (endCharIndex < content.length()) {
                                                hasOverflow = true;
                                            }
                                        }
                                    }
                                    if (hasOverflow) {
                                        holder.tvSeeMore.setVisibility(View.VISIBLE);
                                        holder.tvSeeMore.setText("See more");
                                    } else {
                                        holder.tvSeeMore.setVisibility(View.GONE);
                                    }
                                }
                            }
                        }
                    });
                }

                View.OnClickListener toggleExpandListener = v -> {
                    boolean expand = !post.isExpanded();
                    post.setExpanded(expand);
                    if (expand) {
                        holder.tvPostContent.setMaxLines(Integer.MAX_VALUE);
                        holder.tvPostContent.setEllipsize(null);
                        holder.tvSeeMore.setText("See less");
                        holder.tvSeeMore.setVisibility(View.VISIBLE);
                    } else {
                        holder.tvPostContent.setMaxLines(3);
                        holder.tvPostContent.setEllipsize(android.text.TextUtils.TruncateAt.END);
                        holder.tvSeeMore.setText("See more");
                        holder.tvSeeMore.setVisibility(View.VISIBLE);
                    }
                };

                holder.tvSeeMore.setOnClickListener(toggleExpandListener);

                holder.tvPostContent.setOnClickListener(v -> {
                    if (!post.isExpanded() && holder.tvSeeMore.getVisibility() == View.VISIBLE) {
                        toggleExpandListener.onClick(v);
                    }
                });
            }
        }

        holder.tvLikeCount.setText(formatMetricCount(post.getLikeCount()));
        if (holder.tvCommentLabel != null) {
            holder.tvCommentLabel.setText(formatMetricCount(post.getCommentCount()));
        }

        if (post.isPinned()) {
            holder.layoutBadge.setVisibility(View.VISIBLE);
            String badgeText = post.getBadgeText();
            if (badgeText != null) {
                badgeText = badgeText.replace("📌", "").replace("Pinned Advisory:", "").trim();
            }
            holder.tvBadgeText.setText(badgeText != null && !badgeText.isEmpty() ? badgeText : "Pinned Post");
        } else {
            holder.layoutBadge.setVisibility(View.GONE);
        }

        // Media Rendering (Carousel for 1-20 items)
        if (holder.layoutMediaContainer != null && holder.post_collage != null) {
            java.util.List<String> uris = post.getMediaUris();
            if (uris == null || uris.isEmpty()) {
                uris = new java.util.ArrayList<>();
                if (post.getVideoUri() != null && !post.getVideoUri().isEmpty()) uris.add(post.getVideoUri());
                else if (post.getPhotoUri() != null && !post.getPhotoUri().isEmpty()) uris.add(post.getPhotoUri());
            }

            boolean isSingleVideo = false;
            if (uris.size() == 1) {
                String firstUri = uris.get(0);
                if (firstUri != null && (firstUri.contains(".mp4") || firstUri.contains("video") || firstUri.contains("cloudinary"))) {
                    // Cloudinary auto/upload can be a video, but let's check getVideoUri explicitly
                    if ((post.getVideoUri() != null && !post.getVideoUri().isEmpty()) || firstUri.contains(".mp4")) {
                        isSingleVideo = true;
                    }
                }
            }

            if (isSingleVideo) {
                holder.layoutMediaContainer.setVisibility(View.GONE);
                holder.layoutSingleVideo.setVisibility(View.VISIBLE);
                holder.vvSingleVideo.setVideoURI(Uri.parse(uris.get(0)));
                holder.vvSingleVideo.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                });
                
                holder.vvSingleVideo.setOnErrorListener((mp, what, extra) -> {
                    android.util.Log.e("PostAdapter", "Video playback error: " + what + " " + extra);
                    return true;
                });
                
                // Allow tapping to open fullscreen
                final String videoUrlToPlay = uris.get(0);
                holder.layoutSingleVideo.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(v.getContext(), FullScreenVideoActivity.class);
                    intent.putExtra("videoUri", videoUrlToPlay);
                    v.getContext().startActivity(intent);
                });
            } else if (uris != null && !uris.isEmpty()) {
                holder.layoutSingleVideo.setVisibility(View.GONE);
                holder.layoutMediaContainer.setVisibility(View.VISIBLE);
                
                // Hide all containers initially
                holder.collage_1_img1.setVisibility(View.GONE);
                holder.collage_2_container.setVisibility(View.GONE);
                holder.collage_3_container.setVisibility(View.GONE);
                if (holder.collage_4_container != null) holder.collage_4_container.setVisibility(View.GONE);
                holder.collage_5_container.setVisibility(View.GONE);
                
                int count = uris.size();
                android.content.Context ctx = holder.itemView.getContext();
                
                final java.util.List<String> finalUris = uris;
                
                if (count == 1) {
                    holder.collage_1_img1.setVisibility(View.VISIBLE);
                    com.bumptech.glide.Glide.with(ctx).load(uris.get(0)).centerCrop().override(600).thumbnail(0.3f).into(holder.collage_1_img1);
                    holder.collage_1_img1.setOnClickListener(v -> openGallery(ctx, finalUris, 0));
                } else if (count == 2) {
                    holder.collage_2_container.setVisibility(View.VISIBLE);
                    com.bumptech.glide.Glide.with(ctx).load(uris.get(0)).centerCrop().override(600).thumbnail(0.3f).into(holder.collage_2_img1);
                    com.bumptech.glide.Glide.with(ctx).load(uris.get(1)).centerCrop().override(600).thumbnail(0.3f).into(holder.collage_2_img2);
                    holder.collage_2_img1.setOnClickListener(v -> openGallery(ctx, finalUris, 0));
                    holder.collage_2_img2.setOnClickListener(v -> openGallery(ctx, finalUris, 1));
                } else if (count == 3) {
                    holder.collage_3_container.setVisibility(View.VISIBLE);
                    com.bumptech.glide.Glide.with(ctx).load(uris.get(0)).centerCrop().override(600).thumbnail(0.3f).into(holder.collage_3_img1);
                    com.bumptech.glide.Glide.with(ctx).load(uris.get(1)).centerCrop().override(600).thumbnail(0.3f).into(holder.collage_3_img2);
                    com.bumptech.glide.Glide.with(ctx).load(uris.get(2)).centerCrop().override(600).thumbnail(0.3f).into(holder.collage_3_img3);
                    holder.collage_3_img1.setOnClickListener(v -> openGallery(ctx, finalUris, 0));
                    holder.collage_3_img2.setOnClickListener(v -> openGallery(ctx, finalUris, 1));
                    holder.collage_3_img3.setOnClickListener(v -> openGallery(ctx, finalUris, 2));
                } else if (count == 4 && holder.collage_4_container != null) {
                    holder.collage_4_container.setVisibility(View.VISIBLE);
                    com.bumptech.glide.Glide.with(ctx).load(uris.get(0)).centerCrop().override(600).thumbnail(0.3f).into(holder.collage_4_img1);
                    com.bumptech.glide.Glide.with(ctx).load(uris.get(1)).centerCrop().override(600).thumbnail(0.3f).into(holder.collage_4_img2);
                    com.bumptech.glide.Glide.with(ctx).load(uris.get(2)).centerCrop().override(600).thumbnail(0.3f).into(holder.collage_4_img3);
                    com.bumptech.glide.Glide.with(ctx).load(uris.get(3)).centerCrop().override(600).thumbnail(0.3f).into(holder.collage_4_img4);
                    holder.collage_4_img1.setOnClickListener(v -> openGallery(ctx, finalUris, 0));
                    holder.collage_4_img2.setOnClickListener(v -> openGallery(ctx, finalUris, 1));
                    holder.collage_4_img3.setOnClickListener(v -> openGallery(ctx, finalUris, 2));
                    holder.collage_4_img4.setOnClickListener(v -> openGallery(ctx, finalUris, 3));
                } else {
                    holder.collage_5_container.setVisibility(View.VISIBLE);
                    com.bumptech.glide.Glide.with(ctx).load(uris.get(0)).centerCrop().override(600).thumbnail(0.3f).into(holder.collage_5_img1);
                    com.bumptech.glide.Glide.with(ctx).load(uris.get(1)).centerCrop().override(600).thumbnail(0.3f).into(holder.collage_5_img2);
                    com.bumptech.glide.Glide.with(ctx).load(uris.get(2)).centerCrop().override(600).thumbnail(0.3f).into(holder.collage_5_img3);
                    com.bumptech.glide.Glide.with(ctx).load(uris.get(3)).centerCrop().override(600).thumbnail(0.3f).into(holder.collage_5_img4);
                    com.bumptech.glide.Glide.with(ctx).load(uris.get(4)).centerCrop().override(600).thumbnail(0.3f).into(holder.collage_5_img5);
                    
                    if (count > 5) {
                        holder.collage_5_overlay.setVisibility(View.VISIBLE);
                        holder.collage_5_more_text.setVisibility(View.VISIBLE);
                        holder.collage_5_more_text.setText("+" + (count - 5));
                    } else {
                        holder.collage_5_overlay.setVisibility(View.GONE);
                        holder.collage_5_more_text.setVisibility(View.GONE);
                    }
                    
                    holder.collage_5_img1.setOnClickListener(v -> openGallery(ctx, finalUris, 0));
                    holder.collage_5_img2.setOnClickListener(v -> openGallery(ctx, finalUris, 1));
                    holder.collage_5_img3.setOnClickListener(v -> openGallery(ctx, finalUris, 2));
                    holder.collage_5_img4.setOnClickListener(v -> openGallery(ctx, finalUris, 3));
                    holder.collage_5_img5.setOnClickListener(v -> openGallery(ctx, finalUris, 4));
                }
            } else {
                holder.layoutMediaContainer.setVisibility(View.GONE);
                holder.layoutSingleVideo.setVisibility(View.GONE);
            }
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final String uid = (currentUser != null && currentUser.getUid() != null) ? currentUser.getUid() : "guest_user";


        // Three dots menu for post options
        if (holder.btnMoreOptions != null) {
            holder.btnMoreOptions.setVisibility(View.VISIBLE);
            holder.btnMoreOptions.setOnClickListener(v -> showProfessionalPostOptions(v.getContext(), post, holder));
        }

        if (holder.ivLikeIcon != null) {
            if (post.isLikedByCurrentUser()) {
                holder.ivLikeIcon.setImageResource(R.drawable.ic_heart_filled);
                holder.ivLikeIcon.clearColorFilter();
            } else {
                holder.ivLikeIcon.setImageResource(R.drawable.ic_heart_outline);
                holder.ivLikeIcon.clearColorFilter();
            }
        }

        if (post.isLikedByCurrentUser() && holder.tvLikeCount != null) {
            holder.tvLikeCount.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.purple_primary, null));
        } else if (holder.tvLikeCount != null) {
            holder.tvLikeCount.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_secondary, null));
        }

        // Tap Like -> Anti-Race Condition Atomic Increment & Button Lock
        // SCALABILITY FIX: Likes are now stored in subcollection posts/{postId}/likes/{uid}
        // instead of an unbounded likedByUsers array inside the document. This avoids the
        // Firestore 1MB document limit that would be hit on viral posts with thousands of likes.
        holder.btnLike.setOnClickListener(v -> {
            if (post.getId() == null || post.getId().isEmpty()) return;

            holder.btnLike.setEnabled(false); // Lock button to prevent spam clicks
            boolean alreadyLiked = post.isLikedByCurrentUser();

            com.google.firebase.firestore.DocumentReference likeRef = FirebaseFirestore.getInstance()
                    .collection("posts").document(post.getId())
                    .collection("likes").document(uid);

            if (alreadyLiked) {
                // UNLIKE: Delete the user's like document from the subcollection
                likeRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            FirebaseFirestore.getInstance()
                                    .collection("posts").document(post.getId())
                                    .update("likeCount", FieldValue.increment(-1));
                            post.setLikedByCurrentUser(false);
                            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
                            holder.btnLike.setEnabled(true);
                            int latestPos = holder.getAdapterPosition();
                            if (latestPos != RecyclerView.NO_POSITION) {
                                notifyItemChanged(latestPos);
                            }
                            Toast.makeText(v.getContext(), "Unliked post", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            holder.btnLike.setEnabled(true);
                            Toast.makeText(v.getContext(), "Failed to update like: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                // LIKE: Write the user's like document to the subcollection
                Map<String, Object> likeData = new HashMap<>();
                likeData.put("uid", uid);
                likeData.put("timestamp", FieldValue.serverTimestamp());
                likeRef.set(likeData)
                        .addOnSuccessListener(aVoid -> {
                            FirebaseFirestore.getInstance()
                                    .collection("posts").document(post.getId())
                                    .update("likeCount", FieldValue.increment(1));
                            post.setLikedByCurrentUser(true);
                            post.setLikeCount(post.getLikeCount() + 1);
                            holder.btnLike.setEnabled(true);
                            int latestPos = holder.getAdapterPosition();
                            if (latestPos != RecyclerView.NO_POSITION) {
                                notifyItemChanged(latestPos);
                            }
                            ChatNotificationHelper.notifyPostLiked(v.getContext(), post, uid, getSafeDisplayName(FirebaseAuth.getInstance().getCurrentUser()), "👍");
                            Toast.makeText(v.getContext(), "Liked post.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            holder.btnLike.setEnabled(true);
                            Toast.makeText(v.getContext(), "Failed to update like: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        // Long Press Like -> Reaction Picker Menu
        holder.btnLike.setOnLongClickListener(v -> {
            showReactionPicker(v.getContext(), post, holder);
            return true;
        });

        // Tap Comment -> Open Comments Dialog
        if (holder.btnComment != null) {
            holder.btnComment.setOnClickListener(v -> showCommentsDialog(v.getContext(), post));
        }

        // Tap Repost -> Repost to Campus Profile Feed via Firestore
        if (holder.btnRepost != null) {
            holder.btnRepost.setOnClickListener(v -> {
                Context ctx = v.getContext();
                new AlertDialog.Builder(ctx)
                        .setTitle("Repost to Feed")
                        .setMessage("Share this post to your profile and campus feed?")
                        .setPositiveButton("Repost", (dialog, which) -> {
                            holder.btnRepost.setEnabled(false);
                            String currentUid = uid;
                            String currentUserName = (currentUser != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty())
                                    ? currentUser.getDisplayName() : "BISU Student";

                            Map<String, Object> repostMap = new HashMap<>();
                            repostMap.put("authorName", currentUserName);
                            repostMap.put("authorUid", currentUid);
                            repostMap.put("postMeta", "Just now • Reposted from " + post.getAuthorName());
                            repostMap.put("content", post.getContent() != null ? post.getContent() : "");
                            repostMap.put("badgeText", "Repost");
                            repostMap.put("category", post.getCategory() != null ? post.getCategory() : "General");
                            repostMap.put("isPinned", false);
                            repostMap.put("isAiPick", false);
                            repostMap.put("likeCount", 0);
                            repostMap.put("loveCount", 0);
                            repostMap.put("commentCount", 0);
                            repostMap.put("timestamp", System.currentTimeMillis());
                            repostMap.put("scheduledTimestamp", System.currentTimeMillis());
                            repostMap.put("moderationStatus", "APPROVED");

                            if (post.getPhotoUri() != null && !post.getPhotoUri().isEmpty()) repostMap.put("photoUri", post.getPhotoUri());
                            if (post.getVideoUri() != null && !post.getVideoUri().isEmpty()) repostMap.put("videoUri", post.getVideoUri());
                            if (post.getDocUri() != null && !post.getDocUri().isEmpty()) repostMap.put("docUri", post.getDocUri());
                            if (post.getMediaUris() != null && !post.getMediaUris().isEmpty()) repostMap.put("mediaUris", post.getMediaUris());

                            FirebaseFirestore.getInstance().collection("posts")
                                    .add(repostMap)
                                    .addOnSuccessListener(docRef -> {
                                        holder.btnRepost.setEnabled(true);
                                        Toast.makeText(ctx, "Reposted successfully to your feed.", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        holder.btnRepost.setEnabled(true);
                                        Toast.makeText(ctx, "Failed to repost: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // Tap Send / Share -> Direct Options for Messenger, Instagram, Telegram, TikTok, More
        if (holder.btnSendPost != null) {
            holder.btnSendPost.setOnClickListener(v -> showSendOptionsDialog(v.getContext(), post));
        }
    }

    private String formatMetricCount(int count) {
        if (count <= 0) return "0";
        if (count >= 1000000) {
            return String.format(java.util.Locale.US, "%.1fM", count / 1000000.0).replace(".0M", "M");
        } else if (count >= 10000) {
            return String.format(java.util.Locale.US, "%.1fK", count / 1000.0).replace(".0K", "K");
        } else if (count >= 1000) {
            return String.format(java.util.Locale.US, "%,d", count);
        }
        return String.valueOf(count);
    }

    private void showSendOptionsDialog(Context context, Post post) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_share_post_options, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View btnMessenger = dialogView.findViewById(R.id.btnShareMessenger);
        View btnInstagram = dialogView.findViewById(R.id.btnShareInstagram);
        View btnTelegram = dialogView.findViewById(R.id.btnShareTelegram);
        View btnTikTok = dialogView.findViewById(R.id.btnShareTikTok);
        View btnMore = dialogView.findViewById(R.id.btnShareMore);
        View btnCancel = dialogView.findViewById(R.id.btnCancelShare);

        if (btnMessenger != null) {
            btnMessenger.setOnClickListener(v -> {
                dialog.dismiss();
                shareToApp(context, post, "com.facebook.orca", "Messenger");
            });
        }

        if (btnInstagram != null) {
            btnInstagram.setOnClickListener(v -> {
                dialog.dismiss();
                shareToApp(context, post, "com.instagram.android", "Instagram");
            });
        }

        if (btnTelegram != null) {
            btnTelegram.setOnClickListener(v -> {
                dialog.dismiss();
                shareToApp(context, post, "org.telegram.messenger", "Telegram");
            });
        }

        if (btnTikTok != null) {
            btnTikTok.setOnClickListener(v -> {
                dialog.dismiss();
                shareToApp(context, post, "com.zhiliaoapp.musically", "TikTok");
            });
        }

        if (btnMore != null) {
            btnMore.setOnClickListener(v -> {
                dialog.dismiss();
                shareToApp(context, post, null, "More Apps");
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void shareToApp(Context context, Post post, String targetPackage, String appName) {
        try {
            String shareBody = "";
            if (post != null) {
                String author = (post.getAuthorName() != null && !post.getAuthorName().isEmpty()) ? post.getAuthorName() : "Campus Student";
                String content = (post.getContent() != null && !post.getContent().isEmpty()) ? post.getContent() : "";
                shareBody = "Shared from TCA Campus App:\n\n" + author + ": \"" + content + "\"";
                if (post.getPhotoUri() != null && !post.getPhotoUri().isEmpty()) {
                    shareBody += "\n" + post.getPhotoUri();
                } else if (post.getMediaUris() != null && !post.getMediaUris().isEmpty()) {
                    shareBody += "\n" + post.getMediaUris().get(0);
                }
            }

            android.content.Intent sendIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "TCA Campus Post");
            sendIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);

            if (targetPackage != null) {
                sendIntent.setPackage(targetPackage);
                try {
                    context.startActivity(sendIntent);
                    return;
                } catch (android.content.ActivityNotFoundException e) {
                    Toast.makeText(context, appName + " is not installed. Opening chooser...", Toast.LENGTH_SHORT).show();
                }
            }

            // Fallback / More apps
            android.content.Intent chooser = android.content.Intent.createChooser(sendIntent, "Send Post via");
            context.startActivity(chooser);
        } catch (Exception ex) {
            Toast.makeText(context, "Unable to share: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showReportPostDialog(Context ctx, Post post) {
        if (ctx == null || post == null || post.getId() == null || post.getId().isEmpty()) return;

        View dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_report_post, null);
        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        android.widget.RadioGroup rgReportReasons = dialogView.findViewById(R.id.rgReportReasons);
        android.widget.EditText etReportNotes = dialogView.findViewById(R.id.etReportNotes);
        android.widget.Button btnCancelReport = dialogView.findViewById(R.id.btnCancelReport);
        android.widget.Button btnSubmitReport = dialogView.findViewById(R.id.btnSubmitReport);

        btnCancelReport.setOnClickListener(view -> dialog.dismiss());

        btnSubmitReport.setOnClickListener(view -> {
            int selectedRadioId = rgReportReasons.getCheckedRadioButtonId();
            android.widget.RadioButton selectedRb = dialogView.findViewById(selectedRadioId);
            String chosenReason = selectedRb != null ? selectedRb.getText().toString() : "Other campus guideline violation";
            String optionalNotes = etReportNotes != null ? etReportNotes.getText().toString().trim() : "";

            dialog.dismiss();
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String currentUid = (currentUser != null && currentUser.getUid() != null) ? currentUser.getUid() : "guest_user";

            Map<String, Object> flagData = new HashMap<>();
            flagData.put("postId", post.getId());
            flagData.put("content", post.getContent() != null ? post.getContent() : "");
            flagData.put("authorName", post.getAuthorName() != null ? post.getAuthorName() : "");
            flagData.put("reportedByUid", currentUid);
            flagData.put("reason", chosenReason);
            if (!optionalNotes.isEmpty()) {
                flagData.put("notes", optionalNotes);
            }
            flagData.put("aiScore", "User Flag: " + chosenReason);
            flagData.put("moderationStatus", "PENDING");
            flagData.put("timestamp", System.currentTimeMillis());

            FirebaseFirestore.getInstance().collection("moderation_queue")
                    .add(flagData)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(ctx, "Thank you. Your report has been submitted for moderation review.", Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ctx, "Unable to submit report. Please check your connection and try again.", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    private void showReactionPicker(Context context, Post post, PostViewHolder holder) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reactions_horizontal, null);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setDimAmount(0.25f);
        }

        View container = dialogView.findViewById(R.id.reactionsContainer);
        if (container != null) {
            container.setScaleX(0.75f);
            container.setScaleY(0.75f);
            container.setAlpha(0f);
            container.animate()
                    .scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(220)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.3f))
                    .start();
        }

        TextView tvReactLike = dialogView.findViewById(R.id.tvReactLike);
        TextView tvReactHeart = dialogView.findViewById(R.id.tvReactHeart);
        TextView tvReactCare = dialogView.findViewById(R.id.tvReactCare);
        TextView tvReactHaha = dialogView.findViewById(R.id.tvReactHaha);
        TextView tvReactWow = dialogView.findViewById(R.id.tvReactWow);
        TextView tvReactSad = dialogView.findViewById(R.id.tvReactSad);
        TextView tvReactAngry = dialogView.findViewById(R.id.tvReactAngry);

        final java.util.List<android.animation.Animator> activeAnimators = new java.util.ArrayList<>();
        TextView[] emojis = {tvReactLike, tvReactHeart, tvReactCare, tvReactHaha, tvReactWow, tvReactSad, tvReactAngry};

        // Staggered pop-in + continuous alive bobbing & breathing pulse ("mag lihok2")
        for (int i = 0; i < emojis.length; i++) {
            final TextView emojiView = emojis[i];
            if (emojiView == null) continue;
            final int index = i;

            emojiView.setScaleX(0f);
            emojiView.setScaleY(0f);
            emojiView.setTranslationY(24f);

            emojiView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(260)
                    .setStartDelay(index * 35)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(2.2f))
                    .withEndAction(() -> {
                        // 1. Continuous Bobbing Float Animation (lihok-lihok)
                        android.animation.ObjectAnimator bob = android.animation.ObjectAnimator.ofFloat(emojiView, "translationY", 0f, -6f, 0f);
                        bob.setDuration(950 + (index % 3) * 150);
                        bob.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                        bob.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                        bob.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
                        bob.setStartDelay((index * 80) % 360);
                        bob.start();
                        activeAnimators.add(bob);

                        // 2. Continuous Subtle Breathing Pulse
                        android.animation.ObjectAnimator pulseX = android.animation.ObjectAnimator.ofFloat(emojiView, "scaleX", 1f, 1.07f, 1f);
                        pulseX.setDuration(950 + (index % 3) * 150);
                        pulseX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                        pulseX.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                        pulseX.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
                        pulseX.setStartDelay((index * 80) % 360);
                        pulseX.start();
                        activeAnimators.add(pulseX);

                        android.animation.ObjectAnimator pulseY = android.animation.ObjectAnimator.ofFloat(emojiView, "scaleY", 1f, 1.07f, 1f);
                        pulseY.setDuration(950 + (index % 3) * 150);
                        pulseY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                        pulseY.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                        pulseY.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
                        pulseY.setStartDelay((index * 80) % 360);
                        pulseY.start();
                        activeAnimators.add(pulseY);
                    })
                    .start();

            // Touch zoom effect
            emojiView.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(1.45f).scaleY(1.45f).translationY(-12f).setDuration(120).start();
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).translationY(0f).setDuration(120).start();
                }
                return false;
            });
        }

        dialog.setOnDismissListener(d -> {
            for (android.animation.Animator a : activeAnimators) {
                if (a != null) a.cancel();
            }
            activeAnimators.clear();
        });
        View.OnClickListener reactionListener = v -> {
            String emoji = "👍";
            if (v == tvReactHeart) emoji = "❤️";
            else if (v == tvReactCare) emoji = "🥰";
            else if (v == tvReactHaha) emoji = "😆";
            else if (v == tvReactWow) emoji = "😮";
            else if (v == tvReactSad) emoji = "😢";
            else if (v == tvReactAngry) emoji = "😡";

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String uid = (currentUser != null && currentUser.getUid() != null) ? currentUser.getUid() : "guest_user";

            if (post.isLikedByCurrentUser()) {
                Toast.makeText(context, "You have already reacted to this post.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }

            // Animate selection
            final String finalEmoji = emoji;
            v.animate().scaleX(1.5f).scaleY(1.5f).setDuration(150).withEndAction(() -> {
                dialog.dismiss();
                
                if (post.getId() != null && !post.getId().isEmpty()) {
                holder.btnLike.setEnabled(false);
                // SCALABILITY FIX: Write reaction to the likes subcollection
                Map<String, Object> reactionData = new HashMap<>();
                reactionData.put("uid", uid);
                reactionData.put("reaction", finalEmoji);
                reactionData.put("timestamp", FieldValue.serverTimestamp());

                FirebaseFirestore.getInstance()
                        .collection("posts").document(post.getId())
                        .collection("likes").document(uid)
                        .set(reactionData)
                        .addOnSuccessListener(aVoid -> {
                            FirebaseFirestore.getInstance()
                                    .collection("posts").document(post.getId())
                                    .update("likeCount", FieldValue.increment(1),
                                            "reactionType", finalEmoji);
                            post.setLikedByCurrentUser(true);
                            post.setLikeCount(post.getLikeCount() + 1);
                            holder.btnLike.setEnabled(true);
                            int latestPos = holder.getAdapterPosition();
                            if (latestPos != RecyclerView.NO_POSITION) {
                                notifyItemChanged(latestPos);
                            }
                            ChatNotificationHelper.notifyPostLiked(context, post, uid, getSafeDisplayName(currentUser), finalEmoji);
                            Toast.makeText(context, "Reacted " + finalEmoji + " to post!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            holder.btnLike.setEnabled(true);
                            Toast.makeText(context, "Failed to react: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
            });
        };

        if (tvReactLike != null) tvReactLike.setOnClickListener(reactionListener);
        if (tvReactHeart != null) tvReactHeart.setOnClickListener(reactionListener);
        if (tvReactCare != null) tvReactCare.setOnClickListener(reactionListener);
        if (tvReactHaha != null) tvReactHaha.setOnClickListener(reactionListener);
        if (tvReactWow != null) tvReactWow.setOnClickListener(reactionListener);
        if (tvReactSad != null) tvReactSad.setOnClickListener(reactionListener);
        if (tvReactAngry != null) tvReactAngry.setOnClickListener(reactionListener);

        dialog.show();
    }

    private void showCommentsDialog(Context context, Post post) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_comments, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        RecyclerView rvComments = dialogView.findViewById(R.id.rvComments);
        EditText etCommentText = dialogView.findViewById(R.id.etCommentText);
        TextView btnSendComment = dialogView.findViewById(R.id.btnSendComment);

        List<Comment> commentList = new java.util.ArrayList<>();
        final String[] replyingToCommentId = {null};

        CommentAdapter commentAdapter = new CommentAdapter(commentList, post.getId(), parentComment -> {
            replyingToCommentId[0] = parentComment.getId();
            
            // --- NEW UX UPDATE: Auto-Mention the Author ---
            String mention = "@" + parentComment.getAuthor() + " ";
            etCommentText.setText(mention);
            etCommentText.setSelection(etCommentText.getText().length());
            etCommentText.requestFocus();
        });

        etCommentText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0 && replyingToCommentId[0] != null) {
                    replyingToCommentId[0] = null;
                    etCommentText.setHint("Write a comment...");
                }
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        if (rvComments != null) {
            rvComments.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(context));
            rvComments.setAdapter(commentAdapter);
        }

        // Fetch live comments from Firestore subcollection: posts/{postId}/comments
        if (post.getId() != null && !post.getId().isEmpty()) {
            com.google.firebase.firestore.ListenerRegistration registration =
                FirebaseFirestore.getInstance()
                    .collection("posts").document(post.getId())
                    .collection("comments")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                    .addSnapshotListener((querySnapshot, error) -> {
                        if (querySnapshot != null) {
                            commentList.clear();
                            for (com.google.firebase.firestore.DocumentSnapshot cDoc : querySnapshot.getDocuments()) {
                                String author = cDoc.getString("author");
                                String text = cDoc.getString("text");
                                Long ts = cDoc.getLong("timestamp");

                                Comment c = new Comment(
                                        cDoc.getId(),
                                        author != null ? author : "BISU Student",
                                        text != null ? text : "",
                                        ts != null ? ts : System.currentTimeMillis()
                                );
                                String rxType = cDoc.getString("reactionType");
                                Long rxCount = cDoc.getLong("reactionCount");
                                c.setReactionType(rxType != null ? rxType : "");
                                c.setReactionCount(rxCount != null ? rxCount.intValue() : 0);

                                Map<String, Object> summaryRaw = (Map<String, Object>) cDoc.get("reactionsSummary");
                                if (summaryRaw != null) {
                                    Map<String, Long> summary = new HashMap<>();
                                    for (Map.Entry<String, Object> e : summaryRaw.entrySet()) {
                                        if (e.getValue() instanceof Number) {
                                            summary.put(e.getKey(), ((Number) e.getValue()).longValue());
                                        }
                                    }
                                    c.setReactionsSummary(summary);
                                }

                                Map<String, Object> userRxsRaw = (Map<String, Object>) cDoc.get("userReactions");
                                if (userRxsRaw != null) {
                                    Map<String, String> userRxs = new HashMap<>();
                                    for (Map.Entry<String, Object> e : userRxsRaw.entrySet()) {
                                        if (e.getValue() != null) {
                                            userRxs.put(e.getKey(), e.getValue().toString());
                                        }
                                    }
                                    c.setUserReactions(userRxs);
                                }

                                Map<String, Object> userRxNamesRaw = (Map<String, Object>) cDoc.get("userReactionNames");
                                if (userRxNamesRaw != null) {
                                    Map<String, String> userRxNames = new HashMap<>();
                                    for (Map.Entry<String, Object> e : userRxNamesRaw.entrySet()) {
                                        if (e.getValue() != null) {
                                            userRxNames.put(e.getKey(), e.getValue().toString());
                                        }
                                    }
                                    c.setUserReactionNames(userRxNames);
                                }

                                commentList.add(c);
                            }
                            commentAdapter.notifyDataSetChanged();
                            if (rvComments != null && commentList.size() > 0) {
                                rvComments.smoothScrollToPosition(commentList.size() - 1);
                            }
                        }
                    });

            dialog.setOnDismissListener(d -> {
                if (registration != null) {
                    registration.remove();
                }
                if (commentAdapter != null) {
                    commentAdapter.cleanup();
                }
            });
        }

        if (btnSendComment != null) {
            btnSendComment.setOnClickListener(v -> {
                String commentStr = etCommentText.getText().toString().trim();
                if (commentStr.isEmpty()) return;

                btnSendComment.setEnabled(false); // Lock comment button against spam

                FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                String authorName = getSafeDisplayName(u);

                if (post.getId() != null && !post.getId().isEmpty()) {
                    Map<String, Object> commentMap = new HashMap<>();
                    commentMap.put("author", authorName);
                    commentMap.put("text", commentStr);
                    commentMap.put("timestamp", System.currentTimeMillis());

                    if (replyingToCommentId[0] != null) {
                        FirebaseFirestore.getInstance()
                                .collection("posts").document(post.getId())
                                .collection("comments").document(replyingToCommentId[0])
                                .collection("replies").add(commentMap)
                                .addOnSuccessListener(docRef -> {
                                    btnSendComment.setEnabled(true);
                                    etCommentText.setText("");
                                    replyingToCommentId[0] = null;
                                    etCommentText.setHint("Write a comment...");
                                    Toast.makeText(context, "Reply posted.", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    btnSendComment.setEnabled(true);
                                    Toast.makeText(context, "Failed to post reply: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        FirebaseFirestore.getInstance()
                                .collection("posts").document(post.getId())
                                .collection("comments").add(commentMap)
                                .addOnSuccessListener(docRef -> {
                                    FirebaseFirestore.getInstance()
                                            .collection("posts").document(post.getId())
                                            .update("commentCount", FieldValue.increment(1))
                                            .addOnCompleteListener(t -> {
                                                post.setCommentCount(post.getCommentCount() + 1);
                                                notifyDataSetChanged();
                                                btnSendComment.setEnabled(true);
                                                etCommentText.setText("");
                                                ChatNotificationHelper.notifyCommentAdded(context, post, u != null ? u.getUid() : "", authorName, commentStr);
                                                Toast.makeText(context, "Comment posted.", Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    btnSendComment.setEnabled(true);
                                    Toast.makeText(context, "Failed to post comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                } else {
                    btnSendComment.setEnabled(true);
                }
            });
        }

        dialog.show();
    }

    public static String getSafeDisplayName(FirebaseUser user) {
        // --- NEW PRIVACY UPDATE: Strictly use Full Name, never expose email ---
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName();
        }
        return "BISU Student";
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull PostViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder.layoutSingleVideo != null && holder.layoutSingleVideo.getVisibility() == View.VISIBLE) {
            if (holder.vvSingleVideo != null) {
                holder.vvSingleVideo.start();
            }
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull PostViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder.layoutSingleVideo != null && holder.layoutSingleVideo.getVisibility() == View.VISIBLE) {
            if (holder.vvSingleVideo != null) {
                holder.vvSingleVideo.pause();
            }
        }
    }


    private void showProfessionalPostOptions(Context context, Post post, PostViewHolder holder) {
        if (context == null || post == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.layout_bottom_sheet_post_options, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView tvSheetPostCategory = sheetView.findViewById(R.id.tvSheetPostCategory);
        TextView tvSheetPostPreview = sheetView.findViewById(R.id.tvSheetPostPreview);

        if (tvSheetPostCategory != null) {
            String cat = post.getCategory();
            tvSheetPostCategory.setText(cat != null && !cat.isEmpty() ? cat : "Post");
        }

        if (tvSheetPostPreview != null) {
            String content = post.getContent();
            tvSheetPostPreview.setText(content != null && !content.trim().isEmpty() ? content.trim() : "No text caption");
        }

        View layoutOptionDownload = sheetView.findViewById(R.id.layoutOptionDownload);
        View layoutOptionCopy = sheetView.findViewById(R.id.layoutOptionCopy);
        View layoutAdminSection = sheetView.findViewById(R.id.layoutAdminSection);
        View layoutOptionEdit = sheetView.findViewById(R.id.layoutOptionEdit);
        View layoutOptionPin = sheetView.findViewById(R.id.layoutOptionPin);
        TextView tvOptionPinTitle = sheetView.findViewById(R.id.tvOptionPinTitle);
        TextView tvOptionPinSubtitle = sheetView.findViewById(R.id.tvOptionPinSubtitle);
        ImageView ivOptionPinIcon = sheetView.findViewById(R.id.ivOptionPinIcon);
        View layoutOptionArchive = sheetView.findViewById(R.id.layoutOptionArchive);
        View layoutOptionDelete = sheetView.findViewById(R.id.layoutOptionDelete);

        if (layoutAdminSection != null) {
            layoutAdminSection.setVisibility(this.isAdmin ? View.VISIBLE : View.GONE);
        }

        if (tvOptionPinTitle != null) {
            if (post.isPinned()) {
                tvOptionPinTitle.setText("Unpin Post");
                if (tvOptionPinSubtitle != null) tvOptionPinSubtitle.setText("Remove pinned badge and unpin from top");
            } else {
                tvOptionPinTitle.setText("Pin Post");
                if (tvOptionPinSubtitle != null) tvOptionPinSubtitle.setText("Keep post highlighted at the top of the feed");
            }
        }

        // 1. Download Media
        if (layoutOptionDownload != null) {
            layoutOptionDownload.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                MediaDownloadHelper.downloadPostMedia(context, post);
            });
        }

        // 2. Copy Text
        if (layoutOptionCopy != null) {
            layoutOptionCopy.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                String textContent = post.getContent();
                if (textContent != null && !textContent.isEmpty()) {
                    android.content.ClipboardManager cb = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cb != null) {
                        cb.setPrimaryClip(android.content.ClipData.newPlainText("Post Content", textContent));
                        Toast.makeText(context, "Post text copied to clipboard.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // 3. Report Post (Available to all users)
        View layoutOptionReport = sheetView.findViewById(R.id.layoutOptionReport);
        if (layoutOptionReport != null) {
            layoutOptionReport.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                showReportPostDialog(context, post);
            });
        }

        // 3. Edit (Admin)
        if (layoutOptionEdit != null) {
            layoutOptionEdit.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                showEditPostDialog(context, post, holder);
            });
        }

        // 4. Pin / Unpin (Admin)
        if (layoutOptionPin != null) {
            layoutOptionPin.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                if (post.getId() == null || post.getId().isEmpty()) return;
                boolean newPinnedStatus = !post.isPinned();
                FirebaseFirestore.getInstance().collection("posts").document(post.getId())
                        .update("isPinned", newPinnedStatus)
                        .addOnSuccessListener(aVoid -> {
                            post.setPinned(newPinnedStatus);
                            int currentPos = holder.getAdapterPosition();
                            if (currentPos != RecyclerView.NO_POSITION) {
                                notifyItemChanged(currentPos);
                            }
                            Toast.makeText(context, newPinnedStatus ? "Post pinned to top." : "Post unpinned.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Error updating pin: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        }

        // 5. Archive (Admin)
        if (layoutOptionArchive != null) {
            layoutOptionArchive.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                showConfirmActionDialog(context, "Archive Post", "Hide from student feed?",
                        "This post will be hidden from the campus feed. It will not be deleted, but students will no longer see it.",
                        "Confirm Archive", R.drawable.ic_archive, "#475569", R.drawable.bg_icon_circle_slate, () -> {
                            if (post.getId() == null || post.getId().isEmpty()) return;
                            FirebaseFirestore.getInstance().collection("posts").document(post.getId())
                                    .update("moderationStatus", "ARCHIVED")
                                    .addOnSuccessListener(aVoid -> {
                                        int currentPos = holder.getAdapterPosition();
                                        if (currentPos != RecyclerView.NO_POSITION && currentPos < postList.size()) {
                                            postList.remove(currentPos);
                                            notifyItemRemoved(currentPos);
                                        }
                                        Toast.makeText(context, "Post archived successfully.", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to archive: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        });
            });
        }

        // 6. Delete (Admin)
        if (layoutOptionDelete != null) {
            layoutOptionDelete.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                showConfirmActionDialog(context, "Delete Post", "Permanently remove from campus hub?",
                        "Are you sure you want to permanently delete this post? This action is irreversible.",
                        "Confirm Delete", R.drawable.ic_delete, "#D32F2F", R.drawable.bg_icon_circle_red, () -> {
                            if (post.getId() == null || post.getId().isEmpty()) return;
                            FirebaseFirestore.getInstance().collection("posts").document(post.getId()).delete()
                                    .addOnSuccessListener(aVoid -> {
                                        int currentPos = holder.getAdapterPosition();
                                        if (currentPos != RecyclerView.NO_POSITION && currentPos < postList.size()) {
                                            postList.remove(currentPos);
                                            notifyItemRemoved(currentPos);
                                        }
                                        Toast.makeText(context, "Post permanently deleted.", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        });
            });
        }

        bottomSheetDialog.show();
    }

    private void showEditPostDialog(Context context, Post post, PostViewHolder holder) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_post, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etEditPostContent = dialogView.findViewById(R.id.etEditPostContent);
        if (etEditPostContent != null) {
            etEditPostContent.setText(post.getContent());
            etEditPostContent.setSelection(etEditPostContent.getText().length());
        }

        dialogView.findViewById(R.id.btnCancelEdit).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSaveEdit).setOnClickListener(v -> {
            String newText = etEditPostContent.getText().toString().trim();
            if (newText.isEmpty()) {
                Toast.makeText(context, "Post content cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (post.getId() == null || post.getId().isEmpty()) return;

            FirebaseFirestore.getInstance().collection("posts").document(post.getId())
                    .update("content", newText)
                    .addOnSuccessListener(aVoid -> {
                        post.setContent(newText);
                        int currentPos = holder.getAdapterPosition();
                        if (currentPos != RecyclerView.NO_POSITION) {
                            notifyItemChanged(currentPos);
                        }
                        dialog.dismiss();
                        Toast.makeText(context, "Post updated successfully.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    private void showConfirmActionDialog(Context context, String title, String subtitle, String message,
                                         String confirmBtnText, int iconRes, String tintColorHex,
                                         int frameBgRes, Runnable onConfirm) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_confirm_post_action, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvConfirmTitle);
        TextView tvSubtitle = dialogView.findViewById(R.id.tvConfirmSubtitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvConfirmMessage);
        ImageView ivIcon = dialogView.findViewById(R.id.ivConfirmIcon);
        FrameLayout layoutFrame = dialogView.findViewById(R.id.layoutConfirmIconFrame);
        androidx.appcompat.widget.AppCompatButton btnConfirm = dialogView.findViewById(R.id.btnConfirmAction);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
        if (tvMessage != null) tvMessage.setText(message);
        if (ivIcon != null) {
            ivIcon.setImageResource(iconRes);
            ivIcon.setColorFilter(Color.parseColor(tintColorHex));
        }
        if (layoutFrame != null) {
            layoutFrame.setBackgroundResource(frameBgRes);
        }
        if (btnConfirm != null) {
            btnConfirm.setText(confirmBtnText);
            if ("Confirm Archive".equals(confirmBtnText)) {
                btnConfirm.setBackgroundResource(R.drawable.bg_purple_button);
            }
        }

        dialogView.findViewById(R.id.btnCancelAction).setOnClickListener(v -> dialog.dismiss());
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                if (onConfirm != null) onConfirm.run();
            });
        }

        dialog.show();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthorName, tvPostMeta, tvPostContent, tvSeeMore, tvBadgeText, tvLikeIcon, tvLikeCount, tvCommentLabel;
        LinearLayout layoutBadge, btnLike, btnComment, btnRepost, btnSendPost;
        ImageView ivLikeIcon, ivCommentIcon, ivRepostIcon, ivSendIcon;
        android.widget.RelativeLayout layoutMediaContainer;
        FrameLayout layoutSingleVideo;
        VideoView vvSingleVideo;
        ImageView ivVideoPlayOverlay;
        View post_collage;
        ImageView collage_1_img1;
        View collage_2_container;
        ImageView collage_2_img1, collage_2_img2;
        View collage_3_container;
        ImageView collage_3_img1, collage_3_img2, collage_3_img3;
        View collage_4_container;
        ImageView collage_4_img1, collage_4_img2, collage_4_img3, collage_4_img4;
        View collage_5_container;
        ImageView collage_5_img1, collage_5_img2, collage_5_img3, collage_5_img4, collage_5_img5;
        View collage_5_overlay;
        android.widget.TextView collage_5_more_text;
        ImageView btnMoreOptions;

        public PostViewHolder(@androidx.annotation.NonNull View itemView) {
            super(itemView);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            tvPostMeta = itemView.findViewById(R.id.tvPostMeta);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvSeeMore = itemView.findViewById(R.id.tvSeeMore);
            tvBadgeText = itemView.findViewById(R.id.tvBadgeText);
            tvLikeIcon = itemView.findViewById(R.id.tvLikeIcon);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvCommentLabel = itemView.findViewById(R.id.tvCommentLabel);
            layoutBadge = itemView.findViewById(R.id.layoutBadge);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnRepost = itemView.findViewById(R.id.btnRepost);
            btnSendPost = itemView.findViewById(R.id.btnSendPost);
            ivLikeIcon = itemView.findViewById(R.id.ivLikeIcon);
            ivCommentIcon = itemView.findViewById(R.id.ivCommentIcon);
            ivRepostIcon = itemView.findViewById(R.id.ivRepostIcon);
            ivSendIcon = itemView.findViewById(R.id.ivSendIcon);
            layoutMediaContainer = itemView.findViewById(R.id.layoutMediaContainer);
            
            layoutSingleVideo = itemView.findViewById(R.id.layoutSingleVideo);
            vvSingleVideo = itemView.findViewById(R.id.vvSingleVideo);
            
            post_collage = itemView.findViewById(R.id.post_collage);
            if (post_collage != null) {
                collage_1_img1 = post_collage.findViewById(R.id.collage_1_img1);
                
                collage_2_container = post_collage.findViewById(R.id.collage_2_container);
                collage_2_img1 = post_collage.findViewById(R.id.collage_2_img1);
                collage_2_img2 = post_collage.findViewById(R.id.collage_2_img2);
                
                collage_3_container = post_collage.findViewById(R.id.collage_3_container);
                collage_3_img1 = post_collage.findViewById(R.id.collage_3_img1);
                collage_3_img2 = post_collage.findViewById(R.id.collage_3_img2);
                collage_3_img3 = post_collage.findViewById(R.id.collage_3_img3);
                
                collage_4_container = post_collage.findViewById(R.id.collage_4_container);
                collage_4_img1 = post_collage.findViewById(R.id.collage_4_img1);
                collage_4_img2 = post_collage.findViewById(R.id.collage_4_img2);
                collage_4_img3 = post_collage.findViewById(R.id.collage_4_img3);
                collage_4_img4 = post_collage.findViewById(R.id.collage_4_img4);
                
                collage_5_container = post_collage.findViewById(R.id.collage_5_container);
                collage_5_img1 = post_collage.findViewById(R.id.collage_5_img1);
                collage_5_img2 = post_collage.findViewById(R.id.collage_5_img2);
                collage_5_img3 = post_collage.findViewById(R.id.collage_5_img3);
                collage_5_img4 = post_collage.findViewById(R.id.collage_5_img4);
                collage_5_img5 = post_collage.findViewById(R.id.collage_5_img5);
                collage_5_overlay = post_collage.findViewById(R.id.collage_5_overlay);
                collage_5_more_text = post_collage.findViewById(R.id.collage_5_more_text);
            }
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
        }
    }
}
