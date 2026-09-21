package com.example.tca_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class OrganizationFragment extends Fragment {

    private RecyclerView rvTopContributors;
    private ContributorAdapter contributorAdapter;
    private List<Contributor> topContributorsList;

    private RecyclerView rvMembersList;
    private MemberAdapter memberAdapter;
    private List<Contributor> fullMembersList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_organization, container, false);

        rvTopContributors = view.findViewById(R.id.rvTopContributors);
        rvMembersList = view.findViewById(R.id.rvMembersList);

        rvTopContributors.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvMembersList.setLayoutManager(new LinearLayoutManager(getContext()));

        topContributorsList = new ArrayList<>();
        fullMembersList = new ArrayList<>();

        contributorAdapter = new ContributorAdapter(getContext(), topContributorsList);
        rvTopContributors.setAdapter(contributorAdapter);

        memberAdapter = new MemberAdapter(getContext(), fullMembersList);
        rvMembersList.setAdapter(memberAdapter);

        loadDynamicMembers();

        return view;
    }

    private void loadDynamicMembers() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("isMember", true)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!isAdded() || getContext() == null || getView() == null) return;
                    if (snapshots != null && !snapshots.isEmpty()) {
                        List<Contributor> allMembers = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                            String id = doc.getId();
                            String name = doc.getString("name");
                            String role = doc.getString("role");
                            String avatarUrl = doc.getString("profileImageUrl"); // if they have one
                            Long postCountLong = doc.getLong("postCount");
                            long postCount = postCountLong != null ? postCountLong : 0;

                            if (name == null || name.isEmpty()) name = "Member";
                            if (role == null || role.isEmpty()) role = "Contributor";

                            allMembers.add(new Contributor(id, name, role, avatarUrl, postCount));
                        }

                        // Sort locally by postCount descending
                        java.util.Collections.sort(allMembers, (c1, c2) -> Long.compare(c2.getPostCount(), c1.getPostCount()));

                        topContributorsList.clear();
                        fullMembersList.clear();

                        // Top 3 go to contributors list
                        int topCount = Math.min(3, allMembers.size());
                        for (int i = 0; i < topCount; i++) {
                            Contributor c = allMembers.get(i);
                            c.setTopContributor(true);
                            topContributorsList.add(c);
                        }

                        // Everyone goes to the full members list
                        fullMembersList.addAll(allMembers);

                        if (contributorAdapter != null) contributorAdapter.notifyDataSetChanged();
                        if (memberAdapter != null) memberAdapter.notifyDataSetChanged();
                    }
                });
    }
}
