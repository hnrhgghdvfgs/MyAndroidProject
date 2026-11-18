package com.example.socialnetwork.data.model;

/**
 * Результат входа или регистрации.
 */
public final class AuthResult {

    public final boolean success;
    public final String message;
    public final User user;

    private AuthResult(boolean success, String message, User user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    public static AuthResult success(User user) {
        return new AuthResult(true, null, user);
    }

    public static AuthResult failure(String message) {
        return new AuthResult(false, message, null);
    }
}
