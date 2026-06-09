package com.example.projectwork.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.projectwork.model.AlbumEntity;

@Database(entities = {AlbumEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract AlbumDao albumDao();

    private static AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "photoalbum_db"
            ).build();
        }
        return instance;
    }
}