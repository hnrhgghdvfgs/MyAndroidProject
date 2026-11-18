package com.example.socialnetwork.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialnetwork.R;
import com.example.socialnetwork.data.DataCallback;
import com.example.socialnetwork.data.MessageRepository;
import com.example.socialnetwork.data.UserRepository;
import com.example.socialnetwork.data.model.Message;
import com.example.socialnetwork.data.model.User;
import com.example.socialnetwork.util.AppExecutors;
import com.example.socialnetwork.util.KeyboardUtils;
import com.example.socialnetwork.util.SessionManager;
import com.example.socialnetwork.widget.AvatarView;

import java.util.List;

/**
 * Экран переписки с одним пользователем: история сообщений и поле отправки.
 */
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_PEER_ID = "extra_peer_id";

    private static final long REFRESH_INTERVAL_MS = 3000L;

    private MessageRepository messageRepository;
    private UserRepository userRepository;
    private SessionManager session;

    private long peerId;
    private long userId;

    private MessageAdapter adapter;
    private RecyclerView messagesList;
    private EditText input;
    private ImageButton btnSend;
    private TextView emptyView;
    private AvatarView avatar;
    private TextView peerName;
    private TextView peerStatus;

    private boolean loading;

    private final Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            loadMessages(false);
            messagesList.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    public static Intent intent(Context context, long peerId) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_PEER_ID, peerId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        peerId = getIntent().getLongExtra(EXTRA_PEER_ID, -1L);
        session = new SessionManager(this);
        userId = session.getUserId();
        if (peerId <= 0 || peerId == userId) {
            finish();
            return;
        }

        setContentView(R.layout.activity_chat);
        messageRepository = new MessageRepository(this);
        userRepository = new UserRepository(this);

        messagesList = findViewById(R.id.chat_messages);
        input = findViewById(R.id.chat_input);
        btnSend = findViewById(R.id.btn_send_message);
        emptyView = findViewById(R.id.chat_empty);
        avatar = findViewById(R.id.chat_avatar);
        peerName = findViewById(R.id.chat_peer_name);
        peerStatus = findViewById(R.id.chat_peer_status);

        adapter = new MessageAdapter(userId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesList.setLayoutManager(layoutManager);
        messagesList.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> send());

        loadPeer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMessages(true);
        messagesList.postDelayed(refreshTask, REFRESH_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        messagesList.removeCallbacks(refreshTask);
    }

    private void loadPeer() {
        AppExecutors.get().execute(
                () -> userRepository.findById(peerId),
                new DataCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (user == null) {
                            finish();
                            return;
                        }
                        avatar.setUser(user.fullName, user.username);
                        peerName.setText(user.fullName);
                        peerStatus.setText(getString(R.string.username_at, user.username));
                    }
                });
    }

    private void loadMessages(boolean markRead) {
        if (loading) {
            return;
        }
        loading = true;
        AppExecutors.get().execute(
                () -> {
                    if (markRead) {
                        messageRepository.markConversationRead(userId, peerId);
                    }
                    return messageRepository.getMessages(userId, peerId);
                },
                new DataCallback<List<Message>>() {
                    @Override
                    public void onSuccess(List<Message> messages) {
                        loading = false;
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        boolean atBottom = isAtBottom();
                        adapter.submitList(messages, () -> {
                            emptyView.setVisibility(messages.isEmpty()
                                    ? android.view.View.VISIBLE : android.view.View.GONE);
                            if (atBottom) {
                                scrollToBottom();
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        loading = false;
                    }
                });
    }

    private boolean isAtBottom() {
        LinearLayoutManager layoutManager =
                (LinearLayoutManager) messagesList.getLayoutManager();
        if (layoutManager == null || layoutManager.findLastVisibleItemPosition() == -1) {
            return true;
        }
        return layoutManager.findLastVisibleItemPosition()
                >= adapter.getItemCount() - 2;
    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            messagesList.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void send() {
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        if (text.isEmpty()) {
            input.setError(getString(R.string.error_message_empty));
            return;
        }
        if (text.length() > 2000) {
            input.setError(getString(R.string.error_message_too_long));
            return;
        }
        input.setError(null);
        btnSend.setEnabled(false);

        AppExecutors.get().execute(
                () -> messageRepository.send(userId, peerId, text),
                new DataCallback<Message>() {
                    @Override
                    public void onSuccess(Message message) {
                        btnSend.setEnabled(true);
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (message == null) {
                            Toast.makeText(ChatActivity.this, R.string.error_generic,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        input.setText("");
                        KeyboardUtils.hide(input);
                        loadMessages(false);
                        scrollToBottom();
                    }

                    @Override
                    public void onError(Exception e) {
                        btnSend.setEnabled(true);
                        Toast.makeText(ChatActivity.this, R.string.error_generic,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
