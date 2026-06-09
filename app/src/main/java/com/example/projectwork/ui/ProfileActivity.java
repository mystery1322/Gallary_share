package com.example.projectwork.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.example.projectwork.R;
import com.google.android.material.appbar.MaterialToolbar;

import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import de.hdodenhof.circleimageview.CircleImageView;


public class ProfileActivity extends AppCompatActivity {

    private CircleImageView ivAvatar;
    private TextInputEditText etNickname;
    private TextView tvEmail;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String currentAvatarUrl = "";

    private ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadAvatar(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ivAvatar = findViewById(R.id.ivAvatar);
        etNickname = findViewById(R.id.etNickname);
        tvEmail = findViewById(R.id.tvEmail);
        MaterialButton btnSave = findViewById(R.id.btnSave);
        MaterialButton btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);

        tvEmail.setText(auth.getCurrentUser().getEmail());

        loadProfile();

        btnChangeAvatar.setOnClickListener(v ->
                pickImageLauncher.launch("image/*"));

        btnSave.setOnClickListener(v -> saveProfile());

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void loadProfile() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nickname = doc.getString("nickname");
                        String avatarUrl = doc.getString("avatarUrl");
                        etNickname.setText(nickname);
                        currentAvatarUrl = avatarUrl != null ? avatarUrl : "";
                        if (!currentAvatarUrl.isEmpty()) {
                            Glide.with(this).load(currentAvatarUrl)
                                    .circleCrop().into(ivAvatar);
                        }
                    }
                });
    }

    private void saveProfile() {
        String userId = auth.getCurrentUser().getUid();
        String nickname = etNickname.getText().toString().trim();

        if (nickname.isEmpty()) {
            Toast.makeText(this, "Введи ник", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> user = new HashMap<>();
        user.put("nickname", nickname);
        user.put("email", auth.getCurrentUser().getEmail());
        user.put("avatarUrl", currentAvatarUrl);

        db.collection("users").document(userId).set(user)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "Профиль сохранён!", Toast.LENGTH_SHORT).show()
                );
    }

    private void uploadAvatar(Uri uri) {
        Toast.makeText(this, "Загружаем аватар...", Toast.LENGTH_SHORT).show();
        MediaManager.get().upload(uri)
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        currentAvatarUrl = (String) resultData.get("secure_url");
                        runOnUiThread(() ->
                                Glide.with(ProfileActivity.this)
                                        .load(currentAvatarUrl)
                                        .circleCrop()
                                        .into(ivAvatar)
                        );
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() ->
                                Toast.makeText(ProfileActivity.this,
                                        "Ошибка: " + error.getDescription(),
                                        Toast.LENGTH_LONG).show()
                        );
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }
}