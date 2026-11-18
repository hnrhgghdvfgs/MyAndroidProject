package com.example.socialnetwork.data.model;

/**
 * Диалог в списке сообщений: собеседник, последнее сообщение и число непрочитанных.
 */
public class Conversation {

    public final long peerId;
    public final String peerName;
    public final String peerUsername;
    public final String lastMessage;
    public final long lastMessageAt;
    public final boolean lastMessageMine;
    public final int unreadCount;

    public Conversation(long peerId, String peerName, String peerUsername, String lastMessage,
                        long lastMessageAt, boolean lastMessageMine, int unreadCount) {
        this.peerId = peerId;
        this.peerName = peerName;
        this.peerUsername = peerUsername;
        this.lastMessage = lastMessage == null ? "" : lastMessage;
        this.lastMessageAt = lastMessageAt;
        this.lastMessageMine = lastMessageMine;
        this.unreadCount = unreadCount;
    }
}
