package com.example.socialnetwork.ui.post;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialnetwork.R;
import com.example.socialnetwork.data.CommentRepository;
import com.example.socialnetwork.data.DataCallback;
import com.example.socialnetwork.data.PostRepository;
import com.example.socialnetwork.data.UserRepository;
import com.example.socialnetwork.data.model.Comment;
import com.example.socialnetwork.data.model.LikeResult;
import com.example.socialnetwork.data.model.Post;
import com.example.socialnetwork.data.model.UserStats;
import com.example.socialnetwork.ui.chat.ChatActivity;
import com.example.socialnetwork.util.AppExecutors;
import com.example.socialnetwork.util.ImageLoader;
import com.example.socialnetwork.util.KeyboardUtils;
import com.example.socialnetwork.util.SessionManager;
import com.example.socialnetwork.util.TimeUtils;
import com.example.socialnetwork.widget.AvatarView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * Экран деталей публикации: полный текст, лайки, подписка и комментарии.
 */
public class PostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "extra_post_id";

    private SessionManager session;
    private PostRepository postRepository;
    private CommentRepository commentRepository;
    private UserRepository userRepository;

    private long postId;
    private Post post;
    private boolean following;
    private CommentAdapter commentAdapter;

    private AvatarView avatar;
    private TextView author;
    private TextView meta;
    private TextView category;
    private TextView title;
    private TextView content;
    private TextView likeCount;
    private TextView commentCount;
    private TextView commentsHeader;
    private TextView commentsEmpty;
    private ImageView likeIcon;
    private MaterialButton btnFollow;
    private MaterialButton btnWriteMessage;
    private EditText commentInput;
    private ImageButton btnSend;
    private RecyclerView commentsList;
    private View imageCard;
    private ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        postId = getIntent().getLongExtra(EXTRA_POST_ID, -1L);
        if (postId <= 0) {
            finish();
            return;
        }

        setContentView(R.layout.activity_post_detail);
        session = new SessionManager(this);
        postRepository = new PostRepository(this);
        commentRepository = new CommentRepository(this);
        userRepository = new UserRepository(this);

        avatar = findViewById(R.id.detail_avatar);
        author = findViewById(R.id.detail_author);
        meta = findViewById(R.id.detail_meta);
        category = findViewById(R.id.detail_category);
        title = findViewById(R.id.detail_title);
        content = findViewById(R.id.detail_content);
        likeCount = findViewById(R.id.detail_like_count);
        commentCount = findViewById(R.id.detail_comment_count);
        commentsHeader = findViewById(R.id.comments_header);
        commentsEmpty = findViewById(R.id.comments_empty);
        likeIcon = findViewById(R.id.detail_like_icon);
        btnFollow = findViewById(R.id.btn_follow);
        btnWriteMessage = findViewById(R.id.btn_write_message);
        commentInput = findViewById(R.id.comment_input);
        btnSend = findViewById(R.id.btn_send);
        commentsList = findViewById(R.id.comments_list);
        imageCard = findViewById(R.id.detail_image_card);
        image = findViewById(R.id.detail_image);

        commentAdapter = new CommentAdapter(this::confirmDeleteComment);
        commentsList.setLayoutManager(new LinearLayoutManager(this));
        commentsList.setAdapter(commentAdapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.like_wrap).setOnClickListener(v -> toggleLike());
        btnSend.setOnClickListener(v -> sendComment());
        btnFollow.setOnClickListener(v -> toggleFollow());
        btnWriteMessage.setOnClickListener(v -> {
            if (post != null) {
                startActivity(ChatActivity.intent(this, post.userId));
            }
        });

        loadPost();
    }

    private void loadPost() {
        long viewerId = session.getUserId();
        AppExecutors.get().execute(
                () -> postRepository.getPost(postId, viewerId),
                new DataCallback<Post>() {
                    @Override
                    public void onSuccess(Post result) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (result == null) {
                            Toast.makeText(PostDetailActivity.this, R.string.error_generic,
                                    Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        post = result;
                        bindPost();
                        loadComments();
                        if (post.userId != viewerId) {
                            loadFollowState(viewerId);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isFinishing()) {
                            Toast.makeText(PostDetailActivity.this, R.string.error_generic,
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
    }

    private void bindPost() {
        avatar.setUser(post.authorName, post.authorUsername);
        author.setText(post.authorName);
        meta.setText(TimeUtils.relative(post.createdAt));
        category.setText(post.category);
        title.setText(post.title);
        content.setText(post.content);
        likeCount.setText(String.valueOf(post.likeCount));
        commentCount.setText(String.valueOf(post.commentCount));
        commentsHeader.setText(getString(R.string.comments_title, post.commentCount));
        updateLikeIcon();

        if (post.hasImage()) {
            imageCard.setVisibility(View.VISIBLE);
            ImageLoader.get().load(image, post.imagePath);
        } else {
            imageCard.setVisibility(View.GONE);
            ImageLoader.get().load(image, null);
        }
        btnFollow.setVisibility(post.userId == session.getUserId() ? View.GONE : View.VISIBLE);
        btnWriteMessage.setVisibility(post.userId == session.getUserId() ? View.GONE : View.VISIBLE);
    }

    private void updateLikeIcon() {
        int tint = ContextCompat.getColor(this,
                post.liked ? R.color.like : R.color.text_secondary);
        likeIcon.setImageTintList(ColorStateList.valueOf(tint));
    }

    private void loadFollowState(long viewerId) {
        AppExecutors.get().execute(
                () -> userRepository.statsFor(post.userId, viewerId),
                new DataCallback<UserStats>() {
                    @Override
                    public void onSuccess(UserStats stats) {
                        following = stats.following;
                        updateFollowButton();
                    }
                });
    }

    private void updateFollowButton() {
        btnFollow.setText(following ? R.string.unfollow : R.string.follow);
    }

    private void toggleFollow() {
        if (post == null) {
            return;
        }
        btnFollow.setEnabled(false);
        long viewerId = session.getUserId();
        AppExecutors.get().execute(
                () -> userRepository.toggleFollow(viewerId, post.userId),
                new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        btnFollow.setEnabled(true);
                        following = result;
                        updateFollowButton();
                        Toast.makeText(PostDetailActivity.this,
                                following ? R.string.toast_followed : R.string.toast_unfollowed,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        btnFollow.setEnabled(true);
                    }
                });
    }

    private void toggleLike() {
        if (post == null) {
            return;
        }
        long viewerId = session.getUserId();
        AppExecutors.get().execute(
                () -> postRepository.toggleLike(post.id, viewerId),
                new DataCallback<LikeResult>() {
                    @Override
                    public void onSuccess(LikeResult result) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        post.liked = result.liked;
                        post.likeCount = result.count;
                        likeCount.setText(String.valueOf(post.likeCount));
                        updateLikeIcon();
                    }
                });
    }

    private void loadComments() {
        AppExecutors.get().execute(
                () -> commentRepository.getComments(postId),
                new DataCallback<List<Comment>>() {
                    @Override
                    public void onSuccess(List<Comment> comments) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        commentAdapter.submitList(comments);
                        commentsEmpty.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
                        commentsHeader.setText(getString(R.string.comments_title, comments.size()));
                        commentCount.setText(String.valueOf(comments.size()));
                    }
                });
    }

    private void sendComment() {
        if (post == null) {
            return;
        }
        String text = commentInput.getText() == null
                ? "" : commentInput.getText().toString().trim();
        if (text.isEmpty()) {
            commentInput.setError(getString(R.string.error_comment_empty));
            return;
        }
        commentInput.setError(null);
        btnSend.setEnabled(false);
        long viewerId = session.getUserId();
        AppExecutors.get().execute(
                () -> commentRepository.addComment(post.id, viewerId, text),
                new DataCallback<Comment>() {
                    @Override
                    public void onSuccess(Comment comment) {
                        btnSend.setEnabled(true);
                        if (comment == null) {
                            Toast.makeText(PostDetailActivity.this, R.string.error_generic,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        commentInput.setText("");
                        KeyboardUtils.hide(commentInput);
                        Toast.makeText(PostDetailActivity.this, R.string.toast_comment_added,
                                Toast.LENGTH_SHORT).show();
                        loadComments();
                    }

                    @Override
                    public void onError(Exception e) {
                        btnSend.setEnabled(true);
                        Toast.makeText(PostDetailActivity.this, R.string.error_generic,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmDeleteComment(Comment comment) {
        boolean allowed = comment.userId == session.getUserId() || session.isAdmin();
        if (!allowed) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_comment_title)
                .setMessage(R.string.dialog_delete_comment_message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> deleteComment(comment))
                .show();
    }

    private void deleteComment(Comment comment) {
        AppExecutors.get().execute(
                () -> commentRepository.deleteComment(comment.id),
                new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean deleted) {
                        Toast.makeText(PostDetailActivity.this, R.string.toast_comment_deleted,
                                Toast.LENGTH_SHORT).show();
                        loadComments();
                    }
                });
    }
}
