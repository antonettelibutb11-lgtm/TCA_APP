package com.example.tca_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MembershipRequestAdapter extends RecyclerView.Adapter<MembershipRequestAdapter.ViewHolder> {

    public interface OnActionListener {
        void onRequestHandled(int remainingCount);
    }

    private List<MembershipRequest> requestList;
    private OnActionListener listener;

    public MembershipRequestAdapter(List<MembershipRequest> requestList, OnActionListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_membership_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MembershipRequest item = requestList.get(position);
        holder.tvStudentName.setText(item.getName());
        holder.tvStudentEmail.setText(item.getEmail());

        holder.btnApproveMember.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION && currentPos < requestList.size()) {
                MembershipRequest req = requestList.get(currentPos);
                setButtonsEnabled(holder, false);
                processMembershipAction(req, "APPROVE", v.getContext(), currentPos, holder);
            }
        });

        holder.btnRejectMember.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION && currentPos < requestList.size()) {
                MembershipRequest req = requestList.get(currentPos);
                setButtonsEnabled(holder, false);
                processMembershipAction(req, "REJECT", v.getContext(), currentPos, holder);
            }
        });
    }

    private void setButtonsEnabled(ViewHolder holder, boolean enabled) {
        if (holder.btnApproveMember != null) holder.btnApproveMember.setEnabled(enabled);
        if (holder.btnRejectMember != null) holder.btnRejectMember.setEnabled(enabled);
    }

    private void processMembershipAction(MembershipRequest req, String action, Context context, int position, ViewHolder holder) {
        String uid = req.getUserUid().isEmpty() ? req.getId() : req.getUserUid();
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        
        if ("APPROVE".equalsIgnoreCase(action)) {
            updates.put("isMember", true);
            updates.put("isMemberPending", false);
        } else {
            updates.put("isMember", false);
            updates.put("isMemberPending", false);
        }
        
        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(result -> {
                    setButtonsEnabled(holder, true);
                    String statusStr = "APPROVE".equalsIgnoreCase(action) ? "Approved" : "Rejected";
                    Toast.makeText(context, "✅ Membership " + statusStr + " for " + req.getName(), Toast.LENGTH_SHORT).show();
                    int latestPos = holder.getAdapterPosition();
                    if (latestPos != RecyclerView.NO_POSITION && latestPos < requestList.size()) {
                        requestList.remove(latestPos);
                        notifyItemRemoved(latestPos);
                        notifyItemRangeChanged(latestPos, requestList.size());
                        if (listener != null) listener.onRequestHandled(requestList.size());
                    }
                })
                .addOnFailureListener(e -> {
                    setButtonsEnabled(holder, true);
                    Toast.makeText(context, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvStudentEmail, tvRequestMeta;
        Button btnApproveMember, btnRejectMember;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentEmail = itemView.findViewById(R.id.tvStudentEmail);
            tvRequestMeta = itemView.findViewById(R.id.tvRequestMeta);
            btnApproveMember = itemView.findViewById(R.id.btnApproveMember);
            btnRejectMember = itemView.findViewById(R.id.btnRejectMember);
        }
    }
}
