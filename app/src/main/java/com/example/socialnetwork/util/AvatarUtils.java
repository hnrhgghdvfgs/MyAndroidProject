package com.example.socialnetwork.util;

import java.util.Locale;

/**
 * Цвета и инициалы для аватаров.
 */
public final class AvatarUtils {

    public static final int[] PALETTE = {
            0xFF6C4CF1, 0xFFFF5C8A, 0xFF2BB3A3, 0xFFF2A03D,
            0xFF3D8BF2, 0xFF9B51E0, 0xFFE05C5C, 0xFF3AA76D
    };

    private AvatarUtils() {
    }

    public static int colorFor(String key) {
        if (key == null || key.isEmpty()) {
            return PALETTE[0];
        }
        int hash = 0;
        for (int i = 0; i < key.length(); i++) {
            hash = hash * 31 + key.charAt(i);
        }
        int index = Math.abs(hash) % PALETTE.length;
        return PALETTE[index];
    }

    public static String initialsFor(String fullName, String username) {
        String source = fullName == null ? "" : fullName.trim();
        if (source.isEmpty()) {
            source = username == null ? "" : username.trim();
        }
        if (source.isEmpty()) {
            return "?";
        }
        String[] parts = source.split("\\s+");
        StringBuilder initials = new StringBuilder();
        initials.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1 && !parts[1].isEmpty()) {
            initials.append(Character.toUpperCase(parts[1].charAt(0)));
        }
        return initials.toString().toUpperCase(Locale.getDefault());
    }
}
