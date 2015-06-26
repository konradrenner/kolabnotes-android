package org.kore.kolabnotes.android.fragment;

import android.content.Intent;

/**
 * Created by koni on 26.06.15.
 */
public interface OnFragmentFinished {
    enum ResultCode{
        OK, CANCEL;
    }

    void fragmentFinished(Intent resultIntent,ResultCode code);
}
