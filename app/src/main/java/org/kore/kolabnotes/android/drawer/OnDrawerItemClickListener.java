package org.kore.kolabnotes.android.drawer;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MenuItem;

import org.kore.kolabnotes.android.Utils;

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
        final boolean checked = item.isChecked();
        if(!checked){
            item.setChecked(true);
            selectionChanged(item.getTitle().toString());
        }
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
