package com.example.projectwork.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.projectwork.R;
import com.example.projectwork.model.Photo;
import com.google.firebase.firestore.FirebaseFirestore;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {

    public interface OnPhotoClickListener {
        void onPhotoClick(Photo photo);
    }

    public interface OnPhotoLongClickListener {
        void onPhotoLongClick(Photo photo);
    }

    private List<Photo> photos;
    private OnPhotoClickListener clickListener;
    private OnPhotoLongClickListener longClickListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public PhotoAdapter(List<Photo> photos,
                        OnPhotoClickListener clickListener,
                        OnPhotoLongClickListener longClickListener) {
        this.photos = photos;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int size = parent.getWidth() / 3;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(size, size);
        params.setMargins(4, 4, 4, 4);
        view.setLayoutParams(params);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Photo photo = photos.get(position);

        Glide.with(holder.ivPhoto.getContext())
                .load(photo.getUrl())
                .centerCrop()
                .into(holder.ivPhoto);

        // Загружаем данные автора
        if (photo.getUploaderId() != null) {
            db.collection("users").document(photo.getUploaderId()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String nickname = doc.getString("nickname");
                            String avatarUrl = doc.getString("avatarUrl");

                            holder.tvAuthorNickname.setText(nickname != null ? nickname : "");

                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                Glide.with(holder.ivAuthorAvatar.getContext())
                                        .load(avatarUrl)
                                        .circleCrop()
                                        .into(holder.ivAuthorAvatar);
                            }
                        }
                    });
        }

        holder.itemView.setOnClickListener(v -> clickListener.onPhotoClick(photo));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onPhotoLongClick(photo);
            return true;
        });
    }

    @Override
    public int getItemCount() { return photos.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        CircleImageView ivAuthorAvatar;
        TextView tvAuthorNickname;

        ViewHolder(View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
            ivAuthorAvatar = itemView.findViewById(R.id.ivAuthorAvatar);
            tvAuthorNickname = itemView.findViewById(R.id.tvAuthorNickname);
        }
    }
}