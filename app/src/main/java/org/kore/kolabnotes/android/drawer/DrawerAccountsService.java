package org.kore.kolabnotes.android.drawer;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.content.AccountIdentifier;
import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.fragment.OnAccountSwitchedFromNavListener;
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

    public void changeSelectedAccount(Context context, String name, String mail, String accountType){
        TextView tname = (TextView) headerView.findViewById(R.id.drawer_header_name);
        TextView tmail = (TextView) headerView.findViewById(R.id.drawer_header_mail);
        ImageView accountImage = (ImageView) headerView.findViewById(R.id.drawer_account_image);

        tname.setText(name);
        String corrmail = mail;
        if("local".equalsIgnoreCase(mail)){
            corrmail = "";
        }
        tmail.setText(corrmail);

        final Drawable icon = evaluateIcon(context, accountType);
        if(icon != null){
            accountImage.setImageDrawable(icon);
        }else{
            accountImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_kolabnotes_breeze));
        }
    }

    public Set<AccountIdentifier> overrideAccounts(OnAccountSwitchedFromNavListener list, Account[] accounts, AccountManager accountManager, DrawerLayout layout){
        Set<AccountIdentifier> createdAccounts = new LinkedHashSet<>();
        final Menu menu = nav.getMenu();
        final Context context = nav.getContext();

        menu.removeGroup(R.id.drawer_accounts);

        createMenuItem(list, menu, context, 0, "local", context.getString(R.string.drawer_account_local), "Notes", "local", layout);

        for(int i=0;i<accounts.length;i++) {
            String email = accountManager.getUserData(accounts[i], AuthenticatorActivity.KEY_EMAIL);
            String name = accountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ACCOUNT_NAME);
            String rootFolder = accountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ROOT_FOLDER);
            String accountType = accountManager.getUserData(accounts[i], AuthenticatorActivity.KEY_ACCOUNT_TYPE);
            final AccountIdentifier accountIdentifier = createMenuItem(list, menu, context, i+1, email, name, rootFolder, accountType, layout);

            createdAccounts.remove(accountIdentifier);
        }
        return createdAccounts;
    }

    @NonNull
    private AccountIdentifier createMenuItem(OnAccountSwitchedFromNavListener list, Menu menu, Context context, int id, String email, String name, String rootFolder, String accountType, DrawerLayout layout) {
        final AccountIdentifier accountIdentifier = new AccountIdentifier(email, rootFolder);

        final MenuItem accountEntry = menu.add(R.id.drawer_accounts, id, Menu.NONE, name);
        setIcon(context, accountType, accountEntry);
        accountEntry.setOnMenuItemClickListener(new AccountSwichtedACL(nav, layout, context, name, accountIdentifier, list));
        if (!"local".equals(email) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            accountEntry.setTooltipText(email);
        }
        return accountIdentifier;
    }

    private void setIcon(Context context, String accountType, MenuItem accountEntry) {
        final Drawable icon = evaluateIcon(context, accountType);

        if(icon != null){
            accountEntry.setIcon(icon);
        }
    }

    private Drawable evaluateIcon(Context context, String accountType) {
        if(accountType == null) {
            return null;
        }

        if("local".equalsIgnoreCase(accountType)){
            return context.getResources().getDrawable(R.drawable.ic_local_account);
        }

        int type = Integer.parseInt(accountType);

        if(type == AuthenticatorActivity.ID_ACCOUNT_TYPE_KOLABNOW){
            return context.getResources().getDrawable(R.drawable.ic_kolabnow);
        }else if(type == AuthenticatorActivity.ID_ACCOUNT_TYPE_KOLAB){
            return context.getResources().getDrawable(R.drawable.ic_kolab);
        }
        return context.getResources().getDrawable(R.drawable.ic_imap);
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
        private final OnAccountSwitchedFromNavListener listener;
        private final Context context;
        private final DrawerLayout layout;
        private final NavigationView view;

        AccountSwichtedACL(NavigationView view, DrawerLayout layout, Context ctx, String name, AccountIdentifier id, OnAccountSwitchedFromNavListener list) {
            this.name = name;
            this.id = id;
            this.listener = list;
            this.context = ctx;
            this.layout = layout;
            this.view = view;
        }


        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            ActiveAccountRepository repo = new ActiveAccountRepository(context);
            final ActiveAccount activeAccount = repo.getActiveAccount();

            view.findViewById(R.id.drawer_header).callOnClick();

            layout.closeDrawer(Gravity.LEFT);

            if(!accountEquals(activeAccount)){
                listener.onAccountSwitchedFromNav(name, id);
            }
            return true;
        }

        private boolean accountEquals(ActiveAccount activeAccount) {
            return activeAccount.getAccount().equals(id.getAccount()) && activeAccount.getRootFolder().equals(id.getRootFolder());
        }
    }

}
