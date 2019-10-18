package org.kore.kolabnotes.android.drawer;

import androidx.drawerlayout.widget.DrawerLayout;

import org.kore.kolabnotes.android.Utils;

/**
 * Created by koni on 30.05.17.
 */

public class OnNotebookItemClickedListener extends OnDrawerItemClickListener {

    public OnNotebookItemClickedListener(OnDrawerSelectionChangedListener listener, DrawerLayout layout) {
        super(listener, layout);
    }

    @Override
    protected void selectionChanged(String name) {
        Utils.setSelectedNotebookName(getContext(), name);
        Utils.setSelectedTagName(getContext(), null);
        getListener().notebookSelected(name);
    }
}
