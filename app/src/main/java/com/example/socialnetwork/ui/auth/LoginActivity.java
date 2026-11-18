package com.example.socialnetwork.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
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
 * Экран входа. Если сессия уже сохранена — сразу открывает главный экран.
 */
public class LoginActivity extends AppCompatActivity {

    private SessionManager session;
    private UserRepository userRepository;

    private TextInputLayout loginLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText inputLogin;
    private TextInputEditText inputPassword;
    private MaterialButton btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        if (session.isLoggedIn()) {
            openMain();
            return;
        }
        setContentView(R.layout.activity_login);
        userRepository = new UserRepository(this);

        loginLayout = findViewById(R.id.login_layout);
        passwordLayout = findViewById(R.id.password_layout);
        inputLogin = findViewById(R.id.input_login);
        inputPassword = findViewById(R.id.input_password);
        btnLogin = findViewById(R.id.btn_login);

        btnLogin.setOnClickListener(v -> attemptLogin());
        inputPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
        findViewById(R.id.link_register).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        TextView demoHint = findViewById(R.id.demo_hint);
        demoHint.setOnClickListener(v -> {
            inputLogin.setText("anna");
            inputPassword.setText("123456");
            loginLayout.setError(null);
            passwordLayout.setError(null);
        });
    }

    private void attemptLogin() {
        String login = textOf(inputLogin);
        String password = textOf(inputPassword);
        loginLayout.setError(null);
        passwordLayout.setError(null);

        boolean valid = true;
        if (login.isEmpty()) {
            loginLayout.setError(getString(R.string.error_empty_login));
            valid = false;
        }
        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.error_empty_password));
            valid = false;
        }
        if (!valid) {
            return;
        }

        KeyboardUtils.hide(inputPassword);
        btnLogin.setEnabled(false);
        AppExecutors.get().execute(
                () -> userRepository.login(login, password),
                new DataCallback<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        btnLogin.setEnabled(true);
                        if (result.success) {
                            session.save(result.user);
                            Toast.makeText(LoginActivity.this,
                                    getString(R.string.toast_welcome, result.user.fullName),
                                    Toast.LENGTH_SHORT).show();
                            openMain();
                        } else if ("blocked".equals(result.message)) {
                            passwordLayout.setError(getString(R.string.error_user_blocked));
                        } else if ("invalid".equals(result.message)) {
                            passwordLayout.setError(getString(R.string.error_invalid_credentials));
                        } else {
                            passwordLayout.setError(getString(R.string.error_generic));
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this, R.string.error_generic,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
