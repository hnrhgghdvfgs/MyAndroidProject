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
import com.example.socialnetwork.data.model.Conversation;
import com.example.socialnetwork.util.TimeUtils;
import com.example.socialnetwork.widget.AvatarView;

/**
 * Список диалогов: собеседник, последнее сообщение, время и число непрочитанных.
 */
public class ConversationAdapter extends
        ListAdapter<Conversation, ConversationAdapter.ConversationViewHolder> {

    public interface Listener {
        void onConversationClick(Conversation conversation);

        void onConversationLongClick(Conversation conversation);
    }

    private static final DiffUtil.ItemCallback<Conversation> DIFF =
            new DiffUtil.ItemCallback<Conversation>() {
                @Override
                public boolean areItemsTheSame(@NonNull Conversation oldItem,
                                               @NonNull Conversation newItem) {
                    return oldItem.peerId == newItem.peerId;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Conversation oldItem,
                                                  @NonNull Conversation newItem) {
                    return oldItem.lastMessage.equals(newItem.lastMessage)
                            && oldItem.lastMessageAt == newItem.lastMessageAt
                            && oldItem.unreadCount == newItem.unreadCount
                            && oldItem.lastMessageMine == newItem.lastMessageMine
                            && oldItem.peerName.equals(newItem.peerName);
                }
            };

    private final Listener listener;

    public ConversationAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {

        private final AvatarView avatar;
        private final TextView name;
        private final TextView preview;
        private final TextView time;
        private final TextView unread;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.conversation_avatar);
            name = itemView.findViewById(R.id.conversation_name);
            preview = itemView.findViewById(R.id.conversation_preview);
            time = itemView.findViewById(R.id.conversation_time);
            unread = itemView.findViewById(R.id.conversation_unread);
        }

        void bind(Conversation conversation, Listener listener) {
            avatar.setUser(conversation.peerName, conversation.peerUsername);
            name.setText(conversation.peerName);
            preview.setText(conversation.lastMessageMine
                    ? itemView.getContext().getString(R.string.chats_you_prefix,
                    conversation.lastMessage)
                    : conversation.lastMessage);
            time.setText(TimeUtils.conversationTime(conversation.lastMessageAt));

            if (conversation.unreadCount > 0) {
                unread.setVisibility(View.VISIBLE);
                unread.setText(conversation.unreadCount > 99
                        ? "99+"
                        : String.valueOf(conversation.unreadCount));
                name.setTextColor(itemView.getContext().getColor(R.color.text_primary));
                preview.setTextColor(itemView.getContext().getColor(R.color.text_primary));
            } else {
                unread.setVisibility(View.GONE);
                preview.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
            }

            itemView.setOnClickListener(v -> listener.onConversationClick(conversation));
            itemView.setOnLongClickListener(v -> {
                listener.onConversationLongClick(conversation);
                return true;
            });
        }
    }
}
