package org.kore.kolabnotes.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Fragment;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.mikepenz.materialdrawer.util.KeyboardUtil;

import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.fragment.DetailFragment;
import org.kore.kolabnotes.android.fragment.OnFragmentCallback;
import org.kore.kolabnotes.android.fragment.OverviewFragment;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

public class MainActivity extends AppCompatActivity implements SyncStatusObserver, OnFragmentCallback {

    public static final String AUTHORITY = "kore.kolabnotes";

    private AccountManager mAccountManager;
    private ActiveAccountRepository activeAccountRepository = new ActiveAccountRepository(this);
    private OverviewFragment overviewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        overviewFragment = (OverviewFragment)getFragmentManager().findFragmentById(R.id.overview_fragment);

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
            overviewFragment.setFromDetail();
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
            overviewFragment.setFromDetail();
            overviewFragment.onResume();
        }else if(ResultCode.SAVED == code){
            Toast.makeText(this, R.string.note_saved, Toast.LENGTH_LONG);
            overviewFragment.setFromDetail();
            overviewFragment.onResume();
        }else if(ResultCode.BACK == code){
            overviewFragment.setFromDetail();
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
            if (activeAccount.getAccount().equalsIgnoreCase(email)) {
                selectedAccount = acc;
                break;
            }
        }

        overviewFragment.refreshFinished(selectedAccount);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
