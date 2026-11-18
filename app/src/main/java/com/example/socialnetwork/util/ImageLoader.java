package com.example.socialnetwork.util;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Небольшой загрузчик изображений публикаций: декодирование в фоновом потоке
 * и кэш в памяти, чтобы прокрутка ленты не подтормаживала.
 */
public final class ImageLoader {

    private static volatile ImageLoader instance;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final LruCache<String, Bitmap> cache;

    private ImageLoader() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024L);
        int cacheSize = maxMemory / 8;
        cache = new LruCache<>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public static ImageLoader get() {
        if (instance == null) {
            synchronized (ImageLoader.class) {
                if (instance == null) {
                    instance = new ImageLoader();
                }
            }
        }
        return instance;
    }

    /**
     * Показывает изображение по пути к файлу. Тег на {@link ImageView} защищает
     * от подстановки картинки в переиспользованную ячейку списка.
     */
    public void load(ImageView target, @Nullable String path) {
        target.setTag(path);
        if (path == null || path.isEmpty()) {
            target.setImageDrawable(null);
            return;
        }
        Bitmap cached = cache.get(path);
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }
        target.setImageDrawable(null);
        executor.execute(() -> {
            Bitmap bitmap = ImageStorage.decodeFile(path);
            if (bitmap != null) {
                cache.put(path, bitmap);
            }
            mainHandler.post(() -> {
                Object tag = target.getTag();
                if (bitmap != null && path.equals(tag)) {
                    target.setImageBitmap(bitmap);
                }
            });
        });
    }
}
