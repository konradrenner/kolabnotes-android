package org.kore.kolabnotes.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.kore.kolabnotes.android.content.AccountIdentifier;
import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.drawer.OnAccountsArrowClicked;
import org.kore.kolabnotes.android.fragment.ChooseAccountDialogFragment;
import org.kore.kolabnotes.android.fragment.DetailFragment;
import org.kore.kolabnotes.android.fragment.OnAccountSwitchedFromNavListener;
import org.kore.kolabnotes.android.fragment.OnAccountSwitchedListener;
import org.kore.kolabnotes.android.fragment.OnFragmentCallback;
import org.kore.kolabnotes.android.fragment.OverviewFragment;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity implements SyncStatusObserver, OnFragmentCallback, OnAccountSwitchedListener, OnAccountSwitchedFromNavListener, AccountChooserActivity {

    public static final String AUTHORITY = "kore.kolabnotes";

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavigationView;
    private AccountManager mAccountManager;
    private ActiveAccountRepository activeAccountRepository = new ActiveAccountRepository(this);
    private OverviewFragment overviewFragment;

    private Deque<OnAccountSwitchedListener> accountSwitchedListeners;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initAccountSwitchedListeners();

        mAccountManager = AccountManager.get(this);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.activity_main);
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                overviewFragment.displayBlankFragment();
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        mNavigationView.getHeaderView(0).findViewById(R.id.drawer_openclose_button).setOnClickListener(new OnAccountsArrowClicked(mNavigationView));
    }

    public DrawerLayout getDrawerLayout(){
        return this.mDrawerLayout;
    }

    public NavigationView getNavigationView(){
        return this.mNavigationView;
    }

    @Override
    protected void onResume() {
        super.onResume();

        initAccountSwitchedListeners();

        if(Utils.getReloadDataAfterDetail(this)){
            Utils.setReloadDataAfterDetail(this,false);
        }
    }

    private void initAccountSwitchedListeners() {
        if(this.accountSwitchedListeners == null){
            this.accountSwitchedListeners = new LinkedList<>();
            overviewFragment = (OverviewFragment)getFragmentManager().findFragmentById(R.id.overview_fragment);
            this.accountSwitchedListeners.push(overviewFragment);
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
            mDrawerLayout.openDrawer(Gravity.LEFT);
        }
    }

    public void dispatchMenuEvent(MenuItem item){
        Fragment fragment = getFragmentManager().findFragmentById(R.id.details_fragment);

        if(fragment instanceof DetailFragment){
            DetailFragment detail = (DetailFragment)fragment;

            detail.onOptionsItemSelected(item);
        }
    }

    public void allNotesSelected(MenuItem item){
        Utils.setSelectedNotebookName(this, null);
        Utils.setSelectedTagName(this, null);

        final boolean checked = item.isChecked();
        if(!checked){
            item.setChecked(true);
            overviewFragment.allNotesSelected();
        }
        mDrawerLayout.closeDrawer(Gravity.LEFT);
    }

    public void allNotesFromAccountSelected(MenuItem item){
        Utils.setSelectedNotebookName(this, null);
        Utils.setSelectedTagName(this, null);

        final boolean checked = item.isChecked();
        if(!checked){
            item.setChecked(true);
            overviewFragment.allNotesFromAccountSelected();
        }
        mDrawerLayout.closeDrawer(Gravity.LEFT);
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
        setTitle(name);
        initAccountSwitchedListeners();
        final Iterator<OnAccountSwitchedListener> iterator = this.accountSwitchedListeners.iterator();
        while(iterator.hasNext()){
            iterator.next().onAccountSwitched(name, accountIdentifier);
        }
    }

    @Override
    public void onAccountSwitchedFromNav(String name, AccountIdentifier accountIdentifier) {
        setTitle(name);
        overviewFragment.onAccountSwitched(name, accountIdentifier);
        overviewFragment.displayBlankFragment();
    }

    @Override
    public void fragementAttached(Fragment fragment) {
        initAccountSwitchedListeners();
        final OnAccountSwitchedListener peek = this.accountSwitchedListeners.peek();
        if(peek instanceof  DetailFragment){
            this.accountSwitchedListeners.poll();
        }

        if(fragment instanceof OnAccountSwitchedListener){
            this.accountSwitchedListeners.push((OnAccountSwitchedListener)fragment);
        }
    }
}
