/*
 * Copyright (C) 2016 Yaroslav Pronin <proninyaroslav@mail.ru>
 *
 * This file is part of Kolab Notes.
 *
 * Kolab Notes is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kore.kolabnotes.android.draweditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.Set;
import java.util.Stack;

/**
 * Created by yaroslav on 24.03.16.
 */

public class DrawingView extends View {
    private static final String TAG_INSTANCE_STATE = "instanceState";
    private static final String TAG_LINES = "Lines";
    private static final String TAG_UNDONE_LINES = "UndoneLines";
    private static final String TAG_BITMAP = "CanvasBitmap";
    private static final String TAG_BRUSH_COLOR = "BrushColor";
    private static final String TAG_BRUSH_ALPHA = "BrushAlpha";
    private static final String TAG_BRUSH_SIZE = "BrushSize";
    private static final String TAG_SCALE_MODE = "ScaleMode";
    private static final String TAG_CANVAS_COLOR = "BackgroundColor";

    private static final int MAX_POINTERS = 10;

    public static final int DEFAULT_BRUSH_SIZE = 10;
    public static final int DEFAULT_BRUSH_ALPHA = 255;
    public static final int DEFAULT_BRUSH_COLOR = Color.BLACK;
    public static final int DEFAULT_CANVAS_COLOR = Color.WHITE;

    private DrawingListener mListener;

    private MultiPointersManager mMultiPointersManager;
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;
    private Bitmap mCanvasBitmap;
    private Canvas mCanvas;
    private int mWidth, mHeight;
    private Paint mBitmapPaint;
    private Stack<Line> mLines = new Stack<Line>();
    private Stack<Line> mUndoneLines = new Stack<Line>();
    private int mBrushColor, mBrushSize, mBrushAlpha;
    private int mCanvasColor;
    private float mScalePointX, mScalePointY;
    private boolean mScaleMode;

    public interface DrawingListener {
        void onDrawEvent();
    }

    public DrawingView(Context context) {
        this(context, null);
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mBrushColor = DEFAULT_BRUSH_COLOR;
        mBrushSize = DEFAULT_BRUSH_SIZE;
        mBrushAlpha = DEFAULT_BRUSH_ALPHA;
        mCanvasColor = DEFAULT_CANVAS_COLOR;

        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        mMultiPointersManager = new MultiPointersManager(MAX_POINTERS);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mScaleMode = false;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(TAG_INSTANCE_STATE, super.onSaveInstanceState());

        bundle.putSerializable(TAG_LINES, mLines);
        bundle.putSerializable(TAG_UNDONE_LINES, mUndoneLines);

        if (mCanvasBitmap != null && !mCanvasBitmap.isRecycled()) {
            bundle.putParcelable(TAG_BITMAP, mCanvasBitmap);
        }

        bundle.putInt(TAG_BRUSH_COLOR, mBrushColor);
        bundle.putInt(TAG_BRUSH_SIZE, mBrushSize);
        bundle.putInt(TAG_BRUSH_ALPHA, mBrushAlpha);
        bundle.putBoolean(TAG_SCALE_MODE, mScaleMode);
        bundle.putInt(TAG_CANVAS_COLOR, mCanvasColor);

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            mLines = (Stack<Line>) bundle.getSerializable(TAG_LINES);
            mUndoneLines = (Stack<Line>) bundle.getSerializable(TAG_UNDONE_LINES);
            mCanvasBitmap = bundle.getParcelable(TAG_BITMAP);
            mBrushColor = bundle.getInt(TAG_BRUSH_COLOR);
            mBrushSize = bundle.getInt(TAG_BRUSH_SIZE);
            mBrushAlpha = bundle.getInt(TAG_BRUSH_ALPHA);
            mScaleMode = bundle.getBoolean(TAG_SCALE_MODE);
            mCanvasColor = bundle.getInt(TAG_CANVAS_COLOR);

            state = bundle.getParcelable(TAG_INSTANCE_STATE);
        }

