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

import android.graphics.Path;

import java.io.Serializable;

/**
 * Created by yaroslav on 24.03.16.
 */

/**
 * The basic model of the drawn line.
 */

public class Line extends Path implements Serializable {
    private static final float TOUCH_TOLERANCE = 4;

    private float mStartX, mStartY, mLastX, mLastY;
    private boolean mIsDot;
    private int mColor, mSize, mAlpha;

    public Line(int color, int alpha, int size) {
        mSize = size;
        mColor = color;
        mAlpha = alpha;
        mIsDot = false;
    }

    public float getStartX() {
        return mStartX;
    }

    public float getStartY() {
        return mStartY;
    }

    public float getLastX() {
        return mLastX;
    }

    public float getLastY() {
        return mLastY;
    }

    public void touchStart(float x, float y) {
        reset();
        moveTo(x, y);
        mLastX = mStartX = x;
        mLastY = mStartY = y;
    }

    public void touchMove(float x, float y) {
        float dx = Math.abs(mLastX - x);
        float dy = Math.abs(mLastY - y);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            quadTo(mLastX, mLastY, (mLastX + x) / 2, (mLastY + y) / 2);
            mLastX = x;
            mLastY = y;
        }
    }

    public boolean isDot() {
        return mIsDot;
    }

    public void setDot(boolean isDot) {
        mIsDot = isDot;
    }

    public void setBrushSize(int size) {
        mSize = size;
    }

    public int getBrushSize() {
        return mSize;
    }

    public void setBrushAlpha(int alpha) {
        mAlpha = alpha;
    }

    public int getBrushAlpha() {
        return mAlpha;
    }

    public void setBrushColor(int color) {
        mColor = color;
    }

    public int getBrushColor() {
        return mColor;
    }

    @Override
    public String toString() {
        return "Line [mLastX=" + mLastX + ", mLastY=" + mLastY + ", mStartX=" + mStartX + ", mStartY="
                + mStartY + ", mSize=" + mSize + ", mColor=" + mColor + ", mAlpha=" + mAlpha + ", mIsDot=" + mIsDot + "]";
    }
}