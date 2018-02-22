package org.kore.kolabnotes.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.kore.kolabnotes.android.content.AccountIdentifier;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.fragment.ChooseAccountDialogFragment;
import org.kore.kolabnotes.android.fragment.DetailFragment;
import org.kore.kolabnotes.android.fragment.OnAccountSwitchedListener;
import org.kore.kolabnotes.android.fragment.OnFragmentCallback;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

public class DetailActivity extends AppCompatActivity implements OnFragmentCallback, OnAccountSwitchedListener, AccountChooserActivity {

    private DetailFragment detailFragment;
    private Toolbar toolbar;

    private ActiveAccountRepository activeAccountRepository = new ActiveAccountRepository(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(toolbar != null){
            toolbar.setTitle("");
        }

        detailFragment = (DetailFragment)getFragmentManager().findFragmentById(R.id.detail_fragment);

        Intent startIntent = getIntent();
        String uid = startIntent.getStringExtra(Utils.NOTE_UID);
        String notebook = startIntent.getStringExtra(Utils.NOTEBOOK_UID);

        detailFragment.setStartNotebook(notebook);
        detailFragment.setStartUid(uid);

        String action = startIntent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            AccountManager accountManager = AccountManager.get(this);
            Account[] accounts = AccountManager.get(this).getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        detailFragment.save();
    }

    @Override
    public void showAccountChooseDialog() {
        FragmentManager fm = getFragmentManager();
        ChooseAccountDialogFragment chooseAccountDialog = new ChooseAccountDialogFragment();
        chooseAccountDialog.show(fm, "fragment_choose_account");
    }

    @Override
    public void onAccountSwitched(String name, AccountIdentifier accountIdentifier){
        detailFragment.onAccountSwitched(name, accountIdentifier);
    }

    @Override
    public void fragmentFinished(Intent resultIntent, ResultCode code) {
        if(ResultCode.OK == code || ResultCode.SAVED == code || ResultCode.DELETED == code){
            Utils.setReloadDataAfterDetail(this,true);
            setResult(RESULT_OK, resultIntent);
        }else{
            setResult(RESULT_CANCELED,resultIntent);
        }
        finish();
    }

    @Override
    public void fragementAttached(Fragment fragment) {
        //nothing at the moment
    }

    @Override
    public void fileSelected() {
        //nothing here
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //outState.putParcelable("appInfo", appInfo.getComponentName());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        detailFragment.onBackPressed();
    }

}