        super.onRestoreInstanceState(state);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        mHeight = View.MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mCanvasBitmap == null) {
            mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mCanvasBitmap);
            mCanvas.drawColor(mCanvasColor);
        } else {
            mCanvas = new Canvas(mCanvasBitmap);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mListener.onDrawEvent();

        canvas.save();

        canvas.scale(mScaleFactor, mScaleFactor, mScalePointX, mScalePointY);

        /* Drawing buffered image */
        if (mCanvasBitmap != null && !mCanvasBitmap.isRecycled()) {
            canvas.drawBitmap(mCanvasBitmap, 0, 0, mBitmapPaint);
        }

        Set<Integer> keys = mMultiPointersManager.getKeys();

        for (int key : keys) {
            Line line = mMultiPointersManager.getLine(key);
            Paint paint = createPaint(line.getBrushColor(), line.getBrushSize(), line.getBrushAlpha());
            if (line.isDot()) {
                canvas.drawPoint(line.getStartX(), line.getStartY(), paint);
            } else {
                canvas.drawPath(line, paint);
            }
        }

        canvas.restore();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            mScalePointX = detector.getFocusX();
            mScalePointY = detector.getFocusY();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

            invalidate();
            return true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Line line;
        int id, index;
        float x, y;

        if (mScaleMode) {
            mScaleDetector.onTouchEvent(event);
        } else {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN: {
                    index = event.getActionIndex();
                    id = event.getPointerId(index);
                    line = new Line(mBrushColor, mBrushAlpha, mBrushSize);
                    mMultiPointersManager.addLine(id, line);

                    /* Correct touch coordinates after scaling */
                    x = (event.getX(index) - mScalePointX) / mScaleFactor + mScalePointX;
                    y = (event.getY(index) - mScalePointY) / mScaleFactor + mScalePointY;

                    line.touchStart(x, y);
                    mLines.push(line);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    for (int i = 0; i < event.getPointerCount(); i++) {
                        id = event.getPointerId(i);
                        index = event.findPointerIndex(id);
                        line = mMultiPointersManager.getLine(id);
                        if (line != null) {
                            /* Correct touch coordinates after scaling */
                            x = (event.getX(index) - mScalePointX) / mScaleFactor + mScalePointX;
                            y = (event.getY(index) - mScalePointY) / mScaleFactor + mScalePointY;

                            line.touchMove(x, y);
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL: {
                    index = event.getActionIndex();
                    id = event.getPointerId(index);
                    line = mMultiPointersManager.getLine(id);
                    if (line != null) {
                        Paint paint = createPaint(line.getBrushColor(), line.getBrushSize(), line.getBrushAlpha());
                        /* Draw dot */
                        if (line.getStartX() == line.getLastX() || line.getStartY() == line.getLastY()) {
                            line.setDot(true);
                            /* Buffering painted image */
                            mCanvas.drawPoint(line.getStartX(), line.getStartY(), paint);
                        } else {
                            line.lineTo(line.getLastX(), line.getLastY());
                            mCanvas.drawPath(line, paint);
                        }
                        mMultiPointersManager.removeLine(id);
                    }
                    break;
                }
            }
            invalidate();
        }

        return true;
    }

    public void setListener(DrawingListener listener) {
        mListener = listener;
    }

    private static Paint createPaint(int color, int size, int alpha) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAlpha(alpha);
        paint.setStrokeWidth(size);

        return paint;
    }

    public void setScaleMode(boolean mode) {
        mScaleMode = mode;
    }

    public boolean getScaleMode() {
        return mScaleMode;
    }

    public Bitmap getBitmap() {
        return mCanvasBitmap;
    }

    private void redrawBitmap() {
        mCanvas.drawColor(mCanvasColor);

        for (Line line : mLines) {
            Paint paint = createPaint(line.getBrushColor(), line.getBrushSize(), line.getBrushAlpha());
            if (line.isDot()) {
                mCanvas.drawPoint(line.getStartX(), line.getStartY(), paint);
            } else {
                mCanvas.drawPath(line, paint);
            }
        }
    }

    public void setBrush(int color, int size, int alpha) {
        mBrushColor = color;
        mBrushSize = size;
        mBrushAlpha = alpha;
    }

    public void undo() {
        if (mLines.size() <= 0) {
            return;
        }

        mUndoneLines.push(mLines.pop());
        redrawBitmap();
        invalidate();
    }

    public void redo() {
        if (mUndoneLines.size() <= 0) {
            return;
        }

        mLines.push(mUndoneLines.pop());
        redrawBitmap();
        invalidate();
    }

    public int getCanvasColor() {
        return mCanvasColor;
    }

    public void setCanvasColor(int color) {
        mCanvasColor = color;
    }

    public void setBrushColor(int color) {
        mBrushColor = color;
    }

    public int getBrushColor() {
        return mBrushColor;
    }

    public void setBrushSize(int size) {
        mBrushSize = size;
    }

    public int getBrushSize() {
        return mBrushSize;
    }

    public int setBrushAlpha() {
        return mBrushAlpha;
    }

    public int getBrushAlpha() {
        return mBrushAlpha;
    }

    public void clearCanvas() {
        mCanvas.drawColor(mCanvasColor);
        mLines.clear();
        mUndoneLines.clear();
        invalidate();
    }

    public int getLinesCount() {
        return mLines.size();
    }

    public int getUndoneLinesCount() {
        return mUndoneLines.size();
    }
}
