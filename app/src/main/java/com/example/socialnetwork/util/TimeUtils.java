package com.example.socialnetwork.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Форматирование дат в стиле социальной сети.
 */
public final class TimeUtils {

    private TimeUtils() {
    }

    public static String relative(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < 60_000L) {
            return "только что";
        }
        if (diff < 3_600_000L) {
            return (diff / 60_000L) + " мин назад";
        }
        if (diff < 86_400_000L) {
            return (diff / 3_600_000L) + " ч назад";
        }
        if (diff < 7L * 86_400_000L) {
            return (diff / 86_400_000L) + " дн назад";
        }
        return new SimpleDateFormat("dd.MM.yyyy", new Locale("ru")).format(new Date(timestamp));
    }

    /**
     * Время последнего сообщения в списке диалогов: «12:40», «вчера», «05.11».
     */
    public static String conversationTime(long timestamp) {
        long now = System.currentTimeMillis();
        String today = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru")).format(new Date(now));
        String thatDay = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru"))
                .format(new Date(timestamp));
        if (today.equals(thatDay)) {
            return new SimpleDateFormat("HH:mm", new Locale("ru")).format(new Date(timestamp));
        }
        String yesterday = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru"))
                .format(new Date(now - 86_400_000L));
        if (yesterday.equals(thatDay)) {
            return "вчера";
        }
        return new SimpleDateFormat("dd.MM", new Locale("ru")).format(new Date(timestamp));
    }

    /**
     * Время сообщения в переписке: «12:40» для сегодняшних, иначе «05.11 12:40».
     */
    public static String messageTime(long timestamp) {
        long now = System.currentTimeMillis();
        String today = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru")).format(new Date(now));
        String thatDay = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru"))
                .format(new Date(timestamp));
        if (today.equals(thatDay)) {
            return new SimpleDateFormat("HH:mm", new Locale("ru")).format(new Date(timestamp));
        }
        return new SimpleDateFormat("dd.MM HH:mm", new Locale("ru")).format(new Date(timestamp));
    }
}
