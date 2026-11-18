package com.example.socialnetwork.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Управление светлой и тёмной темой. Выбор пользователя сохраняется в SharedPreferences.
 */
public final class ThemeManager {

    private static final String PREFS = "circle_settings";
    private static final String KEY_MODE = "theme_mode";

    private ThemeManager() {
    }

    public static void applySavedTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode(getSavedMode(context));
    }

    public static int getSavedMode(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static boolean isDarkSelected(Context context) {
        return getSavedMode(context) == AppCompatDelegate.MODE_NIGHT_YES;
    }

    public static void setDark(Context context, boolean dark) {
        int mode = dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_MODE, mode)
                .apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
