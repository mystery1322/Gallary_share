package com.example.projectwork.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.projectwork.R;
import com.example.projectwork.model.Photo;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlbumActivity extends AppCompatActivity {

    private RecyclerView rvPhotos;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Photo> photoList = new ArrayList<>();
    private PhotoAdapter adapter;
    private String albumId;
    private String albumTitle;
    private String ownerId = "";
    private String currentUserRole = "viewer";
    private FloatingActionButton fabAddPhoto;

    private ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) {
                        uploadPhoto(uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        albumId = getIntent().getStringExtra("albumId");
        albumTitle = getIntent().getStringExtra("albumTitle");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(albumTitle);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        fabAddPhoto = findViewById(R.id.fabAddPhoto);
        fabAddPhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        rvPhotos = findViewById(R.id.rvPhotos);
        adapter = new PhotoAdapter(photoList, photo -> {
            ArrayList<String> urls = new ArrayList<>();
            ArrayList<String> uploaderIds = new ArrayList<>();
            ArrayList<String> photoIds = new ArrayList<>();
            for (Photo p : photoList) {
                urls.add(p.getUrl());
                uploaderIds.add(p.getUploaderId());
                photoIds.add(p.getId());
            }
            int position = photoList.indexOf(photo);
            Intent intent = new Intent(this, PhotoViewActivity.class);
            intent.putStringArrayListExtra("urls", urls);
            intent.putStringArrayListExtra("uploaderIds", uploaderIds);
            intent.putStringArrayListExtra("photoIds", photoIds);
            intent.putExtra("position", position);
            intent.putExtra("albumId", albumId);
            intent.putExtra("currentUserRole", currentUserRole);
            intent.putExtra("currentUserId", auth.getCurrentUser().getUid());
            startActivity(intent);
        }, photo -> handlePhotoDelete(photo));

        rvPhotos.setLayoutManager(new GridLayoutManager(this, 3));
        rvPhotos.setAdapter(adapter);

        loadAlbumInfo();
        loadPhotos();
    }

    private void loadAlbumInfo() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("albums").document(albumId).get()
                .addOnSuccessListener(doc -> {
                    ownerId = doc.getString("ownerId");
                    Map<String, Object> roles = (Map<String, Object>) doc.get("roles");
                    if (roles != null && roles.containsKey(userId)) {
                        currentUserRole = (String) roles.get(userId);
                    }
                    if (userId.equals(ownerId)) currentUserRole = "owner";

                    // Скрываем кнопку ПОСЛЕ того как получили роль
                    if (currentUserRole.equals("viewer")) {
                        fabAddPhoto.setVisibility(android.view.View.GONE);
                    } else {
                        fabAddPhoto.setVisibility(android.view.View.VISIBLE);
                    }
                });
    }

    private void handlePhotoDelete(Photo photo) {
        String userId = auth.getCurrentUser().getUid();

        boolean canDelete = currentUserRole.equals("owner")
                || currentUserRole.equals("admin")
                || (currentUserRole.equals("editor") && photo.getUploaderId().equals(userId));

        if (!canDelete) {
            Toast.makeText(this, "У вас нет прав для удаления этого фото",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("Удалить фото?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    db.collection("albums").document(albumId)
                            .collection("photos").document(photo.getId())
                            .delete()
                            .addOnSuccessListener(v ->
                                    Toast.makeText(this, "Фото удалено", Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void loadPhotos() {
        db.collection("albums").document(albumId)
                .collection("photos")
                .orderBy("uploadedAt")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    photoList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Photo photo = doc.toObject(Photo.class);
                        photo.setId(doc.getId());
                        photoList.add(photo);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void uploadPhoto(Uri uri) {
        Toast.makeText(this, "Загружаем фото...", Toast.LENGTH_SHORT).show();
        MediaManager.get().upload(uri)
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        savePhotoToFirestore(url);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(AlbumActivity.this,
                                "Ошибка загрузки: " + error.getDescription(),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void savePhotoToFirestore(String url) {
        String userId = auth.getCurrentUser().getUid();

        Map<String, Object> photo = new HashMap<>();
        photo.put("url", url);
        photo.put("uploaderId", userId);
        photo.put("uploadedAt", com.google.firebase.Timestamp.now());

        db.collection("albums").document(albumId)
                .collection("photos").add(photo)
                .addOnSuccessListener(ref ->
                        Toast.makeText(this, "Фото добавлено!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_album, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_members) {
            Intent intent = new Intent(this, MembersActivity.class);
            intent.putExtra("albumId", albumId);
            intent.putExtra("ownerId", ownerId);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}