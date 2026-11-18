package com.example.socialnetwork.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import com.example.socialnetwork.util.PasswordUtils;

/**
 * SQLiteOpenHelper: создание схемы, внешние ключи и демонстрационные данные.
 *
 * Схема:
 * users 1—N posts, posts 1—N comments, posts 1—N likes, users N—N users (subscriptions),
 * users 1—N messages (отправитель и получатель личных сообщений).
 */
public class AppDatabase extends SQLiteOpenHelper {

    private static volatile AppDatabase instance;

    private AppDatabase(Context context) {
        super(context.getApplicationContext(), DbContract.DB_NAME, null, DbContract.DB_VERSION);
    }

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = new AppDatabase(context);
                }
            }
        }
        return instance;
    }

    @Override
    public void onConfigure(@NonNull SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + DbContract.TABLE_USERS + " ("
                + DbContract.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.COL_USERNAME + " TEXT NOT NULL UNIQUE COLLATE NOCASE, "
                + DbContract.COL_PASSWORD_HASH + " TEXT NOT NULL, "
                + DbContract.COL_FULL_NAME + " TEXT NOT NULL, "
                + DbContract.COL_BIO + " TEXT NOT NULL DEFAULT '', "
                + DbContract.COL_ROLE + " TEXT NOT NULL DEFAULT 'user', "
                + DbContract.COL_IS_BLOCKED + " INTEGER NOT NULL DEFAULT 0, "
                + DbContract.COL_CREATED_AT + " INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE " + DbContract.TABLE_POSTS + " ("
                + DbContract.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.COL_USER_ID + " INTEGER NOT NULL, "
                + DbContract.COL_TITLE + " TEXT NOT NULL, "
                + DbContract.COL_CONTENT + " TEXT NOT NULL, "
                + DbContract.COL_CATEGORY + " TEXT NOT NULL, "
                + DbContract.COL_IMAGE_PATH + " TEXT, "
                + DbContract.COL_CREATED_AT + " INTEGER NOT NULL, "
                + "FOREIGN KEY (" + DbContract.COL_USER_ID + ") REFERENCES "
                + DbContract.TABLE_USERS + "(" + DbContract.COL_ID + ") ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE " + DbContract.TABLE_COMMENTS + " ("
                + DbContract.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.COL_POST_ID + " INTEGER NOT NULL, "
                + DbContract.COL_USER_ID + " INTEGER NOT NULL, "
                + DbContract.COL_COMMENT_TEXT + " TEXT NOT NULL, "
                + DbContract.COL_CREATED_AT + " INTEGER NOT NULL, "
                + "FOREIGN KEY (" + DbContract.COL_POST_ID + ") REFERENCES "
                + DbContract.TABLE_POSTS + "(" + DbContract.COL_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + DbContract.COL_USER_ID + ") REFERENCES "
                + DbContract.TABLE_USERS + "(" + DbContract.COL_ID + ") ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE " + DbContract.TABLE_LIKES + " ("
                + DbContract.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.COL_POST_ID + " INTEGER NOT NULL, "
                + DbContract.COL_USER_ID + " INTEGER NOT NULL, "
                + DbContract.COL_CREATED_AT + " INTEGER NOT NULL, "
                + "FOREIGN KEY (" + DbContract.COL_POST_ID + ") REFERENCES "
                + DbContract.TABLE_POSTS + "(" + DbContract.COL_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + DbContract.COL_USER_ID + ") REFERENCES "
                + DbContract.TABLE_USERS + "(" + DbContract.COL_ID + ") ON DELETE CASCADE, "
                + "UNIQUE (" + DbContract.COL_POST_ID + ", " + DbContract.COL_USER_ID + "))");

        db.execSQL("CREATE TABLE " + DbContract.TABLE_SUBSCRIPTIONS + " ("
                + DbContract.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.COL_FOLLOWER_ID + " INTEGER NOT NULL, "
                + DbContract.COL_FOLLOWING_ID + " INTEGER NOT NULL, "
                + DbContract.COL_CREATED_AT + " INTEGER NOT NULL, "
                + "FOREIGN KEY (" + DbContract.COL_FOLLOWER_ID + ") REFERENCES "
                + DbContract.TABLE_USERS + "(" + DbContract.COL_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + DbContract.COL_FOLLOWING_ID + ") REFERENCES "
                + DbContract.TABLE_USERS + "(" + DbContract.COL_ID + ") ON DELETE CASCADE, "
                + "UNIQUE (" + DbContract.COL_FOLLOWER_ID + ", " + DbContract.COL_FOLLOWING_ID + "))");

        createMessagesTable(db);

        db.execSQL("CREATE INDEX idx_posts_created ON " + DbContract.TABLE_POSTS
                + "(" + DbContract.COL_CREATED_AT + ")");
        db.execSQL("CREATE INDEX idx_posts_user ON " + DbContract.TABLE_POSTS
                + "(" + DbContract.COL_USER_ID + ")");
        db.execSQL("CREATE INDEX idx_comments_post ON " + DbContract.TABLE_COMMENTS
                + "(" + DbContract.COL_POST_ID + ")");
        db.execSQL("CREATE INDEX idx_likes_post ON " + DbContract.TABLE_LIKES
                + "(" + DbContract.COL_POST_ID + ")");
        db.execSQL("CREATE INDEX idx_subs_follower ON " + DbContract.TABLE_SUBSCRIPTIONS
                + "(" + DbContract.COL_FOLLOWER_ID + ")");
        db.execSQL("CREATE INDEX idx_subs_following ON " + DbContract.TABLE_SUBSCRIPTIONS
                + "(" + DbContract.COL_FOLLOWING_ID + ")");

        seedDemoData(db);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        // Версия 2: личные сообщения и изображения публикаций. Данные пользователей сохраняются.
        if (oldVersion < 2) {
            createMessagesTable(db);
            addColumnIfMissing(db, DbContract.TABLE_POSTS, DbContract.COL_IMAGE_PATH, "TEXT");
            seedDemoMessagesForExistingUsers(db);
        }
    }

    /**
     * После обновления схемы добавляет демонстрационные сообщения, если переписки ещё нет.
     */
    private void seedDemoMessagesForExistingUsers(SQLiteDatabase db) {
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DbContract.TABLE_MESSAGES,
                null)) {
            if (cursor.moveToFirst() && cursor.getInt(0) > 0) {
                return;
            }
        }
        Long anna = findUserId(db, "anna");
        Long max = findUserId(db, "max");
        Long lena = findUserId(db, "lena");
        if (anna == null || max == null || lena == null) {
            return;
        }
        long now = System.currentTimeMillis();
        insertMessage(db, max, anna,
                "Привет! Видел твой пост про дизайн-систему — очень круто.",
                now - 3 * 3_600_000L, 1);
        insertMessage(db, anna, max, "Спасибо! Долго собирали токены, зато теперь проще.",
                now - 2 * 3_600_000L, 1);
        insertMessage(db, max, anna, "А компоненты в Figma или сразу в коде?",
                now - 105 * 60_000L, 0);
        insertMessage(db, lena, anna, "Анна, привет! Скинь, пожалуйста, плейлист для работы 🙌",
                now - 40 * 60_000L, 0);
    }

    private Long findUserId(SQLiteDatabase db, String username) {
        try (Cursor cursor = db.query(DbContract.TABLE_USERS,
                new String[]{DbContract.COL_ID}, DbContract.COL_USERNAME + "=?",
                new String[]{username}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }
        return null;
    }

    private void addColumnIfMissing(SQLiteDatabase db, String table, String column, String type) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIndex = cursor.getColumnIndexOrThrow("name");
            while (cursor.moveToNext()) {
                if (column.equalsIgnoreCase(cursor.getString(nameIndex))) {
                    return;
                }
            }
        }
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
    }

    /**
     * Таблица личных сообщений: отправитель, получатель, текст и признак прочтения.
     */
    private void createMessagesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + DbContract.TABLE_MESSAGES + " ("
                + DbContract.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.COL_SENDER_ID + " INTEGER NOT NULL, "
                + DbContract.COL_RECIPIENT_ID + " INTEGER NOT NULL, "
                + DbContract.COL_MESSAGE_TEXT + " TEXT NOT NULL, "
                + DbContract.COL_IS_READ + " INTEGER NOT NULL DEFAULT 0, "
                + DbContract.COL_CREATED_AT + " INTEGER NOT NULL, "
                + "FOREIGN KEY (" + DbContract.COL_SENDER_ID + ") REFERENCES "
                + DbContract.TABLE_USERS + "(" + DbContract.COL_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + DbContract.COL_RECIPIENT_ID + ") REFERENCES "
                + DbContract.TABLE_USERS + "(" + DbContract.COL_ID + ") ON DELETE CASCADE)");

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_sender ON "
                + DbContract.TABLE_MESSAGES + "(" + DbContract.COL_SENDER_ID + ", "
                + DbContract.COL_RECIPIENT_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_recipient ON "
                + DbContract.TABLE_MESSAGES + "(" + DbContract.COL_RECIPIENT_ID + ", "
                + DbContract.COL_IS_READ + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_created ON "
                + DbContract.TABLE_MESSAGES + "(" + DbContract.COL_CREATED_AT + ")");
    }

    private void seedDemoData(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        long day = 86_400_000L;

        long anna = insertUser(db, "anna", "123456", "Анна Смирнова",
                "Продуктовый дизайнер. Люблю кофе, типографику и длинные прогулки.",
                DbContract.ROLE_USER, now - 40 * day, 0);
        long max = insertUser(db, "max", "123456", "Максим Волков",
                "Разработчик Android. Велосипед, музыка и чистый код.",
                DbContract.ROLE_USER, now - 35 * day, 0);
        long lena = insertUser(db, "lena", "123456", "Елена Крылова",
                "Пишу о кино и путешествиях. Всегда за хороший плейлист.",
                DbContract.ROLE_USER, now - 30 * day, 0);
        long admin = insertUser(db, "admin", "admin123", "Администратор",
                "Слежу за порядком в Круге.", DbContract.ROLE_ADMIN, now - 60 * day, 0);

        long p1 = insertPost(db, anna, "Перезапуск дизайн-системы",
                "Наконец-то показали команде новую дизайн-систему: единые токены, "
                        + "компоненты и тёмная тема. Самое сложное было договориться о названиях "
                        + "цветов — но мы справились. Делитесь, как у вас устроены дизайн-системы?",
                "Идеи", now - 25 * 60_000L);
        long p2 = insertPost(db, max, "Велопрогулка по набережной",
                "Собрались вчетвером и проехали 32 км вдоль реки. Погода идеальная, "
                        + "кофе на середине маршрута — обязательный пункт программы. "
                        + "Присоединяйтесь в следующие выходные!",
                "Спорт", now - 2 * 3_600_000L);
        long p3 = insertPost(db, lena, "Что посмотреть на выходных",
                "Посмотрела новый фильм про космос — визуально потрясающе, но сюжет "
                        + "провисает к финалу. Поставлю 7 из 10. А что вы смотрели на этой неделе?",
                "Кино", now - 5 * 3_600_000L);
        long p4 = insertPost(db, anna, "Маленькие ритуалы продуктивности",
                "Заметила, что мне помогает не тайм-менеджмент, а простые ритуалы: "
                        + "стакан воды перед работой, 10 минут тишины и список из трёх задач на день. "
                        + "Попробуйте — работает лучше любых приложений.",
                "Новости", now - day);
        long p5 = insertPost(db, max, "Плейлист для кодинга",
                "Собрал подборку инструментальной музыки, под которую не отвлекаешься. "
                        + "Идеально для тех дней, когда нужно закрыть сложную задачу. "
                        + "Кидайте свои любимые треки в комментарии.",
                "Музыка", now - 2 * day);
        long p6 = insertPost(db, lena, "Тред: как вы отдыхаете от соцсетей?",
                "Поймала себя на том, что листаю ленту на автомате. Устроила себе "
                        + "цифровой детокс на два дня — и как будто заново увидела город. "
                        + "Расскажите, какие у вас способы отдыхать от экрана?",
                "Вопросы", now - 3 * day);

        insertComment(db, p1, max, "Очень жду, когда доедет до наших экранов!", now - 20 * 60_000L);
        insertComment(db, p1, lena, "Тёмная тема — это любовь.", now - 15 * 60_000L);
        insertComment(db, p2, anna, "Была там в прошлые выходные, маршрут огонь.", now - 90 * 60_000L);
        insertComment(db, p3, max, "Добавил в список на вечер.", now - 4 * 3_600_000L);
        insertComment(db, p4, lena, "Список из трёх задач реально спасает.", now - 20 * 3_600_000L);
        insertComment(db, p5, anna, "Lo-fi beats — моё всё.", now - day);

        insertLike(db, p1, max);
        insertLike(db, p1, lena);
        insertLike(db, p2, anna);
        insertLike(db, p2, lena);
        insertLike(db, p3, anna);
        insertLike(db, p3, max);
        insertLike(db, p4, max);
        insertLike(db, p4, lena);
        insertLike(db, p5, anna);
        insertLike(db, p5, lena);
        insertLike(db, p6, anna);
        insertLike(db, p6, max);

        insertSubscription(db, anna, max);
        insertSubscription(db, anna, lena);
        insertSubscription(db, max, anna);
        insertSubscription(db, lena, anna);
        insertSubscription(db, max, admin);

        insertMessage(db, max, anna, "Привет! Видел твой пост про дизайн-систему — очень круто.",
                now - 3 * 3_600_000L, 1);
        insertMessage(db, anna, max, "Спасибо! Долго собирали токены, зато теперь проще.",
                now - 2 * 3_600_000L, 1);
        insertMessage(db, max, anna, "А компоненты в Figma или сразу в коде?",
                now - 105 * 60_000L, 0);
        insertMessage(db, lena, anna, "Анна, привет! Скинь, пожалуйста, плейлист для работы 🙌",
                now - 40 * 60_000L, 0);
    }

    private long insertUser(SQLiteDatabase db, String username, String password, String fullName,
                            String bio, String role, long createdAt, int blocked) {
        ContentValues values = new ContentValues();
        values.put(DbContract.COL_USERNAME, username);
        values.put(DbContract.COL_PASSWORD_HASH, PasswordUtils.hash(username, password));
        values.put(DbContract.COL_FULL_NAME, fullName);
        values.put(DbContract.COL_BIO, bio);
        values.put(DbContract.COL_ROLE, role);
        values.put(DbContract.COL_IS_BLOCKED, blocked);
        values.put(DbContract.COL_CREATED_AT, createdAt);
        return db.insertOrThrow(DbContract.TABLE_USERS, null, values);
    }

    private long insertPost(SQLiteDatabase db, long userId, String title, String content,
                            String category, long createdAt) {
        ContentValues values = new ContentValues();
        values.put(DbContract.COL_USER_ID, userId);
        values.put(DbContract.COL_TITLE, title);
        values.put(DbContract.COL_CONTENT, content);
        values.put(DbContract.COL_CATEGORY, category);
        values.put(DbContract.COL_CREATED_AT, createdAt);
        return db.insertOrThrow(DbContract.TABLE_POSTS, null, values);
    }

    private void insertComment(SQLiteDatabase db, long postId, long userId, String text,
                               long createdAt) {
        ContentValues values = new ContentValues();
        values.put(DbContract.COL_POST_ID, postId);
        values.put(DbContract.COL_USER_ID, userId);
        values.put(DbContract.COL_COMMENT_TEXT, text);
        values.put(DbContract.COL_CREATED_AT, createdAt);
        db.insertOrThrow(DbContract.TABLE_COMMENTS, null, values);
    }

    private void insertLike(SQLiteDatabase db, long postId, long userId) {
        ContentValues values = new ContentValues();
        values.put(DbContract.COL_POST_ID, postId);
        values.put(DbContract.COL_USER_ID, userId);
        values.put(DbContract.COL_CREATED_AT, System.currentTimeMillis());
        db.insertWithOnConflict(DbContract.TABLE_LIKES, null, values,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void insertSubscription(SQLiteDatabase db, long followerId, long followingId) {
        ContentValues values = new ContentValues();
        values.put(DbContract.COL_FOLLOWER_ID, followerId);
        values.put(DbContract.COL_FOLLOWING_ID, followingId);
        values.put(DbContract.COL_CREATED_AT, System.currentTimeMillis());
        db.insertWithOnConflict(DbContract.TABLE_SUBSCRIPTIONS, null, values,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void insertMessage(SQLiteDatabase db, long senderId, long recipientId, String text,
                               long createdAt, int read) {
        ContentValues values = new ContentValues();
        values.put(DbContract.COL_SENDER_ID, senderId);
        values.put(DbContract.COL_RECIPIENT_ID, recipientId);
        values.put(DbContract.COL_MESSAGE_TEXT, text);
        values.put(DbContract.COL_IS_READ, read);
        values.put(DbContract.COL_CREATED_AT, createdAt);
        db.insertOrThrow(DbContract.TABLE_MESSAGES, null, values);
    }
}
