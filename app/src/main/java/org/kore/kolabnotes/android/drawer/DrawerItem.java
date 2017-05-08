package org.kore.kolabnotes.android.drawer;

import android.graphics.drawable.Drawable;

import java.util.Optional;

/**
 * Created by koni on 08.05.17.
 */

public interface DrawerItem extends Comparable<DrawerItem>{
    String getId();
    String getDisplayText();
}