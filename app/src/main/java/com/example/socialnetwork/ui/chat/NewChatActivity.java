package com.example.socialnetwork.ui.chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialnetwork.R;
import com.example.socialnetwork.data.DataCallback;
import com.example.socialnetwork.data.UserRepository;
import com.example.socialnetwork.data.model.User;
import com.example.socialnetwork.util.AppExecutors;
import com.example.socialnetwork.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Выбор собеседника: список пользователей с поиском по имени и логину.
 */
public class NewChatActivity extends AppCompatActivity {

    private UserRepository userRepository;
    private SessionManager session;
    private UserPickAdapter adapter;
    private TextView emptyView;
    private int requestToken = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_chat);
        userRepository = new UserRepository(this);
        session = new SessionManager(this);

        emptyView = findViewById(R.id.new_chat_empty);
        RecyclerView list = findViewById(R.id.new_chat_users);
        adapter = new UserPickAdapter(user ->
                startActivity(ChatActivity.intent(this, user.id)));
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        EditText search = findViewById(R.id.new_chat_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                loadUsers(s.toString().trim());
            }
        });

        loadUsers("");
    }

    private void loadUsers(String query) {
        requestToken++;
        final int token = requestToken;
        long userId = session.getUserId();

        AppExecutors.get().execute(
                () -> {
                    List<User> users = query.isEmpty()
                            ? userRepository.allUsers()
                            : userRepository.search(query);
                    List<User> result = new ArrayList<>();
                    for (User user : users) {
                        if (user.id != userId && !user.blocked) {
                            result.add(user);
                        }
                    }
                    return result;
                },
                new DataCallback<List<User>>() {
                    @Override
                    public void onSuccess(List<User> users) {
                        if (isFinishing() || isDestroyed() || token != requestToken) {
                            return;
                        }
                        adapter.submitList(users);
                        emptyView.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }
}
