package com.example.socialnetwork.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialnetwork.R;
import com.example.socialnetwork.data.DataCallback;
import com.example.socialnetwork.data.PostRepository;
import com.example.socialnetwork.data.UserRepository;
import com.example.socialnetwork.data.model.LikeResult;
import com.example.socialnetwork.data.model.Post;
import com.example.socialnetwork.data.model.User;
import com.example.socialnetwork.data.model.UserStats;
import com.example.socialnetwork.ui.admin.AdminActivity;
import com.example.socialnetwork.ui.auth.LoginActivity;
import com.example.socialnetwork.ui.chat.NewChatActivity;
import com.example.socialnetwork.ui.feed.PostAdapter;
import com.example.socialnetwork.ui.post.PostDetailActivity;
import com.example.socialnetwork.util.AppExecutors;
import com.example.socialnetwork.util.SessionManager;
import com.example.socialnetwork.util.ThemeManager;
import com.example.socialnetwork.widget.AvatarView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

/**
 * Профиль текущего пользователя: данные, статистика, настройки и свои публикации.
 */
public class ProfileFragment extends Fragment {

    private SessionManager session;
    private UserRepository userRepository;
    private PostRepository postRepository;

    private AvatarView avatar;
    private TextView name;
    private TextView username;
    private TextView bio;
    private TextView statPosts;
    private TextView statFollowers;
    private TextView statLikes;
    private TextView postsEmpty;
    private MaterialSwitch themeSwitch;
    private MaterialButton btnAdmin;
    private PostAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session = new SessionManager(requireContext());
        userRepository = new UserRepository(requireContext());
        postRepository = new PostRepository(requireContext());

        avatar = view.findViewById(R.id.profile_avatar);
        name = view.findViewById(R.id.profile_name);
        username = view.findViewById(R.id.profile_username);
        bio = view.findViewById(R.id.profile_bio);
        statPosts = view.findViewById(R.id.stat_posts_value);
        statFollowers = view.findViewById(R.id.stat_followers_value);
        statLikes = view.findViewById(R.id.stat_likes_value);
        postsEmpty = view.findViewById(R.id.profile_posts_empty);
        themeSwitch = view.findViewById(R.id.switch_theme);
        btnAdmin = view.findViewById(R.id.btn_admin);

        RecyclerView postsList = view.findViewById(R.id.profile_posts);
        adapter = new PostAdapter(new PostAdapter.Listener() {
            @Override
            public void onPostClick(Post post) {
                Intent intent = new Intent(requireContext(), PostDetailActivity.class);
                intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.id);
                startActivity(intent);
            }

            @Override
            public void onLikeClick(Post post) {
                toggleLike(post);
            }

            @Override
            public void onPostLongClick(Post post) {
                confirmDelete(post);
            }
        });
        postsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        postsList.setAdapter(adapter);

        themeSwitch.setChecked(ThemeManager.isDarkSelected(requireContext()));
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                ThemeManager.setDark(requireContext(), isChecked));

        view.findViewById(R.id.btn_edit_profile).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));
        view.findViewById(R.id.btn_messages).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NewChatActivity.class)));
        btnAdmin.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AdminActivity.class)));
        view.findViewById(R.id.btn_logout).setOnClickListener(v -> confirmLogout());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
        loadPosts();
    }

    private void loadProfile() {
        long userId = session.getUserId();
        AppExecutors.get().execute(
                () -> userRepository.findById(userId),
                new DataCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        if (!isAdded()) {
                            return;
                        }
                        if (user == null) {
                            logout();
                            return;
                        }
                        avatar.setUser(user.fullName, user.username);
                        name.setText(user.fullName);
                        username.setText(getString(R.string.username_at, user.username));
                        bio.setText(user.bio.isEmpty()
                                ? getString(R.string.bio_empty) : user.bio);
                        btnAdmin.setVisibility(user.isAdmin() ? View.VISIBLE : View.GONE);
                    }
                });

        AppExecutors.get().execute(
                () -> userRepository.statsFor(userId, userId),
                new DataCallback<UserStats>() {
                    @Override
                    public void onSuccess(UserStats stats) {
                        if (!isAdded()) {
                            return;
                        }
                        statPosts.setText(String.valueOf(stats.posts));
                        statFollowers.setText(String.valueOf(stats.followers));
                        statLikes.setText(String.valueOf(stats.likes));
                    }
                });
    }

    private void loadPosts() {
        long userId = session.getUserId();
        AppExecutors.get().execute(
                () -> postRepository.getUserPosts(userId, userId),
                new DataCallback<List<Post>>() {
                    @Override
                    public void onSuccess(List<Post> posts) {
                        if (!isAdded()) {
                            return;
                        }
                        adapter.submitList(posts);
                        postsEmpty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void toggleLike(Post post) {
        long userId = session.getUserId();
        AppExecutors.get().execute(
                () -> postRepository.toggleLike(post.id, userId),
                new DataCallback<LikeResult>() {
                    @Override
                    public void onSuccess(LikeResult result) {
                        if (!isAdded()) {
                            return;
                        }
                        post.liked = result.liked;
                        post.likeCount = result.count;
                        List<Post> current = adapter.getCurrentList();
                        for (int i = 0; i < current.size(); i++) {
                            if (current.get(i).id == post.id) {
                                adapter.notifyItemChanged(i);
                                break;
                            }
                        }
                    }
                });
    }

    private void confirmDelete(Post post) {
        if (post.userId != session.getUserId() && !session.isAdmin()) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_post_title)
                .setMessage(R.string.dialog_delete_post_message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> deletePost(post))
                .show();
    }

    private void deletePost(Post post) {
        AppExecutors.get().execute(
                () -> postRepository.deletePost(post.id),
                new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean deleted) {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(requireContext(), R.string.toast_post_deleted,
                                Toast.LENGTH_SHORT).show();
                        loadProfile();
                        loadPosts();
                    }
                });
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_logout_confirm,
                        (dialog, which) -> logout())
                .show();
    }

    private void logout() {
        session.clear();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
