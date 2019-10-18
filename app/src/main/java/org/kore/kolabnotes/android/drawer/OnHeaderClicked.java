package org.kore.kolabnotes.android.drawer;

import android.graphics.drawable.Drawable;
import com.google.android.material.navigation.NavigationView;
import android.view.View;
import android.widget.ImageButton;

import org.kore.kolabnotes.android.R;

/**
 * Created by koni on 01.12.17.
 */

public class OnHeaderClicked implements View.OnClickListener {

    private final NavigationView navigationView;
    private final DrawerAccountsService drawerAccountsService;

    private boolean arrowDown;

    public OnHeaderClicked(NavigationView navigationView) {
        this.navigationView = navigationView;
        this.drawerAccountsService = new DrawerAccountsService(this.navigationView);
        this.arrowDown = true;
    }

    @Override
    public void onClick(View view) {
        ImageButton button = (ImageButton)view.findViewById(R.id.drawer_openclose_button);

        final Drawable drawable = button.getDrawable();

        if(arrowDown){
            button.setImageDrawable(view.getContext().getResources().getDrawable(R.drawable.ic_arrow_drop_up_white_24dp));
            drawerAccountsService.displayAccounts();
            arrowDown = false;
        }else{
            button.setImageDrawable(view.getContext().getResources().getDrawable(R.drawable.ic_arrow_drop_down_white_24dp));
            drawerAccountsService.displayNavigation();
            arrowDown = true;
        }
    }
}
