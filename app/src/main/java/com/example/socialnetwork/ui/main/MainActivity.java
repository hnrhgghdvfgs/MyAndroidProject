package com.example.socialnetwork.ui.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.socialnetwork.R;
import com.example.socialnetwork.data.DataCallback;
import com.example.socialnetwork.data.MessageRepository;
import com.example.socialnetwork.ui.auth.LoginActivity;
import com.example.socialnetwork.ui.chat.ChatsFragment;
import com.example.socialnetwork.ui.feed.FeedFragment;
import com.example.socialnetwork.ui.post.CreatePostActivity;
import com.example.socialnetwork.ui.profile.ProfileFragment;
import com.example.socialnetwork.ui.search.SearchFragment;
import com.example.socialnetwork.util.AppExecutors;
import com.example.socialnetwork.util.SessionManager;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Главный экран с BottomNavigationView: лента, поиск, создание, чаты, профиль.
 * Фрагменты добавляются один раз и переключаются через show/hide — состояние сохраняется.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG_FEED = "tab_feed";
    private static final String TAG_SEARCH = "tab_search";
    private static final String TAG_CHATS = "tab_chats";
    private static final String TAG_PROFILE = "tab_profile";

    private static final String[] TABS = {TAG_FEED, TAG_SEARCH, TAG_CHATS, TAG_PROFILE};

    private BottomNavigationView bottomNav;
    private FeedFragment feedFragment;
    private SearchFragment searchFragment;
    private ChatsFragment chatsFragment;
    private ProfileFragment profileFragment;

    private MessageRepository messageRepository;
    private SessionManager session;

    private final ActivityResultLauncher<Intent> createPostLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && feedFragment != null) {
                    feedFragment.refreshAll();
                    bottomNav.setSelectedItemId(R.id.nav_feed);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        messageRepository = new MessageRepository(this);
        bottomNav = findViewById(R.id.bottom_nav);
        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            feedFragment = new FeedFragment();
            searchFragment = new SearchFragment();
            chatsFragment = new ChatsFragment();
            profileFragment = new ProfileFragment();

            fm.beginTransaction()
                    .add(R.id.main_container, feedFragment, TAG_FEED)
                    .add(R.id.main_container, searchFragment, TAG_SEARCH)
                    .hide(searchFragment)
                    .add(R.id.main_container, chatsFragment, TAG_CHATS)
                    .hide(chatsFragment)
                    .add(R.id.main_container, profileFragment, TAG_PROFILE)
                    .hide(profileFragment)
                    .commit();
        } else {
            feedFragment = (FeedFragment) fm.findFragmentByTag(TAG_FEED);
            searchFragment = (SearchFragment) fm.findFragmentByTag(TAG_SEARCH);
            chatsFragment = (ChatsFragment) fm.findFragmentByTag(TAG_CHATS);
            profileFragment = (ProfileFragment) fm.findFragmentByTag(TAG_PROFILE);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_create) {
                createPostLauncher.launch(new Intent(this, CreatePostActivity.class));
                return false;
            }
            if (id == R.id.nav_feed) {
                showTab(TAG_FEED);
            } else if (id == R.id.nav_search) {
                showTab(TAG_SEARCH);
            } else if (id == R.id.nav_chats) {
                showTab(TAG_CHATS);
            } else if (id == R.id.nav_profile) {
                showTab(TAG_PROFILE);
            }
            return true;
        });

        if (savedInstanceState == null) {
            showTab(TAG_FEED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUnreadBadge();
    }

    /**
     * Показывает на вкладке «Чаты» количество непрочитанных сообщений.
     */
    private void updateUnreadBadge() {
        if (bottomNav == null) {
            return;
        }
        long userId = session.getUserId();
        AppExecutors.get().execute(
                () -> messageRepository.unreadCount(userId),
                new DataCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer count) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (count == null || count == 0) {
                            bottomNav.removeBadge(R.id.nav_chats);
                            return;
                        }
                        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_chats);
                        badge.setNumber(count);
                        badge.setMaxCharacterCount(3);
                        badge.setBackgroundColor(androidx.core.content.ContextCompat
                                .getColor(MainActivity.this, R.color.primary));
                        badge.setBadgeTextColor(androidx.core.content.ContextCompat
                                .getColor(MainActivity.this, R.color.on_primary));
                        badge.setVisible(true);
                    }
                });
    }

    private void showTab(String tag) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment target = fm.findFragmentByTag(tag);
        if (target == null) {
            return;
        }
        FragmentTransaction transaction = fm.beginTransaction();
        for (String current : TABS) {
            Fragment fragment = fm.findFragmentByTag(current);
            if (fragment == null) {
                continue;
            }
            if (fragment == target) {
                transaction.show(fragment);
            } else {
                transaction.hide(fragment);
            }
        }
        transaction.commit();

        if (TAG_CHATS.equals(tag) && chatsFragment != null) {
            chatsFragment.refresh();
        }
        updateUnreadBadge();
    }
}
