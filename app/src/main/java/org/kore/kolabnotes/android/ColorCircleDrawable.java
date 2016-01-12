package org.kore.kolabnotes.android;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Created by yaroslav on 11.01.16.
 */
public class ColorCircleDrawable extends Drawable {
    private final Paint mPaint;
    private final Paint mPaintBorder;
    private int mRadius = 0;

    public ColorCircleDrawable(final int color, final int borderColor) {
        this.mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mPaint.setColor(color);
        this.mPaint.setStyle(Paint.Style.FILL);

        this.mPaintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mPaintBorder.setColor(borderColor);
        this.mPaintBorder.setStyle(Paint.Style.STROKE);
        this.mPaintBorder.setStrokeWidth(0.5f);
    }

    @Override
    public void draw(final Canvas canvas) {
        final Rect bounds = getBounds();
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), mRadius, mPaint);
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), mRadius, mPaintBorder);
    }

    @Override
    protected void onBoundsChange(final Rect bounds) {
        super.onBoundsChange(bounds);
        mRadius = Math.min(bounds.width(), bounds.height()) / 2;
    }

    @Override
    public void setAlpha(final int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(final ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}