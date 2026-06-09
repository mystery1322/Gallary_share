package com.example.projectwork.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "albums")
public class AlbumEntity {

    @PrimaryKey
    @NonNull
    public String id = "";
    public String title;
    public String ownerId;

    public AlbumEntity() {}

    public AlbumEntity(String id, String title, String ownerId) {
        this.id = id;
        this.title = title;
        this.ownerId = ownerId;
    }
}