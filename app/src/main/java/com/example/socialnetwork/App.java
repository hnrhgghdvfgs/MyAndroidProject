package com.example.socialnetwork;

import android.app.Application;

import com.example.socialnetwork.util.AppExecutors;
import com.example.socialnetwork.util.ThemeManager;

/**
 * Точка входа приложения. Применяет сохранённую тему и прогревает пул потоков для БД.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.applySavedTheme(this);
        // Инициализация пула потоков для фоновой работы с SQLite.
        AppExecutors.get();
    }
}
