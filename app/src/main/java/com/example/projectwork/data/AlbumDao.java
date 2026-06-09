package com.example.projectwork.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.projectwork.model.AlbumEntity;

import java.util.List;

@Dao
public interface AlbumDao {

    @Query("SELECT * FROM albums")
    List<AlbumEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AlbumEntity> albums);

    @Query("DELETE FROM albums")
    void deleteAll();
}