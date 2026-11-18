package com.example.socialnetwork.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Работа с изображениями публикаций: копирование выбранного фото во внутреннее
 * хранилище приложения и декодирование с уменьшением размера.
 */
public final class ImageStorage {

    private static final String DIR = "post_images";
    private static final int MAX_SIDE = 1280;
    private static final int JPEG_QUALITY = 88;

    private ImageStorage() {
    }

    /**
     * Копирует изображение по {@code uri} в приватную папку приложения.
     *
     * @return путь к сохранённому файлу или {@code null}, если скопировать не удалось.
     */
    @Nullable
    public static String copyToInternal(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        File dir = new File(context.getFilesDir(), DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        File target = new File(dir, "post_" + System.currentTimeMillis() + ".jpg");

        Bitmap bitmap = decodeScaled(context, uri);
        if (bitmap == null) {
            return null;
        }
        try (OutputStream out = new FileOutputStream(target)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
            bitmap.recycle();
            return target.getAbsolutePath();
        } catch (Exception e) {
            bitmap.recycle();
            // Если сжать не получилось, пробуем сохранить исходный поток байтов.
            return copyRaw(context, uri, target);
        }
    }

    /**
     * Удаляет файл изображения (используется при удалении публикации).
     */
    public static void delete(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        try {
            File file = new File(path);
            if (file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        } catch (Exception ignored) {
            // Файл мог быть уже удалён — это не критично.
        }
    }

    /**
     * Декодирует изображение с ограничением по максимальной стороне.
     */
    @Nullable
    public static Bitmap decodeScaled(Context context, Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                if (in == null) {
                    return null;
                }
                BitmapFactory.decodeStream(in, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight);
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                if (in == null) {
                    return null;
                }
                return BitmapFactory.decodeStream(in, null, options);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Декодирует сохранённый файл публикации.
     */
    @Nullable
    public static Bitmap decodeFile(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight);
            return BitmapFactory.decodeFile(path, options);
        } catch (Exception e) {
            return null;
        }
    }

    private static int sampleSize(int width, int height) {
        int sample = 1;
        int max = Math.max(width, height);
        while (max / sample > MAX_SIDE) {
            sample *= 2;
        }
        return sample;
    }

    @Nullable
    private static String copyRaw(Context context, Uri uri, File target) {
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(target)) {
            if (in == null) {
                return null;
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return target.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
}
