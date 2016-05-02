package org.kore.kolabnotes.android.content;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.kore.kolabnotes.android.Utils;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by koni on 12.03.15.
 */
public class ActiveAccountRepository {

    private static ActiveAccount currentActive;

    private Context context;
    private String[] allColumns = { DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_ACCOUNT,
            DatabaseHelper.COLUMN_ROOT_FOLDER};

    public ActiveAccountRepository(Context context) {
        this.context = context;
    }


    public Set<AccountIdentifier> getAllAccounts() {
        LinkedHashSet<AccountIdentifier> accounts = new LinkedHashSet<AccountIdentifier>();

        Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_ACCOUNTS,
                allColumns,
                null,
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            AccountIdentifier id = new AccountIdentifier(cursor.getString(1), cursor.getString(2));
            accounts.add(id);
        }
        cursor.close();
        return accounts;
    }

    public Set<AccountIdentifier> initAccounts() {
        LinkedHashSet<AccountIdentifier> ret = new LinkedHashSet<>();
        final SQLiteDatabase db = ConnectionManager.getDatabase(context);

        insertAccount(db, "local", "Notes");
        ret.add(new AccountIdentifier("local","Notes"));

        final AccountManager accountManager = AccountManager.get(context);
        final Account[] accounts = accountManager.getAccounts();

        for(Account account : accounts){
            String email = accountManager.getUserData(account, AuthenticatorActivity.KEY_EMAIL);
            String rootFolder = accountManager.getUserData(account,AuthenticatorActivity.KEY_ROOT_FOLDER);

            insertAccount(db, email, rootFolder);
            ret.add(new AccountIdentifier(email, rootFolder));
        }

        return ret;
    }

    public void insertAccount(String account, String rootFolder) {
        insertAccount(ConnectionManager.getDatabase(context), account, rootFolder);
    }

    public void insertAccount(SQLiteDatabase db, String account, String rootFolder) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ACCOUNT, account);
        values.put(DatabaseHelper.COLUMN_ROOT_FOLDER, rootFolder);

        db.insert(DatabaseHelper.TABLE_ACCOUNTS, null, values);
    }

    public void deleteAccount(String account, String rootFolder){
        if(Utils.isLocalAccount(account,rootFolder)){
            return;
        }

        ConnectionManager.getDatabase(context).delete(DatabaseHelper.TABLE_ACCOUNTS,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' ",
                null);

        ActiveAccount activeAccount = getActiveAccount();
        if(activeAccount.getAccount().equals(account) && activeAccount.getRootFolder().equals(rootFolder)){
            switchAccount("local", "Notes");
        }
    }

    public synchronized ActiveAccount switchAccount(String account, String rootFolder){
        final ActiveAccount toCheck = getActiveAccount();
        ActiveAccount newActive = new ActiveAccount(account,rootFolder);

        if(newActive.equals(toCheck)){
            currentActive = newActive;
            return currentActive;
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ROOT_FOLDER, rootFolder);
        values.put(DatabaseHelper.COLUMN_ACCOUNT, account);

        int affectedRows = ConnectionManager.getDatabase(context).update(DatabaseHelper.TABLE_ACTIVEACCOUNT,
                values,
                null,
                null);

        currentActive = newActive;
        return currentActive;
    }

    public synchronized ActiveAccount getActiveAccount() {
        if(currentActive == null) {
            Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_ACTIVEACCOUNT,
                    allColumns,
                    null,
                    null,
                    null,
                    null,
                    null);

            if (cursor.moveToNext()) {
                currentActive = new ActiveAccount(cursor.getString(1), cursor.getString(2));
            }
            cursor.close();
        }
        return currentActive;
    }
}
