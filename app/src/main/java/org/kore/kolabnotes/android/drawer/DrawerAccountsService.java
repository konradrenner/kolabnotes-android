package org.kore.kolabnotes.android.drawer;

import android.support.design.widget.NavigationView;
import android.view.View;
import android.widget.TextView;

import org.kore.kolabnotes.android.R;

/**
 * Created by koni on 01.12.17.
 */

public class DrawerAccountsService {

    private final View headerView;

    public DrawerAccountsService(NavigationView view) {
        this(view.getHeaderView(0));
    }

    DrawerAccountsService(View view) {
        this.headerView = view;
    }

    public void changeSelectedAccount(String name, String mail){
        TextView tname = (TextView) headerView.findViewById(R.id.drawer_header_name);
        TextView tmail = (TextView) headerView.findViewById(R.id.drawer_header_mail);

        tname.setText(name);
        if(!"local".equalsIgnoreCase(mail)){
            tmail.setText(mail);
        }
    }
}
