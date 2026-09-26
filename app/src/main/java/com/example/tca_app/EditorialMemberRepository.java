package com.example.tca_app;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorialMemberRepository {

    private static final String COLLECTION = "editorial_members";
    private static final String PREF_NAME = "tca_editorial_members_cache";
    private static final String KEY_PREFIX_PHOTO = "photo_url_";

    public interface MemberListCallback {
        void onLoaded(List<EditorialMember> members);
    }

    public interface MemberUpdateCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public static List<EditorialMember> getDefaultPresets() {
        List<EditorialMember> list = new ArrayList<>();
        int order = 1;

        // EDITORIAL BOARDS - Executives
        list.add(new EditorialMember("exec_1", "John Reno Villapaña", "Editorial Board", "Editor-in-Chief", "", order++));
        list.add(new EditorialMember("exec_2", "Asheyl Bergn Cabarles", "Editorial Board", "Associate Editor-in-Chief", "", order++));
        list.add(new EditorialMember("exec_3", "Stephanie Mae Canabe", "Editorial Board", "Managing Editor", "", order++));
        list.add(new EditorialMember("exec_4", "Dave Torallba", "Editorial Board", "Creative Director", "", order++));
        list.add(new EditorialMember("exec_5", "Reydeyl Dianne Ong", "Editorial Board", "Circulation Manager", "", order++));
        list.add(new EditorialMember("exec_6", "John Celo Butron", "Editorial Board", "Financial Manager", "", order++));

        // EDITORIAL BOARDS - Department Heads
        list.add(new EditorialMember("head_1", "Asheyl Bergn Cabarles", "Editorial Board", "News Editor", "", order++));
        list.add(new EditorialMember("head_2", "Ma. Ayezza Rhenn Jaum", "Editorial Board", "Literary Editor", "", order++));
        list.add(new EditorialMember("head_3", "Danaea Quimbo", "Editorial Board", "PJ Head", "", order++));
        list.add(new EditorialMember("head_4", "Lere Brian Dopeño", "Editorial Board", "VJ Head", "", order++));
        list.add(new EditorialMember("head_5", "Glenndhel Jay Dano", "Editorial Board", "Layout Artist Head", "", order++));
        list.add(new EditorialMember("head_6", "Althea Rhea Tanaid", "Editorial Board", "Editorial Cartoonist Head", "", order++));
        list.add(new EditorialMember("head_7", "Roxanne Dapar", "Editorial Board", "Broadcaster Head", "", order++));

        // WRITING DEPARTMENT - News Writers
        list.add(new EditorialMember("news_1", "Erika Tacurian", "Writing Department", "News Writer", "", order++));
        list.add(new EditorialMember("news_2", "Ferdinand Roy Lopez", "Writing Department", "News Writer", "", order++));
        list.add(new EditorialMember("news_3", "Roche Mae Butil", "Writing Department", "News Writer", "", order++));
        list.add(new EditorialMember("news_4", "Prince Ghian Natividad", "Writing Department", "News Writer", "", order++));
        list.add(new EditorialMember("news_5", "George Kendred Baugbog", "Writing Department", "News Writer", "", order++));
        list.add(new EditorialMember("news_6", "Arizza Claire Helig", "Writing Department", "News Writer", "", order++));
        list.add(new EditorialMember("news_7", "Reydeyl Dianne Ong", "Writing Department", "News Writer", "", order++));

        // WRITING DEPARTMENT - Literary Writers
        list.add(new EditorialMember("lit_1", "Claire Zafra", "Writing Department", "Literary Writer", "", order++));
        list.add(new EditorialMember("lit_2", "Stephanie Mae Canabe", "Writing Department", "Literary Writer", "", order++));
        list.add(new EditorialMember("lit_3", "Rubelyn Oliveros", "Writing Department", "Literary Writer", "", order++));
        list.add(new EditorialMember("lit_4", "Rachelle Calicdan", "Writing Department", "Literary Writer", "", order++));
        list.add(new EditorialMember("lit_5", "Julian Robles", "Writing Department", "Literary Writer", "", order++));
        list.add(new EditorialMember("lit_6", "Jay Ann Domasin", "Writing Department", "Literary Writer", "", order++));

        // CREATIVE DEPARTMENT - Photojournalists (PJ)
        list.add(new EditorialMember("pj_1", "Vincent Lumantas", "Creative Department", "Photojournalist (PJ)", "", order++));
        list.add(new EditorialMember("pj_2", "Mhiaryle Batistis", "Creative Department", "Photojournalist (PJ)", "", order++));
        list.add(new EditorialMember("pj_3", "Shaina Kastein Balaba", "Creative Department", "Photojournalist (PJ)", "", order++));
        list.add(new EditorialMember("pj_4", "Jaqueline Nicole Baquero", "Creative Department", "Photojournalist (PJ)", "", order++));
        list.add(new EditorialMember("pj_5", "Rudmark Renion", "Creative Department", "Photojournalist (PJ)", "", order++));
        list.add(new EditorialMember("pj_6", "Kael Sumaylo", "Creative Department", "Photojournalist (PJ)", "", order++));
        list.add(new EditorialMember("pj_7", "Hazel Orig", "Creative Department", "Photojournalist (PJ)", "", order++));
        list.add(new EditorialMember("pj_8", "Japheth Hamshem Jumamoy", "Creative Department", "Photojournalist (PJ)", "", order++));
        list.add(new EditorialMember("pj_9", "Carissa Jane Bonajos", "Creative Department", "Photojournalist (PJ)", "", order++));
        list.add(new EditorialMember("pj_10", "Realujah Odeza Peligro", "Creative Department", "Photojournalist (PJ)", "", order++));

        // CREATIVE DEPARTMENT - Videojournalists (VJ)
        list.add(new EditorialMember("vj_1", "Cristel Lacea", "Creative Department", "Videojournalist (VJ)", "", order++));
        list.add(new EditorialMember("vj_2", "John Kenneth Celades", "Creative Department", "Videojournalist (VJ)", "", order++));
        list.add(new EditorialMember("vj_3", "Kharylle Gabato", "Creative Department", "Videojournalist (VJ)", "", order++));
        list.add(new EditorialMember("vj_4", "Earl Lorence Ocon", "Creative Department", "Videojournalist (VJ)", "", order++));
        list.add(new EditorialMember("vj_5", "Nelson Jade Jaum", "Creative Department", "Videojournalist (VJ)", "", order++));

        // CREATIVE DEPARTMENT - Layout Artists
        list.add(new EditorialMember("layout_1", "John Carl Lacea", "Creative Department", "Layout Artist", "", order++));
        list.add(new EditorialMember("layout_2", "John Celo Butron", "Creative Department", "Layout Artist", "", order++));
        list.add(new EditorialMember("layout_3", "Karl Jacquin Ag-ag", "Creative Department", "Layout Artist", "", order++));
        list.add(new EditorialMember("layout_4", "Marc Laurence Salido", "Creative Department", "Layout Artist", "", order++));
        list.add(new EditorialMember("layout_5", "Tyrone Briones", "Creative Department", "Layout Artist", "", order++));
        list.add(new EditorialMember("layout_6", "Yusoph Marohombsar", "Creative Department", "Layout Artist", "", order++));

        // CREATIVE DEPARTMENT - Editorial Cartoonists
        list.add(new EditorialMember("cart_1", "Julex Kyle Elle", "Creative Department", "Editorial Cartoonist", "", order++));
        list.add(new EditorialMember("cart_2", "Althea Rhea Tanaid", "Creative Department", "Editorial Cartoonist", "", order++));
        list.add(new EditorialMember("cart_3", "Vanessa Chana Chatto", "Creative Department", "Editorial Cartoonist", "", order++));

        // BROADCASTING DEPARTMENT - Broadcasters
        list.add(new EditorialMember("bcast_1", "Angel Nichole Compania", "Broadcasting Department", "Broadcaster", "", order++));
        list.add(new EditorialMember("bcast_2", "Claire Rea Vallejo", "Broadcasting Department", "Broadcaster", "", order++));
        list.add(new EditorialMember("bcast_3", "Floss Carmelli Daquio", "Broadcasting Department", "Broadcaster", "", order++));
        list.add(new EditorialMember("bcast_4", "Joshua Barbacina", "Broadcasting Department", "Broadcaster", "", order++));
        list.add(new EditorialMember("bcast_5", "Prince Rexorblue Villarin", "Broadcasting Department", "Broadcaster", "", order++));

        return list;
    }

    public static void loadMembers(Context context, MemberListCallback callback) {
        final Context appContext = context.getApplicationContext();
        final List<EditorialMember> defaults = getDefaultPresets();
        applyCachedPhotos(appContext, defaults);

        FirebaseFirestore.getInstance().collection(COLLECTION)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots == null || snapshots.isEmpty()) {
                        // Seed defaults to Firestore
                        seedPresetsToFirestore(defaults);
                        callback.onLoaded(defaults);
                    } else {
                        Map<String, EditorialMember> map = new HashMap<>();
                        for (EditorialMember def : defaults) {
                            map.put(def.getId(), def);
                        }

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String id = doc.getId();
                            EditorialMember member = map.get(id);
                            if (member != null) {
                                String photo = doc.getString("photoUrl");
                                if (photo != null && !photo.isEmpty()) {
                                    member.setPhotoUrl(photo);
                                    saveCachedPhoto(appContext, id, photo);
                                }
                            } else {
                                String name = doc.getString("name");
                                String department = doc.getString("department");
                                String role = doc.getString("role");
                                String photo = doc.getString("photoUrl");
                                Long orderVal = doc.getLong("order");
                                int order = orderVal != null ? orderVal.intValue() : 999;
                                if (name != null && !name.trim().isEmpty()) {
                                    EditorialMember newMember = new EditorialMember(
                                            id,
                                            name,
                                            department != null ? department : "Editorial Board",
                                            role != null ? role : "Staff Member",
                                            photo != null ? photo : "",
                                            order
                                    );
                                    if (photo != null && !photo.isEmpty()) {
                                        saveCachedPhoto(appContext, id, photo);
                                    }
                                    map.put(id, newMember);
                                }
                            }
                        }

                        List<EditorialMember> result = new ArrayList<>(map.values());
                        Collections.sort(result, (m1, m2) -> Integer.compare(m1.getOrder(), m2.getOrder()));
                        callback.onLoaded(result);
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onLoaded(defaults);
                });
    }

    private static void seedPresetsToFirestore(List<EditorialMember> members) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (EditorialMember m : members) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", m.getId());
            data.put("name", m.getName());
            data.put("department", m.getDepartment());
            data.put("role", m.getRole());
            data.put("photoUrl", m.getPhotoUrl());
            data.put("order", m.getOrder());
            db.collection(COLLECTION).document(m.getId()).set(data, SetOptions.merge());
        }
    }

    public static void addMember(Context context, EditorialMember member, MemberUpdateCallback callback) {
        if (member == null) return;
        final Context appContext = context.getApplicationContext();

        Map<String, Object> data = new HashMap<>();
        data.put("id", member.getId());
        data.put("name", member.getName());
        data.put("department", member.getDepartment());
        data.put("role", member.getRole());
        data.put("photoUrl", member.getPhotoUrl());
        data.put("order", member.getOrder());

        FirebaseFirestore.getInstance().collection(COLLECTION).document(member.getId())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (member.getPhotoUrl() != null && !member.getPhotoUrl().isEmpty()) {
                        saveCachedPhoto(appContext, member.getId(), member.getPhotoUrl());
                    }
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public static void updateMemberPhoto(Context context, String memberId, String photoUrl, MemberUpdateCallback callback) {
        saveCachedPhoto(context.getApplicationContext(), memberId, photoUrl);

        Map<String, Object> update = new HashMap<>();
        update.put("photoUrl", photoUrl);

        FirebaseFirestore.getInstance().collection(COLLECTION).document(memberId)
                .set(update, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public static void updateMemberInfo(Context context, String memberId, String newName, String newDepartment, String newRole, MemberUpdateCallback callback) {
        Map<String, Object> update = new HashMap<>();
        update.put("name", newName);
        update.put("department", newDepartment);
        update.put("role", newRole);

        FirebaseFirestore.getInstance().collection(COLLECTION).document(memberId)
                .set(update, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    private static void applyCachedPhotos(Context context, List<EditorialMember> members) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        for (EditorialMember m : members) {
            String cached = prefs.getString(KEY_PREFIX_PHOTO + m.getId(), null);
            if (cached != null && !cached.isEmpty()) {
                m.setPhotoUrl(cached);
            }
        }
    }

    private static void saveCachedPhoto(Context context, String memberId, String photoUrl) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PREFIX_PHOTO + memberId, photoUrl).apply();
    }
}
