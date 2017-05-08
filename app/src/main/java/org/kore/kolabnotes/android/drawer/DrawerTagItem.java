package org.kore.kolabnotes.android.drawer;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import org.kore.kolab.notes.Tag;
import org.kore.kolabnotes.android.ColorCircleDrawable;
import org.kore.kolabnotes.android.R;

import java.util.Optional;

/**
 * Created by koni on 08.05.17.
 */

public class DrawerTagItem extends AbstractDrawerItem {

    private final Tag tag;

    public DrawerTagItem(Tag tag) {
        super(tag.getIdentification().getUid(), tag.getName());
        this.tag = tag;
    }

    public Drawable getTagColorIcon(){
        Drawable circle = new ColorCircleDrawable(Color.WHITE, R.color.theme_selected_notes);
        if(tag.getColor() != null){
            final int color = Color.parseColor(tag.getColor().getHexcode());
            circle.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        }
        return circle;
    }

    public Tag getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return "DrawerTagItem{" +
                super.toString()+
                "tag=" + tag +
                '}';
    }
}
