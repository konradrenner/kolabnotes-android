package org.kore.kolabnotes.android.async;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import org.kore.kolab.notes.AccountInformation;
import org.kore.kolab.notes.imap.ImapNotesRepository;
import org.kore.kolab.notes.v3.KolabNotesParserV3;
import org.kore.kolabnotes.android.content.RepositoryManager;
import org.kore.kolabnotes.android.repository.NotebookContentResolver;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

import java.lang.reflect.Array;

/**
 * Created by koni on 18.04.15.
 */
public class KolabSyncAdapter extends AbstractThreadedSyncAdapter {
    // Global variables
    // Define a variable to contain a content resolver instance
    private ContentResolver mContentResolver;
    /**
     * Set up the sync adapter
     */
    public KolabSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
    }
    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public KolabSyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        syncNow(account,extras,authority,provider,syncResult);
    }

    public void syncNow(Account account, Bundle extras, SyncResult syncResult){
        syncNow(account,extras,null,null,syncResult);
    }

    public void syncNow(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult){

        AccountManager accountManager = AccountManager.get(getContext());

        String email = accountManager.getUserData(account, AuthenticatorActivity.KEY_EMAIL);
        String rootFolder = accountManager.getUserData(account,AuthenticatorActivity.KEY_ROOT_FOLDER);
        String url = accountManager.getUserData(account, AuthenticatorActivity.KEY_SERVER);
        int port = Integer.valueOf(accountManager.getUserData(account,AuthenticatorActivity.KEY_PORT));
        String password = accountManager.getPassword(account);

        AccountInformation info = AccountInformation.createForHost(url).username(email).password(password).port(port).build();
        ImapNotesRepository imapRepository = new ImapNotesRepository(new KolabNotesParserV3(), info, rootFolder);

        RepositoryManager manager = new RepositoryManager(getContext(),imapRepository);
        manager.syncFromNotesRepository();
        manager.syncFromLocalData();
    }
}
