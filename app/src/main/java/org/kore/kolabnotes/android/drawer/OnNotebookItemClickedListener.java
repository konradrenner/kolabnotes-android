package org.kore.kolabnotes.android.drawer;

import android.content.Context;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;

import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.Tag;
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
