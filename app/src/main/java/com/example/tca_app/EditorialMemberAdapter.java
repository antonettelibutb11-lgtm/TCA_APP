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

    private final Context context;
    private final List<EditorialMember> memberList;
    private final OnMemberPhotoClickListener listener;
    private boolean isAdmin = false;

    public EditorialMemberAdapter(Context context, List<EditorialMember> memberList, OnMemberPhotoClickListener listener) {
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

        // Only Admin can edit photos
        if (isAdmin) {
            if (holder.badgeCameraEdit != null) holder.badgeCameraEdit.setVisibility(View.VISIBLE);
            if (holder.btnChangePhoto != null) holder.btnChangePhoto.setVisibility(View.VISIBLE);

            View.OnClickListener clickAction = v -> {
                if (listener != null) {
                    listener.onPhotoClick(member);
                }
            };
            holder.frameMemberAvatar.setOnClickListener(clickAction);
            holder.btnChangePhoto.setOnClickListener(clickAction);
            holder.itemView.setOnClickListener(clickAction);
        } else {
            // Student: View-only mode, cannot edit photos
            if (holder.badgeCameraEdit != null) holder.badgeCameraEdit.setVisibility(View.GONE);
            if (holder.btnChangePhoto != null) holder.btnChangePhoto.setVisibility(View.GONE);
            holder.frameMemberAvatar.setOnClickListener(null);
            holder.btnChangePhoto.setOnClickListener(null);
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
