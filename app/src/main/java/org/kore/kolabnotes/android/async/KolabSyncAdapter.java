package org.kore.kolabnotes.android.async;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.kore.kolab.notes.AccountInformation;
import org.kore.kolab.notes.RemoteNotesRepository;
import org.kore.kolab.notes.imap.ImapNotesRepository;
import org.kore.kolab.notes.v3.KolabConfigurationParserV3;
import org.kore.kolab.notes.v3.KolabNotesParserV3;
import org.kore.kolabnotes.android.MainActivity;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.Utils;
import org.kore.kolabnotes.android.content.RepositoryManager;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

import java.sql.Timestamp;
import java.util.Date;

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
        Context context = getContext();

        AccountManager accountManager = AccountManager.get(context);

        String email = accountManager.getUserData(account, AuthenticatorActivity.KEY_EMAIL);
        String accName = accountManager.getUserData(account, AuthenticatorActivity.KEY_ACCOUNT_NAME);
        String rootFolder = accountManager.getUserData(account,AuthenticatorActivity.KEY_ROOT_FOLDER);
        String url = accountManager.getUserData(account, AuthenticatorActivity.KEY_SERVER);
        String sport = accountManager.getUserData(account,AuthenticatorActivity.KEY_PORT);
        String sssl = accountManager.getUserData(account,AuthenticatorActivity.KEY_SSL);
        String skolab = accountManager.getUserData(account,AuthenticatorActivity.KEY_KOLAB);
        int port = Integer.valueOf(sport == null ? "993" : sport);
        boolean sslEnabled = sssl == null ? true : Boolean.valueOf(sssl);
        boolean kolabEnabled = skolab == null ? true : Boolean.valueOf(skolab);
        String password = accountManager.getPassword(account);

        AccountInformation.Builder builder = AccountInformation.createForHost(url).username(email).password(password).port(port);

        if(!sslEnabled){
            builder.disableSSL();
        }

        if(!kolabEnabled){
            builder.disableFolderAnnotation();
        }

        boolean doit = true;
        AccountInformation info = builder.build();
        ImapNotesRepository imapRepository = new ImapNotesRepository(new KolabNotesParserV3(), info, rootFolder, new KolabConfigurationParserV3());
        final Timestamp lastSyncTime = Utils.getLastSyncTime(context,accName);
        try {
            if(doit) {
                Log.d("syncNow","lastSyncTime:"+lastSyncTime);
                //Just load data completely, which was changed after the given date
                if(lastSyncTime == null){
                    imapRepository.refresh(new RefreshListener());
                }else{
                    imapRepository.refresh(lastSyncTime, new RefreshListener());
                }
            }
        }catch(Exception e){
            final Notification notification = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_kjots)
                    .setContentTitle(context.getResources().getString(R.string.sync_failed))
                    .setContentText(accName+" refresh failed")
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(e.toString()))
                    .build();

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1,notification);
            doit = false;
        }

        Date lastSync;
        if(lastSyncTime == null){
            lastSync = new Date(0);
        }else{
            lastSync = new Date(lastSyncTime.getTime());
        }

        RepositoryManager manager = new RepositoryManager(getContext(),imapRepository,lastSync);
        try{
            if(doit) {
                manager.sync(email, rootFolder);
            }
        }catch(Exception e){
            final Notification notification =  new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_kjots)
                    .setContentTitle(context.getResources().getString(R.string.sync_failed))
                    .setContentText(accName+" sync failed")
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(e.toString()))
                    .build();

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(2,notification);
            doit = false;
        }

        Utils.updateWidgetsForChange(getContext());

        try{
            if(doit) {
                imapRepository.merge();
                Utils.saveLastSyncTime(context,accName);
            }
        }catch(Exception e){
            final Notification notification =  new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_kjots)
                    .setContentTitle(context.getResources().getString(R.string.sync_failed))
                    .setContentText(accName+" merge failed")
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(e.toString()))
                    .build();

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(3,notification);
        }
    }

    static class RefreshListener implements RemoteNotesRepository.Listener{
        @Override
        public void onSyncUpdate(String s) {
            Log.d("onSyncUpdate","Downloaded folder:"+s);
        }
    }
}
