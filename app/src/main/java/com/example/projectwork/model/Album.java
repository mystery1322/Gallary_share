package com.example.projectwork.model;

import com.google.firebase.Timestamp;
import java.util.List;
import java.util.Map;

public class Album {
    private String id;
    private String title;
    private String ownerId;
    private List<String> memberIds;
    private Map<String, String> roles;
    private Timestamp createdAt;

    public Album() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public List<String> getMemberIds() { return memberIds; }
    public Map<String, String> getRoles() { return roles; }
    public Timestamp getCreatedAt() { return createdAt; }
}