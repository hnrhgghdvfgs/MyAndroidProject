package com.example.socialnetwork.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.socialnetwork.util.AvatarUtils;

/**
 * Круглый аватар с инициалами пользователя. Цвет вычисляется по логину.
 */
public class AvatarView extends View {

    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private String initials = "?";
    private int circleColor = AvatarUtils.PALETTE[0];

    public AvatarView(Context context) {
        this(context, null);
    }

    public AvatarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AvatarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setUser(String fullName, String username) {
        this.initials = AvatarUtils.initialsFor(fullName, username);
        this.circleColor = AvatarUtils.colorFor(username);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int defaultSize = (int) (48 * getResources().getDisplayMetrics().density);
        int width = resolveSize(defaultSize, widthMeasureSpec);
        int height = resolveSize(defaultSize, heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float radius = Math.min(getWidth(), getHeight()) / 2f;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        circlePaint.setColor(circleColor);
        circlePaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, radius, circlePaint);

        float desired = radius * 0.95f;
        textPaint.setTextSize(desired);
        float maxWidth = radius * 1.15f;
        float textWidth = textPaint.measureText(initials);
        if (textWidth > maxWidth && textWidth > 0) {
            textPaint.setTextSize(desired * maxWidth / textWidth);
        }

        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = cy - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(initials, cx, baseline, textPaint);
    }
}
