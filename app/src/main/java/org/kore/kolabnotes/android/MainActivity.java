package org.kore.kolabnotes.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import org.kore.kolab.notes.Note;
import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.content.NotebookRepository;
import org.kore.kolabnotes.android.fragment.OverviewFragment;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

public class MainActivity extends ActionBarActivity implements SyncStatusObserver{

    public static final int DETAIL_ACTIVITY_RESULT_CODE = 1;

    public static final String AUTHORITY = "kore.kolabnotes";

    private AccountManager mAccountManager;
    private NotebookRepository notebookRepository = new NotebookRepository(this);
    private ActiveAccountRepository activeAccountRepository = new ActiveAccountRepository(this);
    private OverviewFragment overviewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        overviewFragment = (OverviewFragment)getFragmentManager().findFragmentById(R.id.overview_fragment);

        mAccountManager = AccountManager.get(this);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == DETAIL_ACTIVITY_RESULT_CODE) {
            if(resultCode == RESULT_OK || resultCode == RESULT_CANCELED){
                String nbName = data.getStringExtra("selectedNotebookName");
            }
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


    public void animateActivity(Note note) {
        Intent i = new Intent(this, DetailActivity.class);
        i.putExtra(Utils.NOTE_UID, note.getIdentification().getUid());
        ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
        String selectedNotebookName = overviewFragment.getSelectedNotebookName();
        if(selectedNotebookName != null) {
            i.putExtra(Utils.NOTEBOOK_UID, notebookRepository.getBySummary(activeAccount.getAccount(), activeAccount.getRootFolder(), selectedNotebookName).getIdentification().getUid());
        }

        startActivityForResult(i,DETAIL_ACTIVITY_RESULT_CODE);
    }

}
