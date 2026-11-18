package com.example.socialnetwork.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.socialnetwork.data.model.AdminStats;
import com.example.socialnetwork.data.model.AuthResult;
import com.example.socialnetwork.data.model.User;
import com.example.socialnetwork.data.model.UserStats;
import com.example.socialnetwork.util.ImageStorage;
import com.example.socialnetwork.util.PasswordUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Репозиторий пользователей: регистрация, вход, профиль, подписки и администрирование.
 * Все публичные методы синхронные — вызывайте их из фонового потока.
 */
public class UserRepository {

    private final AppDatabase database;

    public UserRepository(Context context) {
        this.database = AppDatabase.get(context);
    }

    public AuthResult register(String username, String password, String fullName) {
        String login = username.trim().toLowerCase(Locale.ROOT);
        if (findByUsername(login) != null) {
            return AuthResult.failure("username_taken");
        }
        ContentValues values = new ContentValues();
        values.put(DbContract.COL_USERNAME, login);
        values.put(DbContract.COL_PASSWORD_HASH, PasswordUtils.hash(login, password));
        values.put(DbContract.COL_FULL_NAME, fullName.trim());
        values.put(DbContract.COL_BIO, "");
        values.put(DbContract.COL_ROLE, DbContract.ROLE_USER);
        values.put(DbContract.COL_IS_BLOCKED, 0);
        values.put(DbContract.COL_CREATED_AT, System.currentTimeMillis());

        SQLiteDatabase db = database.getWritableDatabase();
        long id = db.insert(DbContract.TABLE_USERS, null, values);
        if (id <= 0) {
            return AuthResult.failure("error");
        }
        return AuthResult.success(findById(id));
    }

    public AuthResult login(String username, String password) {
        String login = username.trim().toLowerCase(Locale.ROOT);
        User user = findByUsername(login);
        if (user == null) {
            return AuthResult.failure("invalid");
        }
        String storedHash = readPasswordHash(user.id);
        if (storedHash == null || !storedHash.equals(PasswordUtils.hash(login, password))) {
            return AuthResult.failure("invalid");
        }
        if (user.blocked) {
            return AuthResult.failure("blocked");
        }
        return AuthResult.success(user);
    }

