package com.example.socialnetwork.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.socialnetwork.data.model.Comment;

import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторий комментариев.
 */
public class CommentRepository {

    private static final String COMMENT_SELECT =
            "SELECT c." + DbContract.COL_ID + ", c." + DbContract.COL_POST_ID
                    + ", c." + DbContract.COL_USER_ID + ", c." + DbContract.COL_COMMENT_TEXT
                    + ", c." + DbContract.COL_CREATED_AT + ", u." + DbContract.COL_FULL_NAME
                    + ", u." + DbContract.COL_USERNAME
                    + " FROM " + DbContract.TABLE_COMMENTS + " c "
                    + "JOIN " + DbContract.TABLE_USERS + " u ON u." + DbContract.COL_ID
                    + " = c." + DbContract.COL_USER_ID + " ";

    private final AppDatabase database;

    public CommentRepository(Context context) {
        this.database = AppDatabase.get(context);
    }

    public List<Comment> getComments(long postId) {
        List<Comment> comments = new ArrayList<>();
        SQLiteDatabase db = database.getReadableDatabase();
        String sql = COMMENT_SELECT + "WHERE c." + DbContract.COL_POST_ID + "=? ORDER BY c."
                + DbContract.COL_CREATED_AT + " ASC";
        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(postId)})) {
            while (cursor.moveToNext()) {
                comments.add(CursorMapper.comment(cursor));
            }
        }
        return comments;
    }

    public Comment addComment(long postId, long userId, String text) {
        SQLiteDatabase db = database.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DbContract.COL_POST_ID, postId);
        values.put(DbContract.COL_USER_ID, userId);
        values.put(DbContract.COL_COMMENT_TEXT, text.trim());
        values.put(DbContract.COL_CREATED_AT, System.currentTimeMillis());
        long id = db.insert(DbContract.TABLE_COMMENTS, null, values);
        if (id <= 0) {
            return null;
        }
        try (Cursor cursor = db.rawQuery(COMMENT_SELECT + "WHERE c." + DbContract.COL_ID + "=?",
                new String[]{String.valueOf(id)})) {
            if (cursor.moveToFirst()) {
                return CursorMapper.comment(cursor);
            }
        }
        return null;
    }

    public boolean deleteComment(long commentId) {
        int rows = database.getWritableDatabase().delete(DbContract.TABLE_COMMENTS,
                DbContract.COL_ID + "=?", new String[]{String.valueOf(commentId)});
        return rows > 0;
    }
}
