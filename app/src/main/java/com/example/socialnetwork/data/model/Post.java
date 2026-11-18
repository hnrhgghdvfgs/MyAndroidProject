package com.example.socialnetwork.data.model;

/**
 * Публикация в ленте вместе с агрегированными данными (лайки, комментарии).
 */
public class Post {

    public final long id;
    public final long userId;
    public final String authorName;
    public final String authorUsername;
    public final String title;
    public final String content;
    public final String category;
    /**
     * Путь к изображению во внутреннем хранилище приложения или {@code null}.
     */
    public final String imagePath;
    public final long createdAt;
    public int likeCount;
    public int commentCount;
    public boolean liked;

    public Post(long id, long userId, String authorName, String authorUsername, String title,
                String content, String category, String imagePath, long createdAt, int likeCount,
                int commentCount, boolean liked) {
        this.id = id;
        this.userId = userId;
        this.authorName = authorName;
        this.authorUsername = authorUsername;
        this.title = title;
        this.content = content;
        this.category = category;
        this.imagePath = imagePath == null || imagePath.isEmpty() ? null : imagePath;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.liked = liked;
    }

    public boolean hasImage() {
        return imagePath != null;
    }
}
