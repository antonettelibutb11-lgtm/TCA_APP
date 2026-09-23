package com.example.tca_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;

public class MemberAvatarStoryAdapter extends RecyclerView.Adapter<MemberAvatarStoryAdapter.ViewHolder> {

    public interface OnMemberClickListener {
        void onMemberClick(EditorialMember member);
    }

    private final Context context;
    private final List<EditorialMember> memberList;
    private final OnMemberClickListener listener;
    private boolean isAdmin = false;

    public MemberAvatarStoryAdapter(Context context, List<EditorialMember> memberList, OnMemberClickListener listener) {
        this.context = context;
        this.memberList = memberList;
        this.listener = listener;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member_avatar_story, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EditorialMember member = memberList.get(position);

        String fullName = member.getName();
        String firstName = fullName;
        if (fullName != null && fullName.contains(" ")) {
            firstName = fullName.split(" ")[0];
        }
        holder.tvStoryMemberName.setText(firstName);
        holder.tvStoryMemberRole.setText(member.getRole());

        // Initials fallback
        String initial = "M";
        if (fullName != null && !fullName.trim().isEmpty()) {
            initial = String.valueOf(fullName.trim().charAt(0)).toUpperCase(Locale.getDefault());
        }
        holder.tvStoryAvatarInitials.setText(initial);

        // Photo loading with Glide
        String photoUrl = member.getPhotoUrl();
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            holder.ivStoryAvatarPhoto.setVisibility(View.VISIBLE);
            holder.tvStoryAvatarInitials.setVisibility(View.GONE);
            Glide.with(context)
                    .load(photoUrl)
                    .circleCrop()
                    .into(holder.ivStoryAvatarPhoto);
        } else {
            holder.ivStoryAvatarPhoto.setVisibility(View.GONE);
            holder.tvStoryAvatarInitials.setVisibility(View.VISIBLE);
        }

        if (holder.badgeStoryCameraEdit != null) {
            holder.badgeStoryCameraEdit.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMemberClick(member);
            }
        });
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStoryAvatarPhoto;
        TextView tvStoryAvatarInitials;
        TextView tvStoryMemberName;
        TextView tvStoryMemberRole;
        View badgeStoryCameraEdit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStoryAvatarPhoto = itemView.findViewById(R.id.ivStoryAvatarPhoto);
            tvStoryAvatarInitials = itemView.findViewById(R.id.tvStoryAvatarInitials);
            tvStoryMemberName = itemView.findViewById(R.id.tvStoryMemberName);
            tvStoryMemberRole = itemView.findViewById(R.id.tvStoryMemberRole);
            badgeStoryCameraEdit = itemView.findViewById(R.id.badgeStoryCameraEdit);
        }
    }
}
