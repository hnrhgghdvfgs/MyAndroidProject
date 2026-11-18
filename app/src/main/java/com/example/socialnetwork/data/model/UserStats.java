package com.example.socialnetwork.data.model;

/**
 * Статистика профиля пользователя и состояние подписки.
 */
public final class UserStats {

    public final int posts;
    public final int followers;
    public final int likes;
    public final boolean following;

    public UserStats(int posts, int followers, int likes, boolean following) {
        this.posts = posts;
        this.followers = followers;
        this.likes = likes;
        this.following = following;
    }
}
