package com.example.socialnetwork.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.socialnetwork.data.model.Conversation;
import com.example.socialnetwork.data.model.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторий личных сообщений: диалоги, переписка, отправка и прочтение.
 * Все публичные методы синхронные — вызывайте их из фонового потока.
 */
public class MessageRepository {

    private final AppDatabase database;

    public MessageRepository(Context context) {
        this.database = AppDatabase.get(context);
    }

    /**
     * Список диалогов пользователя: собеседник, последнее сообщение и число непрочитанных.
     * Собеседники сортируются по времени последнего сообщения.
     */
    public List<Conversation> getConversations(long userId) {
        String lastMessage =
                "(SELECT m." + DbContract.COL_MESSAGE_TEXT + " FROM " + DbContract.TABLE_MESSAGES
                        + " m WHERE (m." + DbContract.COL_SENDER_ID + " = u." + DbContract.COL_ID
                        + " AND m." + DbContract.COL_RECIPIENT_ID + " = ?) OR (m."
                        + DbContract.COL_SENDER_ID + " = ? AND m." + DbContract.COL_RECIPIENT_ID
                        + " = u." + DbContract.COL_ID + ") ORDER BY m." + DbContract.COL_CREATED_AT
                        + " DESC, m." + DbContract.COL_ID + " DESC LIMIT 1)";

        String lastAt =
                "(SELECT m." + DbContract.COL_CREATED_AT + " FROM " + DbContract.TABLE_MESSAGES
                        + " m WHERE (m." + DbContract.COL_SENDER_ID + " = u." + DbContract.COL_ID
                        + " AND m." + DbContract.COL_RECIPIENT_ID + " = ?) OR (m."
                        + DbContract.COL_SENDER_ID + " = ? AND m." + DbContract.COL_RECIPIENT_ID
                        + " = u." + DbContract.COL_ID + ") ORDER BY m." + DbContract.COL_CREATED_AT
                        + " DESC, m." + DbContract.COL_ID + " DESC LIMIT 1)";

        String lastSender =
                "(SELECT m." + DbContract.COL_SENDER_ID + " FROM " + DbContract.TABLE_MESSAGES
                        + " m WHERE (m." + DbContract.COL_SENDER_ID + " = u." + DbContract.COL_ID
                        + " AND m." + DbContract.COL_RECIPIENT_ID + " = ?) OR (m."
                        + DbContract.COL_SENDER_ID + " = ? AND m." + DbContract.COL_RECIPIENT_ID
                        + " = u." + DbContract.COL_ID + ") ORDER BY m." + DbContract.COL_CREATED_AT
                        + " DESC, m." + DbContract.COL_ID + " DESC LIMIT 1)";

        String unread =
                "(SELECT COUNT(*) FROM " + DbContract.TABLE_MESSAGES + " m WHERE m."
                        + DbContract.COL_SENDER_ID + " = u." + DbContract.COL_ID + " AND m."
                        + DbContract.COL_RECIPIENT_ID + " = ? AND m." + DbContract.COL_IS_READ
                        + " = 0)";

        String exists =
                "EXISTS(SELECT 1 FROM " + DbContract.TABLE_MESSAGES + " m WHERE (m."
                        + DbContract.COL_SENDER_ID + " = u." + DbContract.COL_ID + " AND m."
                        + DbContract.COL_RECIPIENT_ID + " = ?) OR (m." + DbContract.COL_SENDER_ID
                        + " = ? AND m." + DbContract.COL_RECIPIENT_ID + " = u."
                        + DbContract.COL_ID + "))";

        String sql = "SELECT u." + DbContract.COL_ID + ", u." + DbContract.COL_FULL_NAME
                + ", u." + DbContract.COL_USERNAME + ", " + lastMessage + " AS last_text, "
                + lastAt + " AS last_at, " + lastSender + " AS last_sender, " + unread
                + " AS unread_count FROM " + DbContract.TABLE_USERS + " u WHERE u."
                + DbContract.COL_ID + " <> ? AND u." + DbContract.COL_IS_BLOCKED + " = 0 AND "
                + exists + " ORDER BY last_at DESC";

        String id = String.valueOf(userId);
        String[] args = new String[]{
                id, id, id, id, id, id, id, id, id, id
        };

        List<Conversation> conversations = new ArrayList<>();
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                long peerId = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.COL_ID));
                conversations.add(new Conversation(
                        peerId,
                        cursor.getString(cursor.getColumnIndexOrThrow(DbContract.COL_FULL_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DbContract.COL_USERNAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow("last_text")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("last_at")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("last_sender")) == userId,
                        cursor.getInt(cursor.getColumnIndexOrThrow("unread_count"))));
            }
        }
        return conversations;
    }

    /**
     * Переписка между двумя пользователями в хронологическом порядке.
     */
    public List<Message> getMessages(long userId, long peerId) {
        String sql = "SELECT * FROM " + DbContract.TABLE_MESSAGES + " WHERE ("
                + DbContract.COL_SENDER_ID + " = ? AND " + DbContract.COL_RECIPIENT_ID + " = ?) OR ("
                + DbContract.COL_SENDER_ID + " = ? AND " + DbContract.COL_RECIPIENT_ID + " = ?) "
                + "ORDER BY " + DbContract.COL_CREATED_AT + " ASC, " + DbContract.COL_ID + " ASC";
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(sql, new String[]{
                String.valueOf(userId), String.valueOf(peerId),
                String.valueOf(peerId), String.valueOf(userId)})) {
            while (cursor.moveToNext()) {
                messages.add(CursorMapper.message(cursor));
            }
        }
        return messages;
    }

    /**
     * Отправляет сообщение. Возвращает сохранённое сообщение или {@code null} при ошибке.
     */
    public Message send(long senderId, long recipientId, String text) {
        if (senderId == recipientId || text == null || text.trim().isEmpty()) {
            return null;
        }
        ContentValues values = new ContentValues();
        values.put(DbContract.COL_SENDER_ID, senderId);
        values.put(DbContract.COL_RECIPIENT_ID, recipientId);
        values.put(DbContract.COL_MESSAGE_TEXT, text.trim());
        values.put(DbContract.COL_IS_READ, 0);
        values.put(DbContract.COL_CREATED_AT, System.currentTimeMillis());
        long id = database.getWritableDatabase()
                .insert(DbContract.TABLE_MESSAGES, null, values);
        if (id <= 0) {
            return null;
        }
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor cursor = db.query(DbContract.TABLE_MESSAGES, null,
                DbContract.COL_ID + "=?", new String[]{String.valueOf(id)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return CursorMapper.message(cursor);
            }
        }
        return null;
    }

    /**
     * Помечает все входящие сообщения диалога как прочитанные.
     */
    public void markConversationRead(long userId, long peerId) {
        ContentValues values = new ContentValues();
        values.put(DbContract.COL_IS_READ, 1);
        database.getWritableDatabase().update(DbContract.TABLE_MESSAGES, values,
                DbContract.COL_SENDER_ID + "=? AND " + DbContract.COL_RECIPIENT_ID + "=? AND "
                        + DbContract.COL_IS_READ + "=0",
                new String[]{String.valueOf(peerId), String.valueOf(userId)});
    }

    /**
     * Общее число непрочитанных входящих сообщений — для значка на вкладке «Чаты».
     * Сообщения от заблокированных пользователей не учитываются.
     */
    public int unreadCount(long userId) {
        SQLiteDatabase db = database.getReadableDatabase();
        String sql = "SELECT COUNT(*) FROM " + DbContract.TABLE_MESSAGES + " m JOIN "
                + DbContract.TABLE_USERS + " u ON u." + DbContract.COL_ID + " = m."
                + DbContract.COL_SENDER_ID + " WHERE m." + DbContract.COL_RECIPIENT_ID
                + "=? AND m." + DbContract.COL_IS_READ + "=0 AND u."
                + DbContract.COL_IS_BLOCKED + "=0";
        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId)})) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }

    /**
     * Число непрочитанных сообщений от конкретного собеседника.
     */
    public int unreadFrom(long userId, long peerId) {
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DbContract.TABLE_MESSAGES
                        + " WHERE " + DbContract.COL_SENDER_ID + "=? AND "
                        + DbContract.COL_RECIPIENT_ID + "=? AND " + DbContract.COL_IS_READ + "=0",
                new String[]{String.valueOf(peerId), String.valueOf(userId)})) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }

    /**
     * Удаляет переписку с собеседником (обе стороны).
     */
    public void deleteConversation(long userId, long peerId) {
        database.getWritableDatabase().delete(DbContract.TABLE_MESSAGES,
                "(" + DbContract.COL_SENDER_ID + "=? AND " + DbContract.COL_RECIPIENT_ID + "=?) OR ("
                        + DbContract.COL_SENDER_ID + "=? AND " + DbContract.COL_RECIPIENT_ID + "=?)",
                new String[]{String.valueOf(userId), String.valueOf(peerId),
                        String.valueOf(peerId), String.valueOf(userId)});
    }
}
