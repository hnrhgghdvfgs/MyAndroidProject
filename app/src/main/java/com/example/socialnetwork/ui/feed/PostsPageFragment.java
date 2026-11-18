package com.example.socialnetwork.ui.feed;

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
import com.example.socialnetwork.data.model.LikeResult;
import com.example.socialnetwork.data.model.Post;
import com.example.socialnetwork.ui.post.PostDetailActivity;
import com.example.socialnetwork.util.AppExecutors;
import com.example.socialnetwork.util.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * Одна страница ленты (Все / Подписки / Популярное).
 */
public class PostsPageFragment extends Fragment {

    private static final String ARG_TYPE = "arg_type";

    private PostRepository.FeedType feedType;
    private PostRepository postRepository;
    private SessionManager session;
    private PostAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyState;

    public static PostsPageFragment newInstance(PostRepository.FeedType type) {
        PostsPageFragment fragment = new PostsPageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type.name());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String type = getArguments() == null ? null : getArguments().getString(ARG_TYPE);
        feedType = type == null
                ? PostRepository.FeedType.ALL
                : PostRepository.FeedType.valueOf(type);
        postRepository = new PostRepository(requireContext());
        session = new SessionManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_posts_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.posts_list);
        emptyState = view.findViewById(R.id.empty_state);
        TextView emptyText = view.findViewById(R.id.empty_text);

        if (feedType == PostRepository.FeedType.FOLLOWING) {
            emptyText.setText(R.string.empty_following);
        } else if (feedType == PostRepository.FeedType.POPULAR) {
            emptyText.setText(R.string.empty_popular);
        } else {
            emptyText.setText(R.string.empty_feed);
        }

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
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPosts();
    }

    public void refresh() {
        if (isAdded()) {
            loadPosts();
        }
    }

    private void loadPosts() {
        if (recyclerView == null) {
            return;
        }
        long viewerId = session.getUserId();
        AppExecutors.get().execute(
                () -> postRepository.getFeed(feedType, viewerId),
                new DataCallback<List<Post>>() {
                    @Override
                    public void onSuccess(List<Post> posts) {
                        if (!isAdded() || adapter == null) {
                            return;
                        }
                        adapter.submitList(posts);
                        emptyState.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), R.string.error_generic,
                                    Toast.LENGTH_SHORT).show();
                        }
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
                        if (!isAdded() || adapter == null) {
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
        boolean allowed = post.userId == session.getUserId() || session.isAdmin();
        if (!allowed) {
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
                        loadPosts();
                    }
                });
    }
}
