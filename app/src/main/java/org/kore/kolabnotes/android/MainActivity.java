package org.kore.kolabnotes.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.BaseDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.util.KeyboardUtil;

import org.kore.kolabnotes.android.content.AccountIdentifier;
import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.fragment.ChooseAccountDialogFragment;
import org.kore.kolabnotes.android.fragment.DetailFragment;
import org.kore.kolabnotes.android.fragment.OnAccountSwitchedListener;
import org.kore.kolabnotes.android.fragment.OnFragmentCallback;
import org.kore.kolabnotes.android.fragment.OverviewFragment;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MainActivity extends AppCompatActivity implements SyncStatusObserver, OnFragmentCallback, OnAccountSwitchedListener, AccountChooserActivity {

    public static final String AUTHORITY = "kore.kolabnotes";

    private AccountManager mAccountManager;
    private ActiveAccountRepository activeAccountRepository = new ActiveAccountRepository(this);
    private OverviewFragment overviewFragment;

    private Deque<OnAccountSwitchedListener> accountSwitchedListeners;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.accountSwitchedListeners = new LinkedList<>();
        overviewFragment = (OverviewFragment)getFragmentManager().findFragmentById(R.id.overview_fragment);
        this.accountSwitchedListeners.push(overviewFragment);

        mAccountManager = AccountManager.get(this);

        if(Utils.isTablet(getResources())) {
            KeyboardUtil keyboardUtil = new KeyboardUtil(this, findViewById(R.id.activity_main));
            keyboardUtil.enable();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(Utils.getReloadDataAfterDetail(this)){
            Utils.setReloadDataAfterDetail(this,false);
        }
    }

    @Override
    public void fileSelected() {
        overviewFragment.preventBlankDisplaying();
    }

    @Override
    public void fragmentFinished(Intent resultIntent, ResultCode code) {
        if(ResultCode.DELETED == code){
            Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_LONG);
            overviewFragment.displayBlankFragment();
            overviewFragment.onResume();
        }else if(ResultCode.SAVED == code){
            Toast.makeText(this, R.string.note_saved, Toast.LENGTH_LONG);
            overviewFragment.onResume();
        }else if(ResultCode.BACK == code){
            overviewFragment.onResume();
            overviewFragment.openDrawer();
        }
    }

    public void dispatchMenuEvent(MenuItem item){
        Fragment fragment = getFragmentManager().findFragmentById(R.id.details_fragment);

        if(fragment instanceof DetailFragment){
            DetailFragment detail = (DetailFragment)fragment;

            detail.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStatusChanged(int which) {
        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

        if (accounts.length <= 0) {
            return;
        }

        Account selectedAccount = null;

        ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();

        for (Account acc : accounts) {
            String email = mAccountManager.getUserData(acc, AuthenticatorActivity.KEY_EMAIL);
            String folder = mAccountManager.getUserData(acc, AuthenticatorActivity.KEY_ROOT_FOLDER);
            if (activeAccount.getAccount().equalsIgnoreCase(email) && activeAccount.getRootFolder().equalsIgnoreCase(folder)) {
                selectedAccount = acc;
                break;
            }
        }

        overviewFragment.refreshFinished(selectedAccount);
    }

    @Override
    protected void onPause() {
        super.onPause();

        final Drawer drawer = overviewFragment.getDrawer();
        final int currentSelection = drawer.getCurrentSelection();

        if(currentSelection >= drawer.getDrawerItems().size()){
            //could be a indexoutofbounds, I think it is a bug in the drawer lib :-(
            return;
        }

        final IDrawerItem drawerItem = drawer.getDrawerItems().get(currentSelection);

        if(drawerItem instanceof BaseDrawerItem){
            BaseDrawerItem selected = (BaseDrawerItem)drawerItem;

            String tag = drawerItem.getTag() == null || drawerItem.getTag().toString().trim().length() == 0 ? "ALL_NOTEBOOK" :  drawerItem.getTag().toString();

            if("NOTEBOOK".equalsIgnoreCase(tag)){
                Utils.setSelectedNotebookName(this, selected.getName());
                Utils.setSelectedTagName(this,null);
            }else if("TAG".equalsIgnoreCase(tag)){
                Utils.setSelectedNotebookName(this, null);
                Utils.setSelectedTagName(this,selected.getName());
            }else if("ALL_NOTES".equalsIgnoreCase(tag)){
                Utils.setSelectedNotebookName(this, null);
                Utils.setSelectedTagName(this,null);
            }else{
                Utils.setSelectedNotebookName(this, null);
                Utils.setSelectedTagName(this,null);
            }
        }
    }

    @Override
    public void showAccountChooseDialog() {
        FragmentManager fm = getFragmentManager();
        ChooseAccountDialogFragment chooseAccountDialog = new ChooseAccountDialogFragment();
        chooseAccountDialog.show(fm, "fragment_choose_account");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAccountSwitched(String name, AccountIdentifier accountIdentifier) {
        final Iterator<OnAccountSwitchedListener> iterator = this.accountSwitchedListeners.iterator();
        while(iterator.hasNext()){
            iterator.next().onAccountSwitched(name, accountIdentifier);
        }
    }

    @Override
    public void fragementAttached(Fragment fragment) {
        final OnAccountSwitchedListener peek = this.accountSwitchedListeners.peek();
        if(peek instanceof  DetailFragment){
            this.accountSwitchedListeners.poll();
        }

        if(fragment instanceof OnAccountSwitchedListener){
            this.accountSwitchedListeners.push((OnAccountSwitchedListener)fragment);
        }
    }
}
