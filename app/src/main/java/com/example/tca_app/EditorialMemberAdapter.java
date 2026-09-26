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

public class EditorialMemberAdapter extends RecyclerView.Adapter<EditorialMemberAdapter.ViewHolder> {

    public interface OnMemberPhotoClickListener {
        void onPhotoClick(EditorialMember member);
    }

    public interface OnMemberEditClickListener {
        void onEditClick(EditorialMember member);
    }

    private final Context context;
    private final List<EditorialMember> memberList;
    private final OnMemberPhotoClickListener photoListener;
    private OnMemberEditClickListener editListener;
    private boolean isAdmin = false;

    public EditorialMemberAdapter(Context context, List<EditorialMember> memberList, OnMemberPhotoClickListener photoListener) {
        this.context = context;
        this.memberList = memberList;
        this.photoListener = photoListener;
    }

    public void setEditListener(OnMemberEditClickListener editListener) {
        this.editListener = editListener;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_editorial_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EditorialMember member = memberList.get(position);

        String name = member.getName();
        holder.tvMemberName.setText(name);
        holder.tvMemberRole.setText(member.getRole());
        holder.tvMemberDepartment.setText("• " + member.getDepartment());

        // Initials
        String initial = "M";
        if (name != null && !name.trim().isEmpty()) {
            initial = String.valueOf(name.trim().charAt(0)).toUpperCase(Locale.getDefault());
        }
        holder.tvMemberInitials.setText(initial);

        // Photo loading with Glide
        String photoUrl = member.getPhotoUrl();
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            holder.ivMemberPhoto.setVisibility(View.VISIBLE);
            holder.tvMemberInitials.setVisibility(View.GONE);
            Glide.with(context)
                    .load(photoUrl)
                    .circleCrop()
                    .into(holder.ivMemberPhoto);
        } else {
            holder.ivMemberPhoto.setVisibility(View.GONE);
            holder.tvMemberInitials.setVisibility(View.VISIBLE);
        }

        // Only Admin can edit
        if (isAdmin) {
            if (holder.badgeCameraEdit != null) holder.badgeCameraEdit.setVisibility(View.VISIBLE);
            if (holder.btnChangePhoto != null) holder.btnChangePhoto.setVisibility(View.VISIBLE);

            // Camera badge / avatar frame → change photo
            View.OnClickListener photoClickAction = v -> {
                if (photoListener != null) {
                    photoListener.onPhotoClick(member);
                }
            };
            holder.frameMemberAvatar.setOnClickListener(photoClickAction);
            if (holder.badgeCameraEdit != null) holder.badgeCameraEdit.setOnClickListener(photoClickAction);

            // Pencil icon → edit name & role
            if (holder.btnChangePhoto != null) {
                holder.btnChangePhoto.setOnClickListener(v -> {
                    if (editListener != null) {
                        editListener.onEditClick(member);
                    }
                });
            }

            holder.itemView.setOnClickListener(null);
        } else {
            // Student: View-only mode
            if (holder.badgeCameraEdit != null) holder.badgeCameraEdit.setVisibility(View.GONE);
            if (holder.btnChangePhoto != null) holder.btnChangePhoto.setVisibility(View.GONE);
            holder.frameMemberAvatar.setOnClickListener(null);
            if (holder.badgeCameraEdit != null) holder.badgeCameraEdit.setOnClickListener(null);
            if (holder.btnChangePhoto != null) holder.btnChangePhoto.setOnClickListener(null);
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View frameMemberAvatar;
        ImageView ivMemberPhoto;
        TextView tvMemberInitials;
        View badgeCameraEdit;
        TextView tvMemberName;
        TextView tvMemberRole;
        TextView tvMemberDepartment;
        ImageView btnChangePhoto;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            frameMemberAvatar = itemView.findViewById(R.id.frameMemberAvatar);
            ivMemberPhoto = itemView.findViewById(R.id.ivMemberPhoto);
            tvMemberInitials = itemView.findViewById(R.id.tvMemberInitials);
            badgeCameraEdit = itemView.findViewById(R.id.badgeCameraEdit);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberRole = itemView.findViewById(R.id.tvMemberRole);
            tvMemberDepartment = itemView.findViewById(R.id.tvMemberDepartment);
            btnChangePhoto = itemView.findViewById(R.id.btnChangePhoto);
        }
    }
}
