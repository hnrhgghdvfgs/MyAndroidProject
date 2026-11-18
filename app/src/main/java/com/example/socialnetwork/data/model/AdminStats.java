package com.example.socialnetwork.data.model;

/**
 * Статистика для панели администратора.
 */
public final class AdminStats {

    public final int users;
    public final int posts;
    public final int comments;
    public final int likes;
    public final int blocked;

    public AdminStats(int users, int posts, int comments, int likes, int blocked) {
        this.users = users;
        this.posts = posts;
        this.comments = comments;
        this.likes = likes;
        this.blocked = blocked;
    }
}
