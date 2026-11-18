package com.example.socialnetwork.ui.feed;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialnetwork.R;
import com.example.socialnetwork.data.model.Post;
import com.example.socialnetwork.util.ImageLoader;
import com.example.socialnetwork.util.TimeUtils;
import com.example.socialnetwork.widget.AvatarView;

/**
 * Адаптер ленты публикаций с DiffUtil и кастомным ViewHolder.
 */
public class PostAdapter extends ListAdapter<Post, PostAdapter.PostViewHolder> {

    public interface Listener {
        void onPostClick(Post post);

        void onLikeClick(Post post);

        void onPostLongClick(Post post);
    }

    private static final DiffUtil.ItemCallback<Post> DIFF = new DiffUtil.ItemCallback<Post>() {
        @Override
        public boolean areItemsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            return oldItem.likeCount == newItem.likeCount
                    && oldItem.commentCount == newItem.commentCount
                    && oldItem.liked == newItem.liked
                    && oldItem.title.equals(newItem.title)
                    && oldItem.content.equals(newItem.content)
                    && oldItem.category.equals(newItem.category)
                    && java.util.Objects.equals(oldItem.imagePath, newItem.imagePath)
                    && oldItem.authorName.equals(newItem.authorName);
        }
    };

    private final Listener listener;

    public PostAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {

        private final AvatarView avatar;
        private final TextView authorName;
        private final TextView postTime;
        private final TextView categoryChip;
        private final TextView title;
        private final TextView content;
        private final TextView likeCount;
        private final TextView commentCount;
        private final ImageView likeIcon;
        private final View likeWrap;
        private final View imageCard;
        private final ImageView image;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            authorName = itemView.findViewById(R.id.author_name);
            postTime = itemView.findViewById(R.id.post_time);
            categoryChip = itemView.findViewById(R.id.category_chip);
            title = itemView.findViewById(R.id.post_title);
            content = itemView.findViewById(R.id.post_content);
            likeCount = itemView.findViewById(R.id.like_count);
            commentCount = itemView.findViewById(R.id.comment_count);
            likeIcon = itemView.findViewById(R.id.like_icon);
            likeWrap = itemView.findViewById(R.id.like_wrap);
            imageCard = itemView.findViewById(R.id.post_image_card);
            image = itemView.findViewById(R.id.post_image);
        }

        void bind(Post post, Listener listener) {
            Context context = itemView.getContext();
            avatar.setUser(post.authorName, post.authorUsername);
            authorName.setText(post.authorName);
            postTime.setText(TimeUtils.relative(post.createdAt));
            categoryChip.setText(post.category);
            title.setText(post.title);
            content.setText(post.content);
            likeCount.setText(String.valueOf(post.likeCount));
            commentCount.setText(String.valueOf(post.commentCount));

            if (post.hasImage()) {
                imageCard.setVisibility(View.VISIBLE);
                ImageLoader.get().load(image, post.imagePath);
            } else {
                imageCard.setVisibility(View.GONE);
                ImageLoader.get().load(image, null);
            }

            int tint = ContextCompat.getColor(context,
                    post.liked ? R.color.like : R.color.text_secondary);
            likeIcon.setImageTintList(ColorStateList.valueOf(tint));

            itemView.setOnClickListener(v -> listener.onPostClick(post));
            itemView.setOnLongClickListener(v -> {
                listener.onPostLongClick(post);
                return true;
            });
            likeWrap.setOnClickListener(v -> listener.onLikeClick(post));
        }
    }
}
