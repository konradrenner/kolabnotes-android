package org.kore.kolabnotes.android.fragment;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.kore.kolabnotes.android.R;

/**
 * An empty fragment
 */
public class BlankFragment extends Fragment {

    public static BlankFragment newInstance(){
        BlankFragment f = new BlankFragment();
        Bundle args = new Bundle();
        f.setArguments(args);
    return f;
    }


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_blank,
                container,
                false);
    }


}
