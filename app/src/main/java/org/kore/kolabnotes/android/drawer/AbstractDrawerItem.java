package org.kore.kolabnotes.android.drawer;

import android.support.annotation.NonNull;

/**
 * Created by koni on 08.05.17.
 */

public abstract class AbstractDrawerItem implements DrawerItem {

    private final String id;
    private final String displayText;

    public AbstractDrawerItem(String id, String displayText) {
        this.id = id;
        this.displayText = displayText;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayText() {
        return displayText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o.getClass().equals(getClass()))) return false;

        AbstractDrawerItem that = (AbstractDrawerItem) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public int compareTo(@NonNull DrawerItem o) {
        if(this.equals(o)){
            return  0;
        }
        return displayText.compareTo(o.getDisplayText());
    }

    @Override
    public String toString() {
        return "AbstractDrawerItem{" +
                "id='" + id + '\'' +
                ", displayText='" + displayText + '\'' +
                '}';
    }
}
