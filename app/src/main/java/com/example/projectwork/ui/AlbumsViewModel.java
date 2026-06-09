package com.example.projectwork.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.projectwork.model.Album;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class AlbumsViewModel extends ViewModel {

    private final MutableLiveData<List<Album>> albumsLiveData = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public LiveData<List<Album>> getAlbums() {
        return albumsLiveData;
    }

    public void loadAlbums() {
        String userId = auth.getCurrentUser().getUid();

        db.collection("albums")
                .whereArrayContains("memberIds", userId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    List<Album> newAlbums = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Album album = doc.toObject(Album.class);
                        album.setId(doc.getId());
                        newAlbums.add(album);
                    }

                    newAlbums.sort((a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    });

                    albumsLiveData.postValue(newAlbums);
                });
    }

    public void deleteAlbum(String albumId) {
        db.collection("albums").document(albumId).delete();
    }

    public void leaveAlbum(String albumId) {
        String userId = auth.getCurrentUser().getUid();
        db.collection("albums").document(albumId)
                .update(
                        "memberIds",
                        com.google.firebase.firestore.FieldValue.arrayRemove(userId),
                        "roles." + userId,
                        com.google.firebase.firestore.FieldValue.delete()
                );
    }

    public void createAlbum(String title) {
        String userId = auth.getCurrentUser().getUid();

        java.util.Map<String, Object> roles = new java.util.HashMap<>();
        roles.put(userId, "owner");

        java.util.Map<String, Object> album = new java.util.HashMap<>();
        album.put("title", title);
        album.put("ownerId", userId);
        album.put("memberIds", java.util.Arrays.asList(userId));
        album.put("createdAt", com.google.firebase.Timestamp.now());
        album.put("roles", roles);

        db.collection("albums").add(album);
    }
}