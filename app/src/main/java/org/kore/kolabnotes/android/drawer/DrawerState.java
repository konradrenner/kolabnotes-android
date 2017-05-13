package org.kore.kolabnotes.android.drawer;

import android.app.Activity;

import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by koni on 08.05.17.
 */

public class DrawerState {
    private final SortedSet<DrawerNotebookItem> notebooks;
    private final SortedSet<DrawerTagItem> tags;

    private DrawerState() {
        this.tags = new ConcurrentSkipListSet<>();
        this.notebooks = new ConcurrentSkipListSet<>();
    }



    public void syncStateWithDrawer(Activity activity){

    }
}
