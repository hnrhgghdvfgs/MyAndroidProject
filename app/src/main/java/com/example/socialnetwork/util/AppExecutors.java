package com.example.socialnetwork.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.socialnetwork.data.DataCallback;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Единый пул фоновых потоков для операций с SQLite.
 * Результат всегда доставляется в главный поток, чтобы UI можно было обновлять безопасно.
 */
public final class AppExecutors {

    private static final String TAG = "AppExecutors";
    private static volatile AppExecutors instance;

    private final ExecutorService diskIO = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AppExecutors() {
    }

    public static AppExecutors get() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }
        return instance;
    }

    public void io(Runnable task) {
        diskIO.execute(task);
    }

    public <T> void execute(Callable<T> task, DataCallback<T> callback) {
        diskIO.execute(() -> {
            try {
                T result = task.call();
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка фоновой операции", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void main(Runnable task) {
        mainHandler.post(task);
    }
}
