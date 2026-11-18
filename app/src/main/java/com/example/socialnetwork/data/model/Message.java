package com.example.socialnetwork.data.model;

/**
 * Личное сообщение между двумя пользователями.
 */
public class Message {

    public final long id;
    public final long senderId;
    public final long recipientId;
    public final String text;
    public final long createdAt;
    public final boolean read;

    public Message(long id, long senderId, long recipientId, String text, long createdAt,
                   boolean read) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.text = text;
        this.createdAt = createdAt;
        this.read = read;
    }

    public boolean isMine(long viewerId) {
        return senderId == viewerId;
    }
}
