package com.example.projectwork.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.projectwork.model.Photo;

import java.util.List;

public class PhotoViewAdapter extends RecyclerView.Adapter<PhotoViewAdapter.ViewHolder> {

    private List<Photo> photos;

    public PhotoViewAdapter(List<Photo> photos) {
        this.photos = photos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return new ViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Glide.with(holder.imageView.getContext())
                .load(photos.get(position).getUrl())
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() { return photos.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }
}