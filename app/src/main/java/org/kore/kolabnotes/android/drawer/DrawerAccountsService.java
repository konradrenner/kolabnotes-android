package org.kore.kolabnotes.android.drawer;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.support.design.widget.NavigationView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.TextView;

import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.content.AccountIdentifier;
import org.kore.kolabnotes.android.fragment.OnAccountSwitchedListener;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by koni on 01.12.17.
 */

public class DrawerAccountsService {

    private final NavigationView nav;
    private final View headerView;


    public DrawerAccountsService(NavigationView view) {
        this.headerView = view.getHeaderView(0);
        this.nav = view;
    }

    public void changeSelectedAccount(String name, String mail){
        TextView tname = (TextView) headerView.findViewById(R.id.drawer_header_name);
        TextView tmail = (TextView) headerView.findViewById(R.id.drawer_header_mail);

        tname.setText(name);
        if(!"local".equalsIgnoreCase(mail)){
            tmail.setText(mail);
        }
    }

    public Set<AccountIdentifier> overrideAccounts(OnAccountSwitchedListener list, Account[] accounts, AccountManager accountManager){
        Set<AccountIdentifier> createdAccounts = new LinkedHashSet<>();
        final Menu menu = nav.getMenu();
        final Context context = nav.getContext();

        for(int i=0;i<accounts.length;i++) {
            String email = accountManager.getUserData(accounts[i], AuthenticatorActivity.KEY_EMAIL);
            String name = accountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ACCOUNT_NAME);
            String rootFolder = accountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ROOT_FOLDER);
            String accountType = accountManager.getUserData(accounts[i], AuthenticatorActivity.KEY_ACCOUNT_TYPE);
            final AccountIdentifier accountIdentifier = new AccountIdentifier(email, rootFolder);

            final MenuItem accountEntry = menu.add(R.id.drawer_accounts, i, Menu.NONE, name);
            if(accountType != null) {
                int type = Integer.parseInt(accountType);

                if(type == AuthenticatorActivity.ID_ACCOUNT_TYPE_KOLABNOW){
                    accountEntry.setIcon(context.getResources().getDrawable(R.drawable.ic_kolabnow));
                }else if(type == AuthenticatorActivity.ID_ACCOUNT_TYPE_KOLAB){
                    accountEntry.setIcon(context.getResources().getDrawable(R.drawable.ic_kolab));
                }else{
                    accountEntry.setIcon(context.getResources().getDrawable(R.drawable.ic_imap));
                }
            }
            accountEntry.setOnMenuItemClickListener(new AccountSwichtedACL(name, accountIdentifier, list));
            accountEntry.setCheckable(true);
            //accountEntry.setTooltipText(email);

            createdAccounts.remove(accountIdentifier);
        }
        return createdAccounts;
    }

    public void displayAccounts(){
        nav.getMenu().setGroupVisible(R.id.drawer_accounts, true);
        nav.getMenu().setGroupVisible(R.id.drawer_navigation, false);
    }

    public void displayNavigation(){
        nav.getMenu().setGroupVisible(R.id.drawer_accounts, false);
        nav.getMenu().setGroupVisible(R.id.drawer_navigation, true);
    }

    static class AccountSwichtedACL implements MenuItem.OnMenuItemClickListener{
        private final String name;
        private final AccountIdentifier id;
        private final OnAccountSwitchedListener listener;

        AccountSwichtedACL(String name, AccountIdentifier id, OnAccountSwitchedListener list) {
            this.name = name;
            this.id = id;
            this.listener = list;
        }


        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            listener.onAccountSwitched(name, id);
            return true;
        }
    }

}
