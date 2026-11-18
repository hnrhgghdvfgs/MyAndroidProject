package com.example.socialnetwork.data.model;

/**
 * Пользователь социальной сети.
 */
public class User {

    public final long id;
    public final String username;
    public final String fullName;
    public final String bio;
    public final String role;
    public final boolean blocked;
    public final long createdAt;

    public User(long id, String username, String fullName, String bio, String role,
                boolean blocked, long createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.bio = bio == null ? "" : bio;
        this.role = role;
        this.blocked = blocked;
        this.createdAt = createdAt;
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }
}
