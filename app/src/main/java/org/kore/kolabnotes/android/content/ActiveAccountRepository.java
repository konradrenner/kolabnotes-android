package org.kore.kolabnotes.android.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
