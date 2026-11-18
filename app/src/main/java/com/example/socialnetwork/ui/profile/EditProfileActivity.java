package com.example.socialnetwork.ui.profile;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.socialnetwork.R;
import com.example.socialnetwork.data.DataCallback;
import com.example.socialnetwork.data.UserRepository;
import com.example.socialnetwork.data.model.User;
import com.example.socialnetwork.util.AppExecutors;
import com.example.socialnetwork.util.KeyboardUtils;
import com.example.socialnetwork.util.SessionManager;
import com.example.socialnetwork.widget.AvatarView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Редактирование профиля: имя и описание. Логин не меняется.
 */
public class EditProfileActivity extends AppCompatActivity {

    private UserRepository userRepository;
    private SessionManager session;

    private AvatarView avatar;
    private TextView editUsername;
    private TextInputLayout nameLayout;
    private TextInputEditText inputName;
    private TextInputEditText inputBio;
    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        userRepository = new UserRepository(this);
        session = new SessionManager(this);

        avatar = findViewById(R.id.edit_avatar);
        editUsername = findViewById(R.id.edit_username);
        nameLayout = findViewById(R.id.name_layout);
        inputName = findViewById(R.id.input_name);
        inputBio = findViewById(R.id.input_bio);
        btnSave = findViewById(R.id.btn_save);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> save());

        loadUser();
    }

    private void loadUser() {
        AppExecutors.get().execute(
                () -> userRepository.findById(session.getUserId()),
                new DataCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        if (user == null || isFinishing()) {
                            finish();
                            return;
                        }
                        avatar.setUser(user.fullName, user.username);
                        editUsername.setText(getString(R.string.username_at, user.username));
                        inputName.setText(user.fullName);
                        inputBio.setText(user.bio);
                    }
                });
    }

    private void save() {
        String fullName = inputName.getText() == null
                ? "" : inputName.getText().toString().trim();
        String bio = inputBio.getText() == null
                ? "" : inputBio.getText().toString().trim();

        nameLayout.setError(null);
        if (fullName.isEmpty()) {
            nameLayout.setError(getString(R.string.error_empty_name));
            return;
        }

        KeyboardUtils.hide(inputBio);
        btnSave.setEnabled(false);
        AppExecutors.get().execute(
                () -> {
                    userRepository.updateProfile(session.getUserId(), fullName, bio);
                    return Boolean.TRUE;
                },
                new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        session.updateFullName(fullName);
                        Toast.makeText(EditProfileActivity.this,
                                R.string.toast_profile_saved, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    public void onError(Exception e) {
                        btnSave.setEnabled(true);
                        Toast.makeText(EditProfileActivity.this, R.string.error_generic,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
