package com.example.socialnetwork.ui.chat;

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
import com.example.socialnetwork.data.MessageRepository;
import com.example.socialnetwork.data.model.Conversation;
import com.example.socialnetwork.util.AppExecutors;
import com.example.socialnetwork.util.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * Раздел «Чаты»: список диалогов и переход в переписку.
 */
public class ChatsFragment extends Fragment {

    private MessageRepository messageRepository;
    private SessionManager session;
    private ConversationAdapter adapter;
    private TextView emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        messageRepository = new MessageRepository(requireContext());
        session = new SessionManager(requireContext());
        emptyView = view.findViewById(R.id.chats_empty);

        RecyclerView list = view.findViewById(R.id.chats_list);
        adapter = new ConversationAdapter(new ConversationAdapter.Listener() {
            @Override
            public void onConversationClick(Conversation conversation) {
                startActivity(ChatActivity.intent(requireContext(), conversation.peerId));
            }

            @Override
            public void onConversationLongClick(Conversation conversation) {
                confirmDelete(conversation);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        view.findViewById(R.id.btn_new_chat).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NewChatActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadConversations();
    }

    /**
     * Перезагружает список диалогов — вызывается из MainActivity при показе вкладки.
     */
    public void refresh() {
        if (isAdded() && getView() != null) {
            loadConversations();
        }
    }

    private void loadConversations() {
        long userId = session.getUserId();
        AppExecutors.get().execute(
                () -> messageRepository.getConversations(userId),
                new DataCallback<List<Conversation>>() {
                    @Override
                    public void onSuccess(List<Conversation> conversations) {
                        if (!isAdded() || emptyView == null) {
                            return;
                        }
                        adapter.submitList(conversations);
                        emptyView.setVisibility(conversations.isEmpty()
                                ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void confirmDelete(Conversation conversation) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_chat_title)
                .setMessage(R.string.dialog_delete_chat_message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    long userId = session.getUserId();
                    AppExecutors.get().execute(
                            () -> {
                                messageRepository.deleteConversation(userId,
                                        conversation.peerId);
                                return Boolean.TRUE;
                            },
                            new DataCallback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean result) {
                                    if (!isAdded()) {
                                        return;
                                    }
                                    Toast.makeText(requireContext(), R.string.toast_chat_deleted,
                                            Toast.LENGTH_SHORT).show();
                                    loadConversations();
                                }
                            });
                })
                .show();
    }
}
