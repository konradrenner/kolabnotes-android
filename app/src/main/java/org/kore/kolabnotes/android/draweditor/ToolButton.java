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
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.ImageButton;

/**
 * Created by yaroslav on 25.03.16.
 */

/**
 *  Custom tool button for drawing editor. Based on the behavior of the RadioButton.
 */

public class ToolButton extends ImageButton implements Checkable {
    private boolean mChecked;
    private boolean mBroadcasting;
    private ToolButtonMode mMode;

    private OnCheckedChangeListener onCheckedChangeListener;

    public enum ToolButtonMode {
        MODE_RADIO_BUTTON, MODE_CHECK_BOX
    }

    private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };

    public ToolButton(Context context) {
        this(context, null);
    }

    public ToolButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        int[] set = {
                android.R.attr.checked,
        };

        TypedArray attr = context.obtainStyledAttributes(attrs, set);

        mMode = ToolButtonMode.MODE_RADIO_BUTTON;

        boolean checked = attr.getBoolean(0, false);
        setChecked(checked);

        attr.recycle();
    }

    public void setMode(ToolButtonMode mode) {
        mMode = mode;
    }

    public ToolButtonMode getMode() {
        return mMode;
    }

    public void toggle() {
        setChecked(!mChecked);
    }

    @Override
    public boolean performClick() {
        if (mMode == ToolButtonMode.MODE_RADIO_BUTTON) {
            setChecked(true);
        } else if (mMode == ToolButtonMode.MODE_CHECK_BOX) {
            toggle();
        }

        return super.performClick();
    }

    public boolean isChecked() {
        return mChecked;
    }

    /**
     * Changes the checked state of this button.
     */
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            refreshDrawableState();

            /* Avoid infinite recursions if setChecked() is called from a listener */
            if (mBroadcasting) {
                return;
            }

            mBroadcasting = true;
            if (onCheckedChangeListener != null) {
                onCheckedChangeListener.onCheckedChanged(this, mChecked);
            }

            mBroadcasting = false;
        }
    }

    /**
     * Register a callback to be invoked when the checked state of this button changes.
     */
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        onCheckedChangeListener = listener;
    }

    /**
     * Interface definition for a callback.
     */
    public interface OnCheckedChangeListener {
        void onCheckedChanged(ToolButton button, boolean isChecked);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }

        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    static class SavedState extends BaseSavedState {
        boolean checked;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            checked = (Boolean) in.readValue(null);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(checked);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.checked = isChecked();

        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;

        super.onRestoreInstanceState(savedState.getSuperState());
        setChecked(savedState.checked);
        requestLayout();
    }
}
