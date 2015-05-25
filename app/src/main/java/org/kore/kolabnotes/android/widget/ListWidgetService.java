package org.kore.kolabnotes.android.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Created by koni on 25.05.15.
 */
public class ListWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListWidgetRemoteViewsFactory(this.getApplication(),intent);
    }
}
