package com.example.socialnetwork.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.socialnetwork.data.model.User;

/**
 * Сессия пользователя в SharedPreferences.
 */
public class SessionManager {

    private static final String PREFS = "circle_session";
    private static final String KEY_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_ROLE = "role";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(User user) {
        prefs.edit()
                .putLong(KEY_ID, user.id)
                .putString(KEY_USERNAME, user.username)
                .putString(KEY_FULL_NAME, user.fullName)
                .putString(KEY_ROLE, user.role)
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getLong(KEY_ID, -1L) > 0;
    }

    public long getUserId() {
        return prefs.getLong(KEY_ID, -1L);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public String getFullName() {
        return prefs.getString(KEY_FULL_NAME, "");
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, "user");
    }

    public boolean isAdmin() {
        return "admin".equals(getRole());
    }

    public void updateFullName(String fullName) {
        prefs.edit().putString(KEY_FULL_NAME, fullName).apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
