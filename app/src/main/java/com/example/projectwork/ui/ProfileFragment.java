package com.example.projectwork.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.projectwork.R;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ivAvatar = view.findViewById(R.id.ivAvatar);
        etNickname = view.findViewById(R.id.etNickname);
        tvEmail = view.findViewById(R.id.tvEmail);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);
        MaterialButton btnChangeAvatar = view.findViewById(R.id.btnChangeAvatar);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);

        tvEmail.setText(auth.getCurrentUser().getEmail());
        loadProfile();

        btnChangeAvatar.setOnClickListener(v ->
                pickImageLauncher.launch("image/*"));

        btnSave.setOnClickListener(v -> saveProfile());

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            requireActivity().finish();
            startActivity(new android.content.Intent(
                    requireContext(), LoginActivity.class));
        });
    }

    private void loadProfile() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        etNickname.setText(doc.getString("nickname"));
                        currentAvatarUrl = doc.getString("avatarUrl") != null
                                ? doc.getString("avatarUrl") : "";
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
            Toast.makeText(requireContext(), "Введи ник", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> user = new HashMap<>();
        user.put("nickname", nickname);
        user.put("email", auth.getCurrentUser().getEmail());
        user.put("avatarUrl", currentAvatarUrl);

        db.collection("users").document(userId).set(user)
                .addOnSuccessListener(v ->
                        Toast.makeText(requireContext(),
                                "Профиль сохранён!", Toast.LENGTH_SHORT).show()
                );
    }

    private void uploadAvatar(Uri uri) {
        Toast.makeText(requireContext(),
                "Загружаем аватар...", Toast.LENGTH_SHORT).show();
        MediaManager.get().upload(uri)
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId,
                                                     long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        currentAvatarUrl = (String) resultData.get("secure_url");
                        requireActivity().runOnUiThread(() ->
                                Glide.with(ProfileFragment.this)
                                        .load(currentAvatarUrl)
                                        .circleCrop()
                                        .into(ivAvatar)
                        );
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        "Ошибка: " + error.getDescription(),
                                        Toast.LENGTH_LONG).show()
                        );
                    }

                    @Override public void onReschedule(String requestId,
                                                       ErrorInfo error) {}
                })
                .dispatch();
    }
}