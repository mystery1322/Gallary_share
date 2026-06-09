package com.example.projectwork.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.projectwork.R;
import com.example.projectwork.model.Album;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ALBUM = 0;
    private static final int TYPE_ADD = 1;

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
    }

    public interface OnAlbumLongClickListener {
        void onAlbumLongClick(Album album);
    }

    public interface OnAddClickListener {
        void onAddClick();
    }

    private List<Album> albums = new ArrayList<>();
    private OnAlbumClickListener listener;
    private OnAlbumLongClickListener longClickListener;
    private OnAddClickListener addClickListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public AlbumAdapter(OnAlbumClickListener listener,
                        OnAlbumLongClickListener longClickListener,
                        OnAddClickListener addClickListener) {
        this.listener = listener;
        this.longClickListener = longClickListener;
        this.addClickListener = addClickListener;
    }

    // Метод для обновления списка через DiffUtil
    public void submitList(List<Album> newAlbums) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return albums.size();
            }

            @Override
            public int getNewListSize() {
                return newAlbums.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                // Сравниваем по ID — один и тот же альбом?
                return albums.get(oldPos).getId().equals(newAlbums.get(newPos).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                // Сравниваем содержимое — изменилось ли название?
                return albums.get(oldPos).getTitle().equals(newAlbums.get(newPos).getTitle());
            }
        });

        albums = new ArrayList<>(newAlbums);
        result.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemViewType(int position) {
        return position == albums.size() ? TYPE_ADD : TYPE_ALBUM;
    }

    @Override
    public int getItemCount() {
        return albums.size() + 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_add_album, parent, false);
            return new AddViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_album, parent, false);
            return new AlbumViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AddViewHolder) {
            holder.itemView.setOnClickListener(v -> addClickListener.onAddClick());
        } else {
            AlbumViewHolder albumHolder = (AlbumViewHolder) holder;
            Album album = albums.get(position);
            albumHolder.tvTitle.setText(album.getTitle());
            albumHolder.ivCover.setImageResource(android.R.drawable.ic_menu_gallery);

            db.collection("albums").document(album.getId())
                    .collection("photos")
                    .orderBy("uploadedAt", Query.Direction.ASCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (!album.getId().equals(albumHolder.currentAlbumId)) return;
                        if (!snapshots.isEmpty()) {
                            QueryDocumentSnapshot doc =
                                    (QueryDocumentSnapshot) snapshots.getDocuments().get(0);
                            String url = doc.getString("url");
                            if (url != null && !url.isEmpty()) {
                                Glide.with(albumHolder.ivCover.getContext())
                                        .load(url)
                                        .centerCrop()
                                        .into(albumHolder.ivCover);
                            }
                        }
                    });

            albumHolder.currentAlbumId = album.getId();
            albumHolder.itemView.setOnClickListener(v -> listener.onAlbumClick(album));
            albumHolder.itemView.setOnLongClickListener(v -> {
                longClickListener.onAlbumLongClick(album);
                return true;
            });
        }
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle;
        String currentAlbumId = "";

        AlbumViewHolder(View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvAlbumTitle);
        }
    }

    static class AddViewHolder extends RecyclerView.ViewHolder {
        AddViewHolder(View itemView) {
            super(itemView);
        }
    }
}