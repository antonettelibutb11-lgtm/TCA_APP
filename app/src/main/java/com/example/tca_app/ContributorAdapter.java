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

public class ContributorAdapter extends RecyclerView.Adapter<ContributorAdapter.ContributorViewHolder> {

    private final Context context;
    private final List<Contributor> contributorList;

    public ContributorAdapter(Context context, List<Contributor> contributorList) {
        this.context = context;
        this.contributorList = contributorList;
    }

    @NonNull
    @Override
    public ContributorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_contributor, parent, false);
        return new ContributorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContributorViewHolder holder, int position) {
        Contributor contributor = contributorList.get(position);
        holder.tvName.setText(contributor.getName());
        holder.tvRole.setText(contributor.getRole());
        holder.tvPostCount.setText(contributor.getPostCount() + " posts");

        if (contributor.getAvatarUrl() != null && !contributor.getAvatarUrl().isEmpty()) {
            Glide.with(context)
                 .load(contributor.getAvatarUrl())
                 .placeholder(R.drawable.ic_bisu_logo_hd)
                 .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_bisu_logo_hd);
        }
        
        // Dynamically style based on rank
        if (position == 0) {
            holder.tvBadge.setText("🏆 Top Contributor");
        } else if (position == 1) {
            holder.tvBadge.setText("⭐ Rising Star");
        } else {
            holder.tvBadge.setText("💡 Key Member");
        }
    }

    @Override
    public int getItemCount() {
        return contributorList.size();
    }

    public static class ContributorViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvRole, tvBadge, tvPostCount;

        public ContributorViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivContributorAvatar);
            tvName = itemView.findViewById(R.id.tvContributorName);
            tvRole = itemView.findViewById(R.id.tvContributorRole);
            tvBadge = itemView.findViewById(R.id.tvContributorBadge);
            tvPostCount = itemView.findViewById(R.id.tvPostCount);
        }
    }
}
