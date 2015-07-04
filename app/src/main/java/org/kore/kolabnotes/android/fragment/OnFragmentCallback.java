package org.kore.kolabnotes.android.fragment;

import android.content.Intent;

/**
 * Created by koni on 26.06.15.
 */
public interface OnFragmentCallback {
    enum ResultCode{
        OK, SAVED, DELETED, CANCEL, BACK;
    }

    void fragmentFinished(Intent resultIntent,ResultCode code);

    void fileSelected();
}
