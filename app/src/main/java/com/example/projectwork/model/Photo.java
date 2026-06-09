package com.example.projectwork.model;

import com.google.firebase.Timestamp;

public class Photo {
    private String id;
    private String url;
    private String uploaderId;
    private Timestamp uploadedAt;

    public Photo() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getUploaderId() { return uploaderId; }
    public Timestamp getUploadedAt() { return uploadedAt; }
}