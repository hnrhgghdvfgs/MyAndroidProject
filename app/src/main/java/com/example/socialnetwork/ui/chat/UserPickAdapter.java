package com.example.socialnetwork.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialnetwork.R;
import com.example.socialnetwork.data.model.User;
import com.example.socialnetwork.widget.AvatarView;

/**
 * Выбор собеседника для нового чата.
 */
public class UserPickAdapter extends ListAdapter<User, UserPickAdapter.UserViewHolder> {

    public interface Listener {
        void onUserClick(User user);
    }

    private static final DiffUtil.ItemCallback<User> DIFF = new DiffUtil.ItemCallback<User>() {
        @Override
        public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.fullName.equals(newItem.fullName)
                    && oldItem.username.equals(newItem.username);
        }
    };

    private final Listener listener;

    public UserPickAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_pick, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {

        private final AvatarView avatar;
        private final TextView name;
        private final TextView username;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.user_avatar);
            name = itemView.findViewById(R.id.user_name);
            username = itemView.findViewById(R.id.user_username);
        }

        void bind(User user, Listener listener) {
            avatar.setUser(user.fullName, user.username);
            name.setText(user.fullName);
            username.setText(itemView.getContext().getString(R.string.username_at, user.username));
            itemView.setOnClickListener(v -> listener.onUserClick(user));
        }
    }
}
