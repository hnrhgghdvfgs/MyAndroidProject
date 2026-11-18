package com.example.socialnetwork.data.model;

/**
 * Комментарий к публикации.
 */
public class Comment {

    public final long id;
    public final long postId;
    public final long userId;
    public final String authorName;
    public final String authorUsername;
    public final String text;
    public final long createdAt;

    public Comment(long id, long postId, long userId, String authorName, String authorUsername,
                   String text, long createdAt) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.authorName = authorName;
        this.authorUsername = authorUsername;
        this.text = text;
        this.createdAt = createdAt;
    }
}
