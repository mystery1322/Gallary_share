package com.example.projectwork.ui;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.example.projectwork.R;
import com.example.projectwork.model.Photo;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.List;

public class PhotoViewActivity extends AppCompatActivity {

    private CircleImageView ivAuthorAvatar;
    private TextView tvAuthorNickname;
    private FirebaseFirestore db;
    private ArrayList<String> uploaderIds;
    private ArrayList<String> photoIds;
    private ArrayList<String> urls;
    private String albumId;
    private String currentUserRole;
    private String currentUserId;
    private int currentPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);

        db = FirebaseFirestore.getInstance();

        urls = getIntent().getStringArrayListExtra("urls");
        uploaderIds = getIntent().getStringArrayListExtra("uploaderIds");
        photoIds = getIntent().getStringArrayListExtra("photoIds");
        albumId = getIntent().getStringExtra("albumId");
        currentUserRole = getIntent().getStringExtra("currentUserRole");
        currentUserId = getIntent().getStringExtra("currentUserId");
        currentPosition = getIntent().getIntExtra("position", 0);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        toolbar.setNavigationOnClickListener(v -> finish());

        List<Photo> photos = new ArrayList<>();
        for (String url : urls) {
            Photo p = new Photo();
            p.setUrl(url);
            photos.add(p);
        }

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TextView tvCounter = findViewById(R.id.tvCounter);
        ivAuthorAvatar = findViewById(R.id.ivAuthorAvatar);
        tvAuthorNickname = findViewById(R.id.tvAuthorNickname);

        viewPager.setAdapter(new PhotoViewAdapter(photos));
        viewPager.setCurrentItem(currentPosition, false);

        tvCounter.setText((currentPosition + 1) + " / " + photos.size());
        loadAuthor(currentPosition);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPosition = position;
                tvCounter.setText((position + 1) + " / " + photos.size());
                loadAuthor(position);
                invalidateOptionsMenu();
            }
        });
    }

    private void loadAuthor(int position) {
        if (uploaderIds == null || position >= uploaderIds.size()) return;
        String uploaderId = uploaderIds.get(position);
        if (uploaderId == null) return;

        db.collection("users").document(uploaderId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nickname = doc.getString("nickname");
                        String avatarUrl = doc.getString("avatarUrl");
                        tvAuthorNickname.setText(nickname != null ? nickname : "");
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this).load(avatarUrl).circleCrop().into(ivAuthorAvatar);
                        }
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photo, menu);

        // Показываем удаление только если есть права
        MenuItem deleteItem = menu.findItem(R.id.action_delete);
        if (uploaderIds != null && currentPosition < uploaderIds.size()) {
            String uploaderId = uploaderIds.get(currentPosition);
            boolean canDelete = currentUserRole.equals("owner")
                    || currentUserRole.equals("admin")
                    || (currentUserRole.equals("editor") && uploaderId.equals(currentUserId));
            deleteItem.setVisible(canDelete);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_download) {
            downloadPhoto();
            return true;
        } else if (item.getItemId() == R.id.action_delete) {
            deletePhoto();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void downloadPhoto() {
        String url = urls.get(currentPosition);
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_PICTURES, "PhotoAlbum_" + System.currentTimeMillis() + ".jpg");
        request.setTitle("Скачивание фото");
        dm.enqueue(request);
        Toast.makeText(this, "Скачивание началось...", Toast.LENGTH_SHORT).show();
    }

    private void deletePhoto() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Удалить фото?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    String photoId = photoIds.get(currentPosition);
                    db.collection("albums").document(albumId)
                            .collection("photos").document(photoId)
                            .delete()
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Фото удалено", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}