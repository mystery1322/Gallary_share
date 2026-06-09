package com.example.projectwork.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.projectwork.R;
import java.util.List;

public class InviteDialogHelper {

    public static void checkAndShowInvites(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();

        // Ищем альбомы где текущий пользователь в pendingInvites
        db.collection("albums")
                .whereArrayContains("pendingInvites", userId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) return;

                    // Показываем диалог для первого приглашения
                    QueryDocumentSnapshot doc =
                            (QueryDocumentSnapshot) snapshots.getDocuments().get(0);
                    String albumId = doc.getId();
                    String albumTitle = doc.getString("title");
                    String ownerId = doc.getString("ownerId");

                    // Загружаем имя владельца
                    db.collection("users").document(ownerId).get()
                            .addOnSuccessListener(ownerDoc -> {
                                String ownerNickname = ownerDoc.getString("nickname");

                                // Загружаем обложку альбома
                                db.collection("albums").document(albumId)
                                        .collection("photos")
                                        .orderBy("uploadedAt",
                                                com.google.firebase.firestore.Query.Direction.ASCENDING)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener(photos -> {
                                            String coverUrl = "";
                                            if (!photos.isEmpty()) {
                                                coverUrl = photos.getDocuments()
                                                        .get(0).getString("url");
                                            }
                                            showDialog(context, db, userId, albumId,
                                                    albumTitle, ownerNickname, coverUrl, snapshots.size());
                                        });
                            });
                });
    }

    private static void showDialog(Context context, FirebaseFirestore db,
                                   String userId, String albumId,
                                   String albumTitle, String ownerNickname,
                                   String coverUrl, int totalInvites) {

        Dialog dialog = new Dialog(context);
        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_invite, null);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvMessage = view.findViewById(R.id.tvInviteMessage);
        TextView tvTitle = view.findViewById(R.id.tvAlbumTitle);
        ImageView ivCover = view.findViewById(R.id.ivAlbumCover);
        MaterialButton btnAccept = view.findViewById(R.id.btnAccept);
        MaterialButton btnDecline = view.findViewById(R.id.btnDecline);

        tvMessage.setText("Пользователь " + ownerNickname +
                " приглашает вас в совместный альбом!");
        tvTitle.setText(albumTitle);

        if (!coverUrl.isEmpty()) {
            Glide.with(context).load(coverUrl).centerCrop().into(ivCover);
        }

        // Принять приглашение
        btnAccept.setOnClickListener(v -> {
            db.collection("albums").document(albumId)
                    .update(
                            "memberIds",
                            com.google.firebase.firestore.FieldValue.arrayUnion(userId),
                            "roles." + userId, "viewer",
                            "pendingInvites",
                            com.google.firebase.firestore.FieldValue.arrayRemove(userId)
                    )
                    .addOnSuccessListener(unused -> {
                        android.widget.Toast.makeText(context,
                                "Вы вступили в альбом!", android.widget.Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        // Проверяем есть ли ещё приглашения
                        if (totalInvites > 1) checkAndShowInvites(context);
                    });
        });

        // Отклонить приглашение
        btnDecline.setOnClickListener(v -> {
            db.collection("albums").document(albumId)
                    .update("pendingInvites",
                            com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                    .addOnSuccessListener(unused -> {
                        android.widget.Toast.makeText(context,
                                "Приглашение отклонено", android.widget.Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        if (totalInvites > 1) checkAndShowInvites(context);
                    });
        });

        dialog.show();
    }
}