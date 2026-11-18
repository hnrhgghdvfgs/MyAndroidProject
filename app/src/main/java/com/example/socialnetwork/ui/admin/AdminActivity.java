package com.example.socialnetwork.ui.admin;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialnetwork.R;
import com.example.socialnetwork.data.DataCallback;
import com.example.socialnetwork.data.UserRepository;
import com.example.socialnetwork.data.model.AdminStats;
import com.example.socialnetwork.data.model.User;
import com.example.socialnetwork.util.AppExecutors;
import com.example.socialnetwork.util.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * Панель администратора: статистика, блокировка и удаление пользователей.
 */
public class AdminActivity extends AppCompatActivity {

    private UserRepository userRepository;
    private SessionManager session;
    private AdminUserAdapter adapter;

    private TextView statUsers;
    private TextView statPosts;
    private TextView statComments;
    private TextView statLikes;
    private TextView statBlocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        if (!session.isAdmin()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_admin);
        userRepository = new UserRepository(this);

        statUsers = findViewById(R.id.admin_stat_users);
        statPosts = findViewById(R.id.admin_stat_posts);
        statComments = findViewById(R.id.admin_stat_comments);
        statLikes = findViewById(R.id.admin_stat_likes);
        statBlocked = findViewById(R.id.admin_stat_blocked);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView usersList = findViewById(R.id.admin_users);
        adapter = new AdminUserAdapter(new AdminUserAdapter.Listener() {
            @Override
            public void onToggleBlock(User user) {
                setBlocked(user);
            }

            @Override
            public void onDelete(User user) {
                confirmDelete(user);
            }
        }, session.getUserId());
        usersList.setLayoutManager(new LinearLayoutManager(this));
        usersList.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        AppExecutors.get().execute(
                () -> userRepository.adminStats(),
                new DataCallback<AdminStats>() {
                    @Override
                    public void onSuccess(AdminStats stats) {
                        statUsers.setText(String.valueOf(stats.users));
                        statPosts.setText(String.valueOf(stats.posts));
                        statComments.setText(String.valueOf(stats.comments));
                        statLikes.setText(String.valueOf(stats.likes));
                        statBlocked.setText(getString(R.string.admin_blocked_line, stats.blocked));
                    }
                });

        AppExecutors.get().execute(
                () -> userRepository.allUsers(),
                new DataCallback<List<User>>() {
                    @Override
                    public void onSuccess(List<User> users) {
                        adapter.submitList(users);
                    }
                });
    }

    private void setBlocked(User user) {
        boolean target = !user.blocked;
        AppExecutors.get().execute(
                () -> {
                    userRepository.setBlocked(user.id, target);
                    return Boolean.TRUE;
                },
                new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        Toast.makeText(AdminActivity.this,
                                target ? R.string.toast_user_blocked : R.string.toast_user_unblocked,
                                Toast.LENGTH_SHORT).show();
                        loadData();
                    }
                });
    }

    private void confirmDelete(User user) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_user_title)
                .setMessage(R.string.dialog_delete_user_message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> deleteUser(user))
                .show();
    }

    private void deleteUser(User user) {
        if (user.id == session.getUserId()) {
            return;
        }
        AppExecutors.get().execute(
                () -> {
                    userRepository.deleteUser(user.id);
                    return Boolean.TRUE;
                },
                new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        Toast.makeText(AdminActivity.this, R.string.toast_user_deleted,
                                Toast.LENGTH_SHORT).show();
                        loadData();
                    }
                });
    }
}
