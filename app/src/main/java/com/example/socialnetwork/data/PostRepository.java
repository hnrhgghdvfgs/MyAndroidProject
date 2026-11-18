package com.example.socialnetwork.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.socialnetwork.data.model.LikeResult;
import com.example.socialnetwork.data.model.Post;
import com.example.socialnetwork.util.ImageStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторий публикаций: лента, поиск, CRUD, лайки.
 * SQL-запросы изолированы от UI.
 */
public class PostRepository {

    /**
     * Тип ленты.
     */
    public enum FeedType {
        ALL,
        FOLLOWING,
        POPULAR
    }

    private static final String POST_SELECT =
            "SELECT p." + DbContract.COL_ID + ", p." + DbContract.COL_USER_ID
                    + ", p." + DbContract.COL_TITLE + ", p." + DbContract.COL_CONTENT
                    + ", p." + DbContract.COL_CATEGORY + ", p." + DbContract.COL_IMAGE_PATH
                    + ", p." + DbContract.COL_CREATED_AT
                    + ", u." + DbContract.COL_FULL_NAME + ", u." + DbContract.COL_USERNAME
                    + ", (SELECT COUNT(*) FROM " + DbContract.TABLE_LIKES
                    + " l WHERE l." + DbContract.COL_POST_ID + " = p." + DbContract.COL_ID
                    + ") AS like_count"
                    + ", (SELECT COUNT(*) FROM " + DbContract.TABLE_COMMENTS
                    + " c WHERE c." + DbContract.COL_POST_ID + " = p." + DbContract.COL_ID
                    + ") AS comment_count"
                    + ", EXISTS(SELECT 1 FROM " + DbContract.TABLE_LIKES
                    + " l2 WHERE l2." + DbContract.COL_POST_ID + " = p." + DbContract.COL_ID
                    + " AND l2." + DbContract.COL_USER_ID + " = ?) AS is_liked "
                    + "FROM " + DbContract.TABLE_POSTS + " p "
                    + "JOIN " + DbContract.TABLE_USERS + " u ON u." + DbContract.COL_ID
                    + " = p." + DbContract.COL_USER_ID + " ";

    private final AppDatabase database;

    public PostRepository(Context context) {
        this.database = AppDatabase.get(context);
    }

    public List<Post> getFeed(FeedType type, long viewerId) {
        String where;
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(viewerId));

        switch (type) {
            case FOLLOWING:
                where = "WHERE u." + DbContract.COL_IS_BLOCKED + "=0 AND (p."
                        + DbContract.COL_USER_ID + "=? OR p." + DbContract.COL_USER_ID
                        + " IN (SELECT " + DbContract.COL_FOLLOWING_ID + " FROM "
                        + DbContract.TABLE_SUBSCRIPTIONS + " WHERE "
                        + DbContract.COL_FOLLOWER_ID + "=?)) ";
                args.add(String.valueOf(viewerId));
                args.add(String.valueOf(viewerId));
                break;
            case POPULAR:
                where = "WHERE u." + DbContract.COL_IS_BLOCKED + "=0 ";
                break;
            case ALL:
            default:
                where = "WHERE u." + DbContract.COL_IS_BLOCKED + "=0 ";
                break;
        }

        String order = type == FeedType.POPULAR
                ? "ORDER BY like_count DESC, p." + DbContract.COL_CREATED_AT + " DESC"
                : "ORDER BY p." + DbContract.COL_CREATED_AT + " DESC";

