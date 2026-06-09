package com.example.projectwork.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectwork.R;
import com.google.firebase.auth.FirebaseAuth;

public class AlbumsFragment extends Fragment {

    private AlbumAdapter adapter;
    private AlbumsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_albums, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Получаем ViewModel
        viewModel = new ViewModelProvider(this).get(AlbumsViewModel.class);

        RecyclerView rvAlbums = view.findViewById(R.id.rvAlbums);

        adapter = new AlbumAdapter(album -> {
            Intent intent = new Intent(requireContext(), AlbumActivity.class);
            intent.putExtra("albumId", album.getId());
            intent.putExtra("albumTitle", album.getTitle());
            startActivity(intent);
        }, album -> handleAlbumLongClick(album),
                () -> showCreateAlbumDialog());

        rvAlbums.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvAlbums.setAdapter(adapter);

        // Наблюдаем за данными — вот и есть MVVM!
        viewModel.getAlbums().observe(getViewLifecycleOwner(), albums -> {
            adapter.submitList(albums);
        });

        // Запускаем загрузку
        viewModel.loadAlbums();
    }

    private void handleAlbumLongClick(com.example.projectwork.model.Album album) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        boolean isOwner = userId.equals(album.getOwnerId());

        if (isOwner) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Удалить альбом «" + album.getTitle() + "»?")
                    .setMessage("Все фотографии будут удалены безвозвратно")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        viewModel.deleteAlbum(album.getId());
                        Toast.makeText(requireContext(),
                                "Альбом удалён", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        } else {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Выйти из альбома «" + album.getTitle() + "»?")
                    .setPositiveButton("Выйти", (dialog, which) -> {
                        viewModel.leaveAlbum(album.getId());
                        Toast.makeText(requireContext(),
                                "Вы вышли из альбома", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        }
    }

    private void showCreateAlbumDialog() {
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Новый альбом");

        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Название альбома");
        input.setPadding(48, 24, 48, 24);
        builder.setView(input);

        builder.setPositiveButton("Создать", (dialog, which) -> {
            String title = input.getText().toString().trim();
            if (!title.isEmpty()) {
                viewModel.createAlbum(title);
                Toast.makeText(requireContext(),
                        "Альбом создан!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }
}