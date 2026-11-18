package com.example.socialnetwork.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Хеширование паролей. Пароли не хранятся в открытом виде — только SHA-256 от соли и логина.
 */
public final class PasswordUtils {

    private static final String SALT = "circle_social_2026";

    private PasswordUtils() {
    }

    public static String hash(String username, String password) {
        String raw = SALT + ":" + username.toLowerCase(Locale.ROOT) + ":" + password;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 гарантированно присутствует на всех Android-устройствах.
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }
}
