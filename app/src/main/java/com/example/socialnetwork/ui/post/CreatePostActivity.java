package com.example.socialnetwork.ui.post;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.socialnetwork.R;
import com.example.socialnetwork.data.DataCallback;
import com.example.socialnetwork.data.PostRepository;
import com.example.socialnetwork.util.AppExecutors;
import com.example.socialnetwork.util.ImageStorage;
import com.example.socialnetwork.util.KeyboardUtils;
import com.example.socialnetwork.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Создание публикации с валидацией полей и необязательным изображением.
 */
public class CreatePostActivity extends AppCompatActivity {

    private PostRepository postRepository;
    private SessionManager session;

    private TextInputLayout titleLayout;
    private TextInputLayout contentLayout;
    private TextInputEditText inputTitle;
    private TextInputEditText inputContent;
    private Spinner categorySpinner;
    private MaterialButton btnPublish;
    private MaterialButton btnPickPhoto;
    private MaterialCardView photoCard;
    private ImageView photoPreview;
    private TextView photoHint;

    /**
     * URI выбранного изображения до сохранения публикации.
     */
    private Uri pendingImage;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    showPreview(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);
        postRepository = new PostRepository(this);
        session = new SessionManager(this);

        titleLayout = findViewById(R.id.title_layout);
        contentLayout = findViewById(R.id.content_layout);
        inputTitle = findViewById(R.id.input_title);
        inputContent = findViewById(R.id.input_content);
        categorySpinner = findViewById(R.id.category_spinner);
        btnPublish = findViewById(R.id.btn_publish);
        btnPickPhoto = findViewById(R.id.btn_pick_photo);
        photoCard = findViewById(R.id.photo_preview_card);
        photoPreview = findViewById(R.id.photo_preview);
        photoHint = findViewById(R.id.photo_hint);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.post_categories, R.layout.item_spinner);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        categorySpinner.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnPublish.setOnClickListener(v -> publish());
        btnPickPhoto.setOnClickListener(v -> pickImage.launch("image/*"));
        findViewById(R.id.btn_remove_photo).setOnClickListener(v -> clearPhoto());

        if (savedInstanceState != null) {
            String uriString = savedInstanceState.getString("image_uri");
            if (uriString != null) {
                pendingImage = Uri.parse(uriString);
                showPreview(pendingImage);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingImage != null) {
            outState.putString("image_uri", pendingImage.toString());
        }
    }

    private void showPreview(Uri uri) {
        pendingImage = uri;
        photoCard.setVisibility(View.VISIBLE);
        photoPreview.setImageDrawable(null);
        btnPickPhoto.setText(R.string.btn_change_photo);
        photoHint.setText(R.string.photo_selected);

        AppExecutors.get().execute(
                () -> ImageStorage.decodeScaled(this, uri),
                new DataCallback<android.graphics.Bitmap>() {
                    @Override
                    public void onSuccess(android.graphics.Bitmap bitmap) {
                        if (isFinishing() || isDestroyed() || bitmap == null) {
                            return;
                        }
                        if (pendingImage != null && pendingImage.equals(uri)) {
                            photoPreview.setImageBitmap(bitmap);
                        }
                    }
                });
    }

    private void clearPhoto() {
        pendingImage = null;
        photoPreview.setImageDrawable(null);
        photoCard.setVisibility(View.GONE);
        btnPickPhoto.setText(R.string.btn_add_photo);
        photoHint.setText(R.string.post_photo_hint);
    }

    private void publish() {
        String title = inputTitle.getText() == null
                ? "" : inputTitle.getText().toString().trim();
        String content = inputContent.getText() == null
                ? "" : inputContent.getText().toString().trim();

        titleLayout.setError(null);
        contentLayout.setError(null);

        boolean valid = true;
        if (title.isEmpty()) {
            titleLayout.setError(getString(R.string.error_title_required));
            valid = false;
        }
        if (content.isEmpty()) {
            contentLayout.setError(getString(R.string.error_content_required));
            valid = false;
        }
        if (!valid) {
            return;
        }

        Object selected = categorySpinner.getSelectedItem();
        String category = selected == null ? "" : selected.toString();
        long userId = session.getUserId();
        Uri image = pendingImage;

        KeyboardUtils.hide(inputContent);
        btnPublish.setEnabled(false);
        AppExecutors.get().execute(
                () -> {
                    String imagePath = null;
                    if (image != null) {
                        imagePath = ImageStorage.copyToInternal(this, image);
                        if (imagePath == null) {
                            throw new IllegalStateException("Не удалось сохранить изображение");
                        }
                    }
                    return postRepository.createPost(userId, title, content, category, imagePath);
                },
                new DataCallback<Long>() {
                    @Override
                    public void onSuccess(Long postId) {
                        btnPublish.setEnabled(true);
                        if (postId != null && postId > 0) {
                            Toast.makeText(CreatePostActivity.this,
                                    R.string.toast_post_published, Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(CreatePostActivity.this, R.string.error_generic,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        btnPublish.setEnabled(true);
                        Toast.makeText(CreatePostActivity.this, R.string.toast_photo_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
