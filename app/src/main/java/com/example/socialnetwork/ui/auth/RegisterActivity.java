package com.example.socialnetwork.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.socialnetwork.R;
import com.example.socialnetwork.data.DataCallback;
import com.example.socialnetwork.data.UserRepository;
import com.example.socialnetwork.data.model.AuthResult;
import com.example.socialnetwork.ui.main.MainActivity;
import com.example.socialnetwork.util.AppExecutors;
import com.example.socialnetwork.util.KeyboardUtils;
import com.example.socialnetwork.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Экран регистрации нового пользователя.
 */
public class RegisterActivity extends AppCompatActivity {

    private UserRepository userRepository;
    private SessionManager session;

    private TextInputLayout nameLayout;
    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmLayout;

    private TextInputEditText inputName;
    private TextInputEditText inputUsername;
    private TextInputEditText inputPassword;
    private TextInputEditText inputConfirm;
    private MaterialButton btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        userRepository = new UserRepository(this);
        session = new SessionManager(this);

        nameLayout = findViewById(R.id.name_layout);
        usernameLayout = findViewById(R.id.username_layout);
        passwordLayout = findViewById(R.id.reg_password_layout);
        confirmLayout = findViewById(R.id.confirm_layout);
        inputName = findViewById(R.id.input_name);
        inputUsername = findViewById(R.id.input_username);
        inputPassword = findViewById(R.id.input_reg_password);
        inputConfirm = findViewById(R.id.input_confirm);
        btnRegister = findViewById(R.id.btn_register);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.link_login).setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> attemptRegister());
        inputConfirm.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptRegister();
                return true;
            }
            return false;
        });
    }

    private void attemptRegister() {
        String name = textOf(inputName);
        String username = textOf(inputUsername);
        String password = textOf(inputPassword);
        String confirm = textOf(inputConfirm);

        nameLayout.setError(null);
        usernameLayout.setError(null);
        passwordLayout.setError(null);
        confirmLayout.setError(null);

        boolean valid = true;
        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.error_empty_name));
            valid = false;
        }
        if (username.length() < 3) {
            usernameLayout.setError(getString(R.string.error_short_username));
            valid = false;
        }
        if (password.length() < 6) {
            passwordLayout.setError(getString(R.string.error_short_password));
            valid = false;
        }
        if (!password.equals(confirm)) {
            confirmLayout.setError(getString(R.string.error_password_mismatch));
            valid = false;
        }
        if (!valid) {
            return;
        }

        KeyboardUtils.hide(inputConfirm);
        btnRegister.setEnabled(false);
        AppExecutors.get().execute(
                () -> userRepository.register(username, password, name),
                new DataCallback<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        btnRegister.setEnabled(true);
                        if (result.success) {
                            session.save(result.user);
                            Toast.makeText(RegisterActivity.this,
                                    R.string.toast_registered, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else if ("username_taken".equals(result.message)) {
                            usernameLayout.setError(getString(R.string.error_username_taken));
                        } else {
                            Toast.makeText(RegisterActivity.this, R.string.error_generic,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, R.string.error_generic,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private static String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
