package com.example.socialnetwork.util;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * Вспомогательные методы для клавиатуры.
 */
public final class KeyboardUtils {

    private KeyboardUtils() {
    }

    public static void hide(View view) {
        if (view == null) {
            return;
        }
        Context context = view.getContext();
        InputMethodManager imm =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