        return queryPosts(POST_SELECT + where + order, args.toArray(new String[0]));
    }

    public Post getPost(long postId, long viewerId) {
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(viewerId));
        args.add(String.valueOf(postId));
        List<Post> posts = queryPosts(POST_SELECT + "WHERE p." + DbContract.COL_ID + "=? AND u."
                + DbContract.COL_IS_BLOCKED + "=0", args.toArray(new String[0]));
        return posts.isEmpty() ? null : posts.get(0);
    }

    public List<Post> getUserPosts(long userId, long viewerId) {
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(viewerId));
        args.add(String.valueOf(userId));
        return queryPosts(POST_SELECT + "WHERE p." + DbContract.COL_USER_ID + "=? ORDER BY p."
                + DbContract.COL_CREATED_AT + " DESC", args.toArray(new String[0]));
    }

    public List<Post> search(String query, long viewerId) {
        String like = "%" + query.trim() + "%";
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(viewerId));
        args.add(like);
        args.add(like);
        args.add(like);
        args.add(like);
        String sql = POST_SELECT + "WHERE u." + DbContract.COL_IS_BLOCKED + "=0 AND (p."
                + DbContract.COL_TITLE + " LIKE ? OR p." + DbContract.COL_CONTENT
                + " LIKE ? OR p." + DbContract.COL_CATEGORY + " LIKE ? OR u."
                + DbContract.COL_FULL_NAME + " LIKE ?) ORDER BY p."
                + DbContract.COL_CREATED_AT + " DESC LIMIT 100";
        return queryPosts(sql, args.toArray(new String[0]));
    }

    public long createPost(long userId, String title, String content, String category,
                           String imagePath) {
        ContentValues values = new ContentValues();
        values.put(DbContract.COL_USER_ID, userId);
        values.put(DbContract.COL_TITLE, title.trim());
        values.put(DbContract.COL_CONTENT, content.trim());
        values.put(DbContract.COL_CATEGORY, category);
        if (imagePath == null || imagePath.isEmpty()) {
            values.putNull(DbContract.COL_IMAGE_PATH);
        } else {
            values.put(DbContract.COL_IMAGE_PATH, imagePath);
        }
        values.put(DbContract.COL_CREATED_AT, System.currentTimeMillis());
        return database.getWritableDatabase()
                .insert(DbContract.TABLE_POSTS, null, values);
    }

    public long createPost(long userId, String title, String content, String category) {
        return createPost(userId, title, content, category, null);
    }

    public boolean deletePost(long postId) {
        SQLiteDatabase db = database.getWritableDatabase();
        String imagePath = null;
        try (Cursor cursor = db.query(DbContract.TABLE_POSTS,
                new String[]{DbContract.COL_IMAGE_PATH},
                DbContract.COL_ID + "=?", new String[]{String.valueOf(postId)},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                imagePath = cursor.getString(0);
            }
        }
        int rows = db.delete(DbContract.TABLE_POSTS,
                DbContract.COL_ID + "=?", new String[]{String.valueOf(postId)});
        if (rows > 0) {
            // Файл изображения больше не нужен — освобождаем место.
            ImageStorage.delete(imagePath);
        }
        return rows > 0;
    }

    /**
     * Переключает лайк пользователя и возвращает актуальное состояние и количество.
     */
    public LikeResult toggleLike(long postId, long userId) {
        SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        try {
            long likeId = -1L;
            try (Cursor cursor = db.query(DbContract.TABLE_LIKES, new String[]{DbContract.COL_ID},
                    DbContract.COL_POST_ID + "=? AND " + DbContract.COL_USER_ID + "=?",
                    new String[]{String.valueOf(postId), String.valueOf(userId)},
                    null, null, null)) {
                if (cursor.moveToFirst()) {
                    likeId = cursor.getLong(0);
                }
            }
            boolean liked;
            if (likeId >= 0) {
                db.delete(DbContract.TABLE_LIKES, DbContract.COL_ID + "=?",
                        new String[]{String.valueOf(likeId)});
                liked = false;
            } else {
                ContentValues values = new ContentValues();
                values.put(DbContract.COL_POST_ID, postId);
                values.put(DbContract.COL_USER_ID, userId);
                values.put(DbContract.COL_CREATED_AT, System.currentTimeMillis());
                db.insertWithOnConflict(DbContract.TABLE_LIKES, null, values,
                        SQLiteDatabase.CONFLICT_IGNORE);
                liked = true;
            }
            int count = 0;
            try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DbContract.TABLE_LIKES
                            + " WHERE " + DbContract.COL_POST_ID + "=?",
                    new String[]{String.valueOf(postId)})) {
                if (cursor.moveToFirst()) {
                    count = cursor.getInt(0);
                }
            }
            db.setTransactionSuccessful();
            return new LikeResult(liked, count);
        } finally {
            db.endTransaction();
        }
    }

    private List<Post> queryPosts(String sql, String[] args) {
        List<Post> posts = new ArrayList<>();
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                posts.add(CursorMapper.post(cursor));
            }
        }
        return posts;
    }
}
