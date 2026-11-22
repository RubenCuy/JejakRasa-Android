package com.example.jejakrasa.model;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SavedSpotsManager {

    public interface OnSpotsLoadedListener {
        void onLoaded(List<CulinarySpot> spots);
    }

    public interface OnToggleListener {
        void onToggled(boolean isSaved);
    }

    private static final List<CulinarySpot> cachedSpots = new ArrayList<>();
    private static String cachedUid = null;

    private static String getCurrentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (user != null) ? user.getUid() : null;
        if (uid == null || !uid.equals(cachedUid)) {
            cachedSpots.clear();
            cachedUid = uid;
        }
        return uid;
    }

    public static void clearCache() {
        cachedSpots.clear();
        cachedUid = null;
    }

    public static List<CulinarySpot> getCachedSpots() {
        getCurrentUid();
        return new ArrayList<>(cachedSpots);
    }

    public static boolean isSaved(String spotId) {
        getCurrentUid();
        for (CulinarySpot s : cachedSpots) {
            if (s.getId() != null && s.getId().equals(spotId)) {
                return true;
            }
        }
        return false;
    }

    public static void loadUserSavedSpots(OnSpotsLoadedListener listener) {
        String uid = getCurrentUid();
        if (uid == null) {
            if (listener != null) listener.onLoaded(new ArrayList<>());
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("saved_spots")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!uid.equals(getCurrentUid())) return;
                    cachedSpots.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        CulinarySpot spot = doc.toObject(CulinarySpot.class);
                        if (spot != null) {
                            spot.setId(doc.getId());
                            cachedSpots.add(spot);
                        }
                    }
                    if (listener != null) listener.onLoaded(new ArrayList<>(cachedSpots));
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onLoaded(new ArrayList<>(cachedSpots));
                });
    }

    public static void toggleSave(CulinarySpot spot, OnToggleListener listener) {
        String uid = getCurrentUid();
        if (uid == null || spot == null || spot.getId() == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        boolean currentlySaved = isSaved(spot.getId());

        if (currentlySaved) {
            for (int i = 0; i < cachedSpots.size(); i++) {
                if (spot.getId().equals(cachedSpots.get(i).getId())) {
                    cachedSpots.remove(i);
                    break;
                }
            }
            db.collection("users").document(uid)
                    .collection("saved_spots").document(spot.getId())
                    .delete()
                    .addOnCompleteListener(t -> {
                        if (listener != null) listener.onToggled(false);
                    });
        } else {
            cachedSpots.add(spot);
            db.collection("users").document(uid)
                    .collection("saved_spots").document(spot.getId())
                    .set(spot)
                    .addOnCompleteListener(t -> {
                        if (listener != null) listener.onToggled(true);
                    });
        }
    }
}