    public User findById(long id) {
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor cursor = db.query(DbContract.TABLE_USERS, null,
                DbContract.COL_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                return CursorMapper.user(cursor);
            }
        }
        return null;
    }

    public User findByUsername(String username) {
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor cursor = db.query(DbContract.TABLE_USERS, null,
                DbContract.COL_USERNAME + "=?", new String[]{username},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                return CursorMapper.user(cursor);
            }
        }
        return null;
    }

    public void updateProfile(long userId, String fullName, String bio) {
        ContentValues values = new ContentValues();
        values.put(DbContract.COL_FULL_NAME, fullName.trim());
        values.put(DbContract.COL_BIO, bio.trim());
        database.getWritableDatabase().update(DbContract.TABLE_USERS, values,
                DbContract.COL_ID + "=?", new String[]{String.valueOf(userId)});
    }

    public UserStats statsFor(long userId, long viewerId) {
        SQLiteDatabase db = database.getReadableDatabase();
        int posts = count(db, "SELECT COUNT(*) FROM " + DbContract.TABLE_POSTS
                + " WHERE " + DbContract.COL_USER_ID + "=?", userId);
        int followers = count(db, "SELECT COUNT(*) FROM " + DbContract.TABLE_SUBSCRIPTIONS
                + " WHERE " + DbContract.COL_FOLLOWING_ID + "=?", userId);
        int likes = count(db, "SELECT COUNT(*) FROM " + DbContract.TABLE_LIKES
                + " WHERE " + DbContract.COL_POST_ID + " IN (SELECT " + DbContract.COL_ID
                + " FROM " + DbContract.TABLE_POSTS + " WHERE " + DbContract.COL_USER_ID + "=?)",
                userId);
        int following = count(db, "SELECT COUNT(*) FROM " + DbContract.TABLE_SUBSCRIPTIONS
                + " WHERE " + DbContract.COL_FOLLOWER_ID + "=? AND "
                + DbContract.COL_FOLLOWING_ID + "=?", viewerId, userId);
        return new UserStats(posts, followers, likes, following > 0);
    }

    public boolean toggleFollow(long followerId, long followingId) {
        SQLiteDatabase db = database.getWritableDatabase();
        int existing = count(db, "SELECT COUNT(*) FROM " + DbContract.TABLE_SUBSCRIPTIONS
                + " WHERE " + DbContract.COL_FOLLOWER_ID + "=? AND "
                + DbContract.COL_FOLLOWING_ID + "=?", followerId, followingId);
        if (existing > 0) {
            db.delete(DbContract.TABLE_SUBSCRIPTIONS,
                    DbContract.COL_FOLLOWER_ID + "=? AND " + DbContract.COL_FOLLOWING_ID + "=?",
                    new String[]{String.valueOf(followerId), String.valueOf(followingId)});
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(DbContract.COL_FOLLOWER_ID, followerId);
        values.put(DbContract.COL_FOLLOWING_ID, followingId);
        values.put(DbContract.COL_CREATED_AT, System.currentTimeMillis());
        db.insert(DbContract.TABLE_SUBSCRIPTIONS, null, values);
        return true;
    }

    public List<User> search(String query) {
        String like = "%" + query.trim() + "%";
        SQLiteDatabase db = database.getReadableDatabase();
        List<User> result = new ArrayList<>();
        try (Cursor cursor = db.query(DbContract.TABLE_USERS, null,
                DbContract.COL_USERNAME + " LIKE ? OR " + DbContract.COL_FULL_NAME + " LIKE ?",
                new String[]{like, like}, null, null,
                DbContract.COL_FULL_NAME + " COLLATE NOCASE")) {
            while (cursor.moveToNext()) {
                result.add(CursorMapper.user(cursor));
            }
        }
        return result;
    }

    public List<User> allUsers() {
        SQLiteDatabase db = database.getReadableDatabase();
        List<User> result = new ArrayList<>();
        try (Cursor cursor = db.query(DbContract.TABLE_USERS, null, null, null, null, null,
                DbContract.COL_CREATED_AT + " DESC")) {
            while (cursor.moveToNext()) {
                result.add(CursorMapper.user(cursor));
            }
        }
        return result;
    }

    public void setBlocked(long userId, boolean blocked) {
        ContentValues values = new ContentValues();
        values.put(DbContract.COL_IS_BLOCKED, blocked ? 1 : 0);
        database.getWritableDatabase().update(DbContract.TABLE_USERS, values,
                DbContract.COL_ID + "=?", new String[]{String.valueOf(userId)});
    }

    public void deleteUser(long userId) {
        SQLiteDatabase db = database.getWritableDatabase();
        // Сначала запоминаем пути к фото, чтобы удалить файлы после каскадного удаления записей.
        List<String> imagePaths = new ArrayList<>();
        try (Cursor cursor = db.query(DbContract.TABLE_POSTS,
                new String[]{DbContract.COL_IMAGE_PATH},
                DbContract.COL_USER_ID + "=?", new String[]{String.valueOf(userId)},
                null, null, null)) {
            while (cursor.moveToNext()) {
                String path = cursor.getString(0);
                if (path != null && !path.isEmpty()) {
                    imagePaths.add(path);
                }
            }
        }
        db.delete(DbContract.TABLE_USERS,
                DbContract.COL_ID + "=?", new String[]{String.valueOf(userId)});
        for (String path : imagePaths) {
            ImageStorage.delete(path);
        }
    }

    public AdminStats adminStats() {
        SQLiteDatabase db = database.getReadableDatabase();
        return new AdminStats(
                count(db, "SELECT COUNT(*) FROM " + DbContract.TABLE_USERS),
                count(db, "SELECT COUNT(*) FROM " + DbContract.TABLE_POSTS),
                count(db, "SELECT COUNT(*) FROM " + DbContract.TABLE_COMMENTS),
                count(db, "SELECT COUNT(*) FROM " + DbContract.TABLE_LIKES),
                count(db, "SELECT COUNT(*) FROM " + DbContract.TABLE_USERS
                        + " WHERE " + DbContract.COL_IS_BLOCKED + "=1"));
    }

    private String readPasswordHash(long userId) {
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor cursor = db.query(DbContract.TABLE_USERS,
                new String[]{DbContract.COL_PASSWORD_HASH},
                DbContract.COL_ID + "=?", new String[]{String.valueOf(userId)},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        }
        return null;
    }

    private int count(SQLiteDatabase db, String sql, long... args) {
        String[] selection = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            selection[i] = String.valueOf(args[i]);
        }
        try (Cursor cursor = db.rawQuery(sql, selection)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }
}
