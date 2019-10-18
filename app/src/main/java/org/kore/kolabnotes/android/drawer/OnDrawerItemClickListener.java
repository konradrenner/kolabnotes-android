package org.kore.kolabnotes.android.drawer;

import android.content.Context;
import androidx.drawerlayout.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MenuItem;

/**
 * Created by koni on 30.05.17.
 */

public abstract class OnDrawerItemClickListener implements MenuItem.OnMenuItemClickListener {

    private final OnDrawerSelectionChangedListener listener;
    private final DrawerLayout layout;

    public OnDrawerItemClickListener(OnDrawerSelectionChangedListener listener, DrawerLayout layout) {
        this.listener = listener;
        this.layout = layout;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        selectionChanged(item.getTitle().toString());
        layout.closeDrawer(Gravity.LEFT);
        return true;
    }

    public Context getContext(){
        return layout.getContext();
    }

    public OnDrawerSelectionChangedListener getListener(){
        return listener;
    }

    protected abstract void selectionChanged(String selectionName);
}
