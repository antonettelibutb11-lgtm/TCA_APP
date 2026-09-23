package com.example.tca_app;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ImageView;
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
        holder.tvPostContent.setText(post.getContent());
        holder.tvLikeCount.setText(String.valueOf(post.getLikeCount()));
        if (holder.tvCommentLabel != null) {
            holder.tvCommentLabel.setText(String.valueOf(post.getCommentCount()));
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

        // Direct Save / Download button on post action bar
        if (holder.btnDownloadPost != null) {
            holder.btnDownloadPost.setOnClickListener(v -> {
                MediaDownloadHelper.downloadPostMedia(v.getContext(), post);
            });
        }

        // Three dots menu for post options
        if (holder.btnMoreOptions != null) {
            holder.btnMoreOptions.setVisibility(View.VISIBLE);

            holder.btnMoreOptions.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), holder.btnMoreOptions);
                popup.getMenu().add("Save / Download Media");
                popup.getMenu().add("Copy Text");

                if (this.isAdmin) {
                    popup.getMenu().add("Edit");
                    popup.getMenu().add(post.isPinned() ? "Unpin Post" : "Pin Post");
                    popup.getMenu().add("Archive");
                    popup.getMenu().add("Delete");
                }

                popup.setOnMenuItemClickListener(item -> {
                    String title = item.getTitle().toString();
                    if (title.equals("Save / Download Media")) {
                        MediaDownloadHelper.downloadPostMedia(v.getContext(), post);
                        return true;
                    } else if (title.equals("Copy Text")) {
                        String text = post.getContent();
                        if (text != null && !text.isEmpty()) {
                            android.content.ClipboardManager cb = (android.content.ClipboardManager) v.getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                            if (cb != null) {
                                cb.setPrimaryClip(android.content.ClipData.newPlainText("Post Content", text));
                                Toast.makeText(v.getContext(), "📋 Post text copied to clipboard!", Toast.LENGTH_SHORT).show();
                            }
                        }
                        return true;
                    } else if (title.equals("Edit")) {
                        EditText input = new EditText(v.getContext());
                        input.setText(post.getContent());
                        input.setSelection(input.getText().length());
                        new AlertDialog.Builder(v.getContext())
                                .setTitle("Edit Post")
                                .setView(input)
                                .setPositiveButton("Save", (dialog, which) -> {
                                    String newText = input.getText().toString().trim();
                                    if (!newText.isEmpty() && post.getId() != null && !post.getId().isEmpty()) {
                                        FirebaseFirestore.getInstance().collection("posts").document(post.getId())
                                                .update("content", newText)
                                                .addOnSuccessListener(aVoid -> {
                                                    post.setContent(newText);
                                                    int currentPos = holder.getAdapterPosition();
                                                    if (currentPos != RecyclerView.NO_POSITION) {
                                                        notifyItemChanged(currentPos);
                                                    }
                                                    Toast.makeText(v.getContext(), "Post updated successfully!", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(v.getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        return true;
                    } else if (title.equals("Pin Post") || title.equals("Unpin Post")) {
                        boolean newPinnedStatus = title.equals("Pin Post");
                        FirebaseFirestore.getInstance().collection("posts").document(post.getId())
                                .update("isPinned", newPinnedStatus)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(v.getContext(), newPinnedStatus ? "📌 Post Pinned" : "Post Unpinned", Toast.LENGTH_SHORT).show();
                                });
                        return true;
                    } else if (title.equals("Archive")) {
                        new AlertDialog.Builder(v.getContext())
                                .setTitle("Archive Post")
                                .setMessage("Hide this post from the feed? It won't be deleted, but no one will see it.")
                                .setPositiveButton("Archive", (dialog, which) -> {
                                    FirebaseFirestore.getInstance().collection("posts").document(post.getId())
                                            .update("moderationStatus", "ARCHIVED")
                                            .addOnSuccessListener(aVoid -> {
                                                int currentPos = holder.getAdapterPosition();
                                                if (currentPos != RecyclerView.NO_POSITION) {
                                                    postList.remove(currentPos);
                                                    notifyItemRemoved(currentPos);
                                                    Toast.makeText(v.getContext(), "📦 Post archived", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        return true;
                    } else if (title.equals("Delete")) {
                        new AlertDialog.Builder(v.getContext())
                                .setTitle("Delete Post")
                                .setMessage("Are you sure you want to permanently delete this post?")
                                .setPositiveButton("Delete", (dialog, which) -> {
                                    FirebaseFirestore.getInstance().collection("posts").document(post.getId()).delete()
                                            .addOnSuccessListener(aVoid -> {
                                                int currentPos = holder.getAdapterPosition();
                                                if (currentPos != RecyclerView.NO_POSITION) {
                                                    postList.remove(currentPos);
                                                    notifyItemRemoved(currentPos);
                                                    Toast.makeText(v.getContext(), "🗑️ Post deleted", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(v.getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
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
                            Toast.makeText(v.getContext(), "👍 Liked post!", Toast.LENGTH_SHORT).show();
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
                        .setTitle("🔁 Repost to Feed")
                        .setMessage("Share this post to your profile and campus feed?")
                        .setPositiveButton("Repost", (dialog, which) -> {
                            holder.btnRepost.setEnabled(false);
                            String currentUid = uid;
                            String currentUserName = (currentUser != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty())
                                    ? currentUser.getDisplayName() : "BISU Student";

                            Map<String, Object> repostMap = new HashMap<>();
                            repostMap.put("authorName", currentUserName);
                            repostMap.put("authorUid", currentUid);
                            repostMap.put("postMeta", "Just now • 🔁 Reposted from " + post.getAuthorName());
                            repostMap.put("content", post.getContent() != null ? post.getContent() : "");
                            repostMap.put("badgeText", "🔁 Repost");
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
                                        Toast.makeText(ctx, "🔁 Reposted successfully to your feed!", Toast.LENGTH_SHORT).show();
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

        // Tap Report -> Securely Submit Report directly to Firestore moderation_queue
        if (holder.btnReportPost != null) {
            holder.btnReportPost.setOnClickListener(v -> {
                if (post.getId() == null || post.getId().isEmpty()) return;
                Context ctx = v.getContext();
                new AlertDialog.Builder(ctx)
                        .setTitle("🚩 Report Post")
                        .setMessage("Submit this post to campus moderators for review?")
                        .setPositiveButton("Report", (dialog, which) -> {
                            holder.btnReportPost.setEnabled(false);
                            String currentUid = uid;

                            Map<String, Object> flagData = new HashMap<>();
                            flagData.put("postId", post.getId());
                            flagData.put("content", post.getContent() != null ? post.getContent() : "");
                            flagData.put("authorName", post.getAuthorName() != null ? post.getAuthorName() : "");
                            flagData.put("reportedByUid", currentUid);
                            flagData.put("reason", "Reported by User");
                            flagData.put("aiScore", "User Flag");
                            flagData.put("moderationStatus", "PENDING");
                            flagData.put("timestamp", System.currentTimeMillis());

                            FirebaseFirestore.getInstance().collection("moderation_queue")
                                    .add(flagData)
                                    .addOnSuccessListener(docRef -> {
                                        holder.btnReportPost.setEnabled(true);
                                        Toast.makeText(ctx, "🚩 Report submitted to Moderation Queue!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        holder.btnReportPost.setEnabled(true);
                                        Toast.makeText(ctx, "❌ Failed to submit report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    private void showReactionPicker(Context context, Post post, PostViewHolder holder) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reactions_horizontal, null);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvReactLike = dialogView.findViewById(R.id.tvReactLike);
        TextView tvReactHeart = dialogView.findViewById(R.id.tvReactHeart);
        TextView tvReactCare = dialogView.findViewById(R.id.tvReactCare);
        TextView tvReactHaha = dialogView.findViewById(R.id.tvReactHaha);
        TextView tvReactWow = dialogView.findViewById(R.id.tvReactWow);
        TextView tvReactAngry = dialogView.findViewById(R.id.tvReactAngry);

        // Animate the emojis popping in one by one
        TextView[] emojis = {tvReactLike, tvReactHeart, tvReactCare, tvReactHaha, tvReactWow, tvReactAngry};
        for (int i = 0; i < emojis.length; i++) {
            if (emojis[i] != null) {
                emojis[i].setScaleX(0f);
                emojis[i].setScaleY(0f);
                emojis[i].animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(250)
                        .setStartDelay(i * 30)
                        .setInterpolator(new android.view.animation.OvershootInterpolator())
                        .start();
            }
        }
        View.OnClickListener reactionListener = v -> {
            String emoji = "👍";
            if (v == tvReactHeart) emoji = "❤️";
            else if (v == tvReactCare) emoji = "🤗";
            else if (v == tvReactHaha) emoji = "😂";
            else if (v == tvReactWow) emoji = "😮";
            else if (v == tvReactAngry) emoji = "😡";

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String uid = (currentUser != null && currentUser.getUid() != null) ? currentUser.getUid() : "guest_user";

            if (post.isLikedByCurrentUser()) {
                Toast.makeText(context, "⚠️ You have already reacted to this post!", Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(context, "💬 Reply posted!", Toast.LENGTH_SHORT).show();
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
                                                Toast.makeText(context, "💬 Comment posted!", Toast.LENGTH_SHORT).show();
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

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthorName, tvPostMeta, tvPostContent, tvBadgeText, tvLikeIcon, tvLikeCount, tvCommentLabel;
        LinearLayout layoutBadge, btnLike, btnComment, btnRepost, btnReportPost;
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
        View btnDownloadPost;

        public PostViewHolder(@androidx.annotation.NonNull View itemView) {
            super(itemView);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            tvPostMeta = itemView.findViewById(R.id.tvPostMeta);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvBadgeText = itemView.findViewById(R.id.tvBadgeText);
            tvLikeIcon = itemView.findViewById(R.id.tvLikeIcon);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvCommentLabel = itemView.findViewById(R.id.tvCommentLabel);
            layoutBadge = itemView.findViewById(R.id.layoutBadge);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnRepost = itemView.findViewById(R.id.btnRepost);
            btnReportPost = itemView.findViewById(R.id.btnReportPost);
            btnDownloadPost = itemView.findViewById(R.id.btnDownloadPost);
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
