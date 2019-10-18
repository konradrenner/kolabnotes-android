package org.kore.kolabnotes.android.drawer;

import androidx.drawerlayout.widget.DrawerLayout;

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
