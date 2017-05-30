package org.kore.kolabnotes.android.drawer;

import android.content.Context;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MenuItem;

import org.kore.kolab.notes.Tag;
import org.kore.kolabnotes.android.Utils;

/**
 * Created by koni on 30.05.17.
 */

public class OnTagItemClickedListener extends OnDrawerItemClickListener {

    public OnTagItemClickedListener(OnDrawerSelectionChangedListener listener, DrawerLayout layout) {
        super(listener, layout);
    }

    @Override
    protected void selectionChanged(String name) {
        Utils.setSelectedNotebookName(getContext(), null);
        Utils.setSelectedTagName(getContext(), name);
        getListener().tagSelected(name);
    }
}
