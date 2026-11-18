package com.example.socialnetwork.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.example.socialnetwork.ui.feed.PostAdapter;
import com.example.socialnetwork.ui.post.PostDetailActivity;
import com.example.socialnetwork.util.AppExecutors;
import com.example.socialnetwork.util.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Collections;
import java.util.List;

/**
 * Поиск публикаций в реальном времени (по заголовку, тексту, категории и автору).
 */
public class SearchFragment extends Fragment {

    private PostRepository postRepository;
    private SessionManager session;
    private PostAdapter adapter;
    private EditText searchInput;
    private TextView searchStatus;
    private RecyclerView searchList;

    private String currentQuery = "";
    private int requestToken = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        postRepository = new PostRepository(requireContext());
        session = new SessionManager(requireContext());

        searchInput = view.findViewById(R.id.search_input);
        searchStatus = view.findViewById(R.id.search_status);
        searchList = view.findViewById(R.id.search_list);

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
        searchList.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchList.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                performSearch(s.toString().trim());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!currentQuery.isEmpty()) {
            performSearch(currentQuery);
        }
    }

    private void performSearch(String query) {
        currentQuery = query;
        requestToken++;
        final int token = requestToken;

        if (query.isEmpty()) {
            adapter.submitList(Collections.emptyList());
            searchStatus.setText(R.string.search_empty_query);
            return;
        }

        long viewerId = session.getUserId();
        AppExecutors.get().execute(
                () -> postRepository.search(query, viewerId),
                new DataCallback<List<Post>>() {
                    @Override
                    public void onSuccess(List<Post> posts) {
                        if (!isAdded() || token != requestToken) {
                            return;
                        }
                        adapter.submitList(posts);
                        if (posts.isEmpty()) {
                            searchStatus.setText(R.string.search_no_results);
                        } else {
                            searchStatus.setText(getString(R.string.search_results, posts.size()));
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (isAdded() && token == requestToken) {
                            searchStatus.setText(R.string.error_generic);
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
                        performSearch(currentQuery);
                    }
                });
    }
}
