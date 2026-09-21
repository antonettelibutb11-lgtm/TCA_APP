package com.example.tca_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private final Context context;
    private final List<Contributor> memberList;
    private boolean isAdmin = false;
    private String currentUserId = "";

    public MemberAdapter(Context context, List<Contributor> memberList) {
        this.context = context;
        this.memberList = memberList;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            FirebaseFirestore.getInstance().collection("users").document(currentUserId)
                .get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        isAdmin = "ADMIN".equals(role);
                    }
                });
        }
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Contributor member = memberList.get(position);
        holder.tvName.setText(member.getName());
        holder.tvRole.setText(member.getRole());
        holder.tvPostCount.setText(member.getPostCount() + " posts");

        if (member.getAvatarUrl() != null && !member.getAvatarUrl().isEmpty()) {
            Glide.with(context)
                 .load(member.getAvatarUrl())
                 .placeholder(R.drawable.ic_bisu_logo_hd)
                 .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_bisu_logo_hd);
        }

        holder.btnMoreOptions.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;
            
            PopupMenu popupMenu = new PopupMenu(context, holder.btnMoreOptions);
            boolean isSelf = currentUserId.equals(member.getId());
            
            if (isAdmin && !isSelf) {
                popupMenu.getMenu().add(0, 1, 0, "Remove Member");
            }
            if (isSelf) {
                popupMenu.getMenu().add(0, 2, 0, "Leave Campus Access");
            }
            
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1 || item.getItemId() == 2) {
                    FirebaseFirestore.getInstance().collection("users").document(member.getId())
                        .update("isMember", false, "role", "STUDENT")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, member.getName() + " removed from Campus Access", Toast.LENGTH_SHORT).show();
                            int posToRemove = holder.getAdapterPosition();
                            if (posToRemove != RecyclerView.NO_POSITION) {
                                memberList.remove(posToRemove);
                                notifyItemRemoved(posToRemove);
                                notifyItemRangeChanged(posToRemove, memberList.size());
                            }
                        });
                    return true;
                }
                return false;
            });
            
            if (popupMenu.getMenu().size() > 0) {
                popupMenu.show();
            } else {
                Toast.makeText(context, "No actions available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, btnMoreOptions;
        TextView tvName, tvRole, tvPostCount;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivMemberAvatar);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvRole = itemView.findViewById(R.id.tvMemberRole);
            tvPostCount = itemView.findViewById(R.id.tvMemberPostCount);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
        }
    }
}
