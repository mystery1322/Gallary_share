package com.example.projectwork.ui;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectwork.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MembersActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String albumId;
    private String ownerId;
    private List<MemberAdapter.Member> memberList = new ArrayList<>();
    private MemberAdapter adapter;
    private TextInputEditText etInviteEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_members);

        albumId = getIntent().getStringExtra("albumId");
        ownerId = getIntent().getStringExtra("ownerId");

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etInviteEmail = findViewById(R.id.etInviteEmail);
        MaterialButton btnInvite = findViewById(R.id.btnInvite);
        RecyclerView rvMembers = findViewById(R.id.rvMembers);

        boolean isOwner = auth.getCurrentUser().getUid().equals(ownerId);

        adapter = new MemberAdapter(memberList, isOwner,
                (userId, newRole) -> {
                    db.collection("albums").document(albumId)
                            .update("roles." + userId, newRole)
                            .addOnSuccessListener(v ->
                                    Toast.makeText(this, "Роль обновлена", Toast.LENGTH_SHORT).show()
                            );
                },
                userId -> {
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("Удалить участника?")
                            .setPositiveButton("Удалить", (dialog, which) -> {
                                db.collection("albums").document(albumId)
                                        .update(
                                                "memberIds", com.google.firebase.firestore.FieldValue.arrayRemove(userId),
                                                "roles." + userId, com.google.firebase.firestore.FieldValue.delete()
                                        )
                                        .addOnSuccessListener(v -> {
                                            Toast.makeText(this, "Участник удалён", Toast.LENGTH_SHORT).show();
                                            loadMembers();
                                        });
                            })
                            .setNegativeButton("Отмена", null)
                            .show();
                }
        );

        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(adapter);

        btnInvite.setOnClickListener(v -> inviteUser());

        loadMembers();
    }

    private void inviteUser() {
        String email = etInviteEmail.getText().toString().trim();
        if (email.isEmpty()) return;

        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String userId = query.getDocuments().get(0).getId();

                    // Добавляем в pendingInvites а не сразу в альбом
                    db.collection("albums").document(albumId)
                            .update("pendingInvites",
                                    com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Приглашение отправлено!", Toast.LENGTH_SHORT).show();
                                etInviteEmail.setText("");
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void loadMembers() {
        db.collection("albums").document(albumId).get()
                .addOnSuccessListener(doc -> {
                    List<String> memberIds = (List<String>) doc.get("memberIds");
                    Map<String, String> roles = (Map<String, String>) doc.get("roles");
                    if (memberIds == null) return;

                    memberList.clear();
                    for (String userId : memberIds) {
                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(userDoc -> {
                                    String nickname = userDoc.getString("nickname");
                                    String email = userDoc.getString("email");
                                    String avatarUrl = userDoc.getString("avatarUrl");
                                    String role = roles != null && roles.containsKey(userId)
                                            ? roles.get(userId) : "viewer";

                                    memberList.add(new MemberAdapter.Member(
                                            userId, nickname, email, avatarUrl, role
                                    ));
                                    adapter.notifyDataSetChanged();
                                });
                    }
                });
    }
}