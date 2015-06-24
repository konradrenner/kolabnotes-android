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
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import org.kore.kolab.notes.Note;
import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.content.NotebookRepository;
import org.kore.kolabnotes.android.fragment.OverviewFragment;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

public class MainActivity extends ActionBarActivity implements SyncStatusObserver{

    public static final int DETAIL_ACTIVITY_RESULT_CODE = 1;

    public static final String AUTHORITY = "kore.kolabnotes";

    private Drawer.Result mDrawer;
    private AccountHeader.Result mAccount;
    private AccountManager mAccountManager;
    private ActiveAccountRepository activeAccountRepository = new ActiveAccountRepository(this);
    private OverviewFragment overviewFragment;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        overviewFragment = (OverviewFragment)getFragmentManager().findFragmentById(R.id.overview_fragment);

        mAccountManager = AccountManager.get(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        Account[] accounts = mAccountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

        ProfileDrawerItem[] profiles = new ProfileDrawerItem[accounts.length+1];
        profiles[0] = new ProfileDrawerItem().withName(getResources().getString(R.string.drawer_account_local)).withTag("Notes");

        for(int i=0;i<accounts.length;i++) {
            String email = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_EMAIL);
            String name = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ACCOUNT_NAME);
            String rootFolder = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ROOT_FOLDER);

            profiles[i+1] = new ProfileDrawerItem().withName(name).withTag(rootFolder).withEmail(email);
        }

        mAccount = new AccountHeader()
                .withActivity(this)
                .withHeaderBackground(R.drawable.drawer_header_background)
                .addProfiles(profiles)
                .withOnAccountHeaderListener(new ProfileChanger())
                .build();
        mDrawer = new Drawer()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(mAccount)
                .withOnDrawerItemClickListener(overviewFragment.getDrawerItemClickedListener())
                .build();

        addDrawerStandardItems(mDrawer);

        mDrawer.setSelection(1);
    }



    public AccountHeader.Result getAccountHeader(){
        return mAccount;
    }

    public Drawer.Result getDrawer(){
        return mDrawer;
    }

    class ProfileChanger implements AccountHeader.OnAccountHeaderListener{
        @Override
        public boolean onProfileChanged(View view, IProfile profile, boolean current) {
            final ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
            String account;
            String rootFolder;
            boolean changed;
            if(profile.getEmail() == null || profile.getEmail().trim().length() == 0){
                changed = !activeAccount.getAccount().equalsIgnoreCase("local");
                account = "local";
                rootFolder = ((ProfileDrawerItem)profile).getTag().toString();
            }else{
                changed = !activeAccount.getAccount().equalsIgnoreCase(profile.getEmail());
                account = profile.getEmail();
                rootFolder = ((ProfileDrawerItem)profile).getTag().toString();
            }

            if(changed){
                new Thread(overviewFragment.createAccountChangeThread(account,rootFolder)).start();
            }

            mDrawer.closeDrawer();
            return changed;
        }
    }

    public final void addDrawerStandardItems(Drawer.Result drawer){
        drawer.getDrawerItems().add(new PrimaryDrawerItem().withName(getResources().getString(R.string.drawer_item_allaccount_notes)).withTag("ALL_NOTES").withIcon(R.drawable.ic_action_group));
        drawer.getDrawerItems().add(new SecondaryDrawerItem().withName(getResources().getString(R.string.drawer_item_allnotes)).withTag("ALL_NOTEBOOK").withIcon(R.drawable.ic_action_person));
        drawer.getDrawerItems().add(new DividerDrawerItem());
        drawer.getDrawerItems().add(new PrimaryDrawerItem().withName(getResources().getString(R.string.drawer_item_tags)).withTag("HEADING_TAG").setEnabled(false).withDisabledTextColor(R.color.material_drawer_dark_header_selection_text).withIcon(R.drawable.ic_action_labels));

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
}
