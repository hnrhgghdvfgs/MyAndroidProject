package com.example.socialnetwork.data.model;

/**
 * Результат переключения лайка.
 */
public final class LikeResult {

    public final boolean liked;
    public final int count;

    public LikeResult(boolean liked, int count) {
        this.liked = liked;
        this.count = count;
    }
}
