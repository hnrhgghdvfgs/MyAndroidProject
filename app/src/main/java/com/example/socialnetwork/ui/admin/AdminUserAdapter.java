package com.example.socialnetwork.ui.admin;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialnetwork.R;
import com.example.socialnetwork.data.model.User;
import com.example.socialnetwork.widget.AvatarView;
import com.google.android.material.button.MaterialButton;

/**
 * Список пользователей в панели администратора.
 */
public class AdminUserAdapter extends ListAdapter<User, AdminUserAdapter.UserViewHolder> {

    public interface Listener {
        void onToggleBlock(User user);

        void onDelete(User user);
    }

    private static final DiffUtil.ItemCallback<User> DIFF = new DiffUtil.ItemCallback<User>() {
        @Override
        public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.blocked == newItem.blocked
                    && oldItem.fullName.equals(newItem.fullName)
                    && oldItem.role.equals(newItem.role);
        }
    };

    private final Listener listener;
    private final long currentUserId;

    public AdminUserAdapter(Listener listener, long currentUserId) {
        super(DIFF);
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(getItem(position), listener, currentUserId);
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {

        private final AvatarView avatar;
        private final TextView name;
        private final TextView username;
        private final TextView status;
        private final MaterialButton toggle;
        private final MaterialButton delete;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            name = itemView.findViewById(R.id.name);
            username = itemView.findViewById(R.id.username);
            status = itemView.findViewById(R.id.status);
            toggle = itemView.findViewById(R.id.btn_toggle_block);
            delete = itemView.findViewById(R.id.btn_delete_user);
        }

        void bind(User user, Listener listener, long currentUserId) {
            Context context = itemView.getContext();
            avatar.setUser(user.fullName, user.username);
            name.setText(user.fullName);
            username.setText(context.getString(R.string.username_at, user.username)
                    + (user.isAdmin() ? " · " + context.getString(R.string.role_admin) : ""));

            if (user.blocked) {
                status.setText(R.string.status_blocked);
                status.setTextColor(ContextCompat.getColor(context, R.color.error));
            } else {
                status.setText(R.string.status_active);
                status.setTextColor(ContextCompat.getColor(context, R.color.success));
            }

            toggle.setText(user.blocked ? R.string.btn_unblock : R.string.btn_block);
            int tint = ContextCompat.getColor(context, R.color.primary);
            toggle.setTextColor(tint);
            toggle.setStrokeColor(ColorStateList.valueOf(tint));

            boolean isSelf = user.id == currentUserId;
            toggle.setVisibility(isSelf ? View.GONE : View.VISIBLE);
            delete.setVisibility(isSelf ? View.GONE : View.VISIBLE);

            toggle.setOnClickListener(v -> listener.onToggleBlock(user));
            delete.setOnClickListener(v -> listener.onDelete(user));
        }
    }
}
