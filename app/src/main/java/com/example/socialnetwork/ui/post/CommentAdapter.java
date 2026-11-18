package com.example.socialnetwork.ui.post;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialnetwork.R;
import com.example.socialnetwork.data.model.Comment;
import com.example.socialnetwork.util.TimeUtils;
import com.example.socialnetwork.widget.AvatarView;

/**
 * Адаптер комментариев. Долгое нажатие — удаление (для автора или администратора).
 */
public class CommentAdapter extends ListAdapter<Comment, CommentAdapter.CommentViewHolder> {

    public interface Listener {
        void onCommentLongClick(Comment comment);
    }

    private static final DiffUtil.ItemCallback<Comment> DIFF = new DiffUtil.ItemCallback<Comment>() {
        @Override
        public boolean areItemsTheSame(@NonNull Comment oldItem, @NonNull Comment newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Comment oldItem, @NonNull Comment newItem) {
            return oldItem.text.equals(newItem.text)
                    && oldItem.authorName.equals(newItem.authorName);
        }
    };

    private final Listener listener;

    public CommentAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {

        private final AvatarView avatar;
        private final TextView authorName;
        private final TextView time;
        private final TextView text;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            authorName = itemView.findViewById(R.id.author_name);
            time = itemView.findViewById(R.id.comment_time);
            text = itemView.findViewById(R.id.comment_text);
        }

        void bind(Comment comment, Listener listener) {
            avatar.setUser(comment.authorName, comment.authorUsername);
            authorName.setText(comment.authorName);
            time.setText(TimeUtils.relative(comment.createdAt));
            text.setText(comment.text);
            itemView.setOnLongClickListener(v -> {
                listener.onCommentLongClick(comment);
                return true;
            });
        }
    }
}
