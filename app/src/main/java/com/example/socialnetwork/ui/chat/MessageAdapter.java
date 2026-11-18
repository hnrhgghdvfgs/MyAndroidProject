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
import com.example.socialnetwork.data.model.Message;
import com.example.socialnetwork.util.TimeUtils;

/**
 * Переписка: входящие сообщения слева, исходящие справа.
 */
public class MessageAdapter extends ListAdapter<Message, RecyclerView.ViewHolder> {

    private static final int TYPE_IN = 0;
    private static final int TYPE_OUT = 1;

    private final long viewerId;

    private static final DiffUtil.ItemCallback<Message> DIFF =
            new DiffUtil.ItemCallback<Message>() {
                @Override
                public boolean areItemsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Message oldItem,
                                                  @NonNull Message newItem) {
                    return oldItem.text.equals(newItem.text)
                            && oldItem.read == newItem.read;
                }
            };

    public MessageAdapter(long viewerId) {
        super(DIFF);
        this.viewerId = viewerId;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).isMine(viewerId) ? TYPE_OUT : TYPE_IN;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_OUT ? R.layout.item_message_out : R.layout.item_message_in;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((MessageViewHolder) holder).bind(getItem(position));
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView text;
        private final TextView time;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.message_text);
            time = itemView.findViewById(R.id.message_time);
        }

        void bind(Message message) {
            text.setText(message.text);
            time.setText(TimeUtils.messageTime(message.createdAt));
        }
    }
}
