package com.example.projectwork.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectwork.R;
import com.example.projectwork.data.AppDatabase;
import com.example.projectwork.model.Album;
import com.example.projectwork.model.AlbumEntity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import com.example.projectwork.ui.InviteDialogHelper;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvAlbums;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Album> albumList = new ArrayList<>();
    private AlbumAdapter adapter;

    private AppDatabase localDb;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Убираем отступ под BottomNavigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            v.setPadding(0, 0, 0, 0);
            return insets;
        });

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        NavigationUI.setupWithNavController(bottomNav, navController);

        // Проверяем приглашения в реальном времени
        String userId = auth.getCurrentUser().getUid();
        db.collection("albums")
                .whereArrayContains("pendingInvites", userId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null || snapshots.isEmpty()) return;
                    InviteDialogHelper.checkAndShowInvites(this);
                });
    }

    private void loadAlbums() {
        String userId = auth.getCurrentUser().getUid();

// Сначала грузим из Room
        new Thread(() -> {
            List<AlbumEntity> cached = localDb.albumDao().getAll();
            if (!cached.isEmpty()) {
                runOnUiThread(() -> {
                    List<Album> cachedAlbums = new ArrayList<>();
                    for (AlbumEntity entity : cached) {
                        Album album = new Album();
                        album.setId(entity.id);
                        album.setTitle(entity.title);
                        album.setOwnerId(entity.ownerId);
                        cachedAlbums.add(album);
                    }
                    adapter.submitList(cachedAlbums);
                });
            }
        }).start();

// Потом из Firebase
        db.collection("albums")
                .whereArrayContains("memberIds", userId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    List<Album> newAlbums = new ArrayList<>();
                    List<AlbumEntity> entities = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Album album = doc.toObject(Album.class);
                        album.setId(doc.getId());
                        newAlbums.add(album);
                        entities.add(new AlbumEntity(
                                doc.getId(),
                                album.getTitle(),
                                album.getOwnerId()
                        ));
                    }

                    newAlbums.sort((a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    });

                    adapter.submitList(newAlbums);

                    List<AlbumEntity> finalEntities = entities;
                    new Thread(() -> {
                        localDb.albumDao().deleteAll();
                        localDb.albumDao().insertAll(finalEntities);
                    }).start();
                });
    }

    private void handleAlbumLongClick(Album album) {
        String userId = auth.getCurrentUser().getUid();
        boolean isOwner = userId.equals(album.getOwnerId());

        if (isOwner) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Удалить альбом «" + album.getTitle() + "»?")
                    .setMessage("Все фотографии будут удалены безвозвратно")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        db.collection("albums").document(album.getId())
                                .delete()
                                .addOnSuccessListener(v ->
                                        Toast.makeText(this, "Альбом удалён", Toast.LENGTH_SHORT).show()
                                );
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        } else {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Выйти из альбома «" + album.getTitle() + "»?")
                    .setPositiveButton("Выйти", (dialog, which) -> {
                        db.collection("albums").document(album.getId())
                                .update(
                                        "memberIds", com.google.firebase.firestore.FieldValue.arrayRemove(userId),
                                        "roles." + userId, com.google.firebase.firestore.FieldValue.delete()
                                )
                                .addOnSuccessListener(v ->
                                        Toast.makeText(this, "Вы вышли из альбома", Toast.LENGTH_SHORT).show()
                                );
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        }
    }

    private void showCreateAlbumDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Новый альбом");

        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Название альбома");
        input.setPadding(48, 24, 48, 24);
        builder.setView(input);

        builder.setPositiveButton("Создать", (dialog, which) -> {
            String title = input.getText().toString().trim();
            if (!title.isEmpty()) createAlbum(title);
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void createAlbum(String title) {
        String userId = auth.getCurrentUser().getUid();

        java.util.Map<String, Object> roles = new java.util.HashMap<>();
        roles.put(userId, "owner");

        java.util.Map<String, Object> album = new java.util.HashMap<>();
        album.put("title", title);
        album.put("ownerId", userId);
        album.put("memberIds", java.util.Arrays.asList(userId));
        album.put("createdAt", com.google.firebase.Timestamp.now());
        album.put("roles", roles);

        db.collection("albums").add(album)
                .addOnSuccessListener(ref ->
                        Toast.makeText(this, "Альбом создан!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }



}
