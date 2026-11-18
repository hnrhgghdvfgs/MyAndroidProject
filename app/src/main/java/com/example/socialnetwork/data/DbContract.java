package com.example.socialnetwork.data;

import android.provider.BaseColumns;

/**
 * Контракт базы данных: имена таблиц и колонок в одном месте.
 */
public final class DbContract {

    private DbContract() {
    }

    public static final String DB_NAME = "social_network.db";
    public static final int DB_VERSION = 2;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_POSTS = "posts";
    public static final String TABLE_COMMENTS = "comments";
    public static final String TABLE_LIKES = "likes";
    public static final String TABLE_SUBSCRIPTIONS = "subscriptions";
    public static final String TABLE_MESSAGES = "messages";

    public static final String COL_ID = BaseColumns._ID;
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD_HASH = "password_hash";
    public static final String COL_FULL_NAME = "full_name";
    public static final String COL_BIO = "bio";
    public static final String COL_ROLE = "role";
    public static final String COL_IS_BLOCKED = "is_blocked";
    public static final String COL_CREATED_AT = "created_at";

    public static final String COL_USER_ID = "user_id";
    public static final String COL_POST_ID = "post_id";
    public static final String COL_TITLE = "title";
    public static final String COL_CONTENT = "content";
    public static final String COL_CATEGORY = "category";
    public static final String COL_IMAGE_PATH = "image_path";

    public static final String COL_COMMENT_TEXT = "text";

    public static final String COL_FOLLOWER_ID = "follower_id";
    public static final String COL_FOLLOWING_ID = "following_id";

    public static final String COL_SENDER_ID = "sender_id";
    public static final String COL_RECIPIENT_ID = "recipient_id";
    public static final String COL_MESSAGE_TEXT = "text";
    public static final String COL_IS_READ = "is_read";

    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";
}
