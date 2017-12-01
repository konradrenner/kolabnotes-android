package org.kore.kolabnotes.android.drawer;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;

import org.kore.kolabnotes.android.R;

/**
 * Created by koni on 01.12.17.
 */

public class OnAccountsArrowClicked implements View.OnClickListener {

    private boolean arrowDown = true;

    @Override
    public void onClick(View view) {
        ImageButton button = (ImageButton)view;

        final Drawable drawable = button.getDrawable();

        if(arrowDown){
            button.setImageDrawable(view.getContext().getResources().getDrawable(R.drawable.ic_arrow_drop_up_white_24dp));
            //TODO display accounts
            arrowDown = false;
        }else{
            button.setImageDrawable(view.getContext().getResources().getDrawable(R.drawable.ic_arrow_drop_down_white_24dp));
            //TODO display navigation
            arrowDown = true;
        }
    }
}
