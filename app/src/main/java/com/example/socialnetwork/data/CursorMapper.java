package com.example.socialnetwork.data;

import android.database.Cursor;

import com.example.socialnetwork.data.model.Comment;
import com.example.socialnetwork.data.model.Message;
import com.example.socialnetwork.data.model.Post;
import com.example.socialnetwork.data.model.User;

/**
 * Преобразование строк Cursor в модели.
 */
public final class CursorMapper {

    private CursorMapper() {
    }

    public static User user(Cursor c) {
        return new User(
                c.getLong(c.getColumnIndexOrThrow(DbContract.COL_ID)),
                c.getString(c.getColumnIndexOrThrow(DbContract.COL_USERNAME)),
                c.getString(c.getColumnIndexOrThrow(DbContract.COL_FULL_NAME)),
                c.getString(c.getColumnIndexOrThrow(DbContract.COL_BIO)),
                c.getString(c.getColumnIndexOrThrow(DbContract.COL_ROLE)),
                c.getInt(c.getColumnIndexOrThrow(DbContract.COL_IS_BLOCKED)) == 1,
                c.getLong(c.getColumnIndexOrThrow(DbContract.COL_CREATED_AT)));
    }

    public static Post post(Cursor c) {
        return new Post(
                c.getLong(c.getColumnIndexOrThrow(DbContract.COL_ID)),
                c.getLong(c.getColumnIndexOrThrow(DbContract.COL_USER_ID)),
                c.getString(c.getColumnIndexOrThrow(DbContract.COL_FULL_NAME)),
                c.getString(c.getColumnIndexOrThrow(DbContract.COL_USERNAME)),
                c.getString(c.getColumnIndexOrThrow(DbContract.COL_TITLE)),
                c.getString(c.getColumnIndexOrThrow(DbContract.COL_CONTENT)),
                c.getString(c.getColumnIndexOrThrow(DbContract.COL_CATEGORY)),
                c.getString(c.getColumnIndexOrThrow(DbContract.COL_IMAGE_PATH)),
                c.getLong(c.getColumnIndexOrThrow(DbContract.COL_CREATED_AT)),
                c.getInt(c.getColumnIndexOrThrow("like_count")),
                c.getInt(c.getColumnIndexOrThrow("comment_count")),
                c.getInt(c.getColumnIndexOrThrow("is_liked")) == 1);
    }

    public static Message message(Cursor c) {
        return new Message(
                c.getLong(c.getColumnIndexOrThrow(DbContract.COL_ID)),
                c.getLong(c.getColumnIndexOrThrow(DbContract.COL_SENDER_ID)),
                c.getLong(c.getColumnIndexOrThrow(DbContract.COL_RECIPIENT_ID)),
                c.getString(c.getColumnIndexOrThrow(DbContract.COL_MESSAGE_TEXT)),
                c.getLong(c.getColumnIndexOrThrow(DbContract.COL_CREATED_AT)),
                c.getInt(c.getColumnIndexOrThrow(DbContract.COL_IS_READ)) == 1);
    }

    public static Comment comment(Cursor c) {
        return new Comment(
                c.getLong(c.getColumnIndexOrThrow(DbContract.COL_ID)),
                c.getLong(c.getColumnIndexOrThrow(DbContract.COL_POST_ID)),
                c.getLong(c.getColumnIndexOrThrow(DbContract.COL_USER_ID)),
                c.getString(c.getColumnIndexOrThrow(DbContract.COL_FULL_NAME)),
                c.getString(c.getColumnIndexOrThrow(DbContract.COL_USERNAME)),
                c.getString(c.getColumnIndexOrThrow(DbContract.COL_COMMENT_TEXT)),
                c.getLong(c.getColumnIndexOrThrow(DbContract.COL_CREATED_AT)));
    }
}
