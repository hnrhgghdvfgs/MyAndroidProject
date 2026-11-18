package com.example.socialnetwork.data;

/**
 * Колбэк для асинхронных операций с базой данных.
 */
public interface DataCallback<T> {

    void onSuccess(T data);

    default void onError(Exception e) {
        // По умолчанию ошибка игнорируется: конкретный экран может её обработать.
    }
}
