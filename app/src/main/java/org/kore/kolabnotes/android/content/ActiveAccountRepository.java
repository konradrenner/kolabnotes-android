package org.kore.kolabnotes.android.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.kore.kolab.notes.Notebook;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by koni on 12.03.15.
 */
public class ActiveAccountRepository {

    // Database fields
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;
    private String[] allColumns = { DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_ACCOUNT,
            DatabaseHelper.COLUMN_ROOT_FOLDER};

    public ActiveAccountRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open(){
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void switchAccount(String account, String rootFolder){
        open();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ROOT_FOLDER, rootFolder);
        values.put(DatabaseHelper.COLUMN_ACCOUNT, account);

        database.update(DatabaseHelper.TABLE_ACTIVEACCOUNT,
                values,
                null,
                null);
        close();
    }

    public ActiveAccount getActiveAccount() {
        open();
        Cursor cursor = database.query(DatabaseHelper.TABLE_ACTIVEACCOUNT,
                allColumns,
                null,
                null,
                null,
                null,
                null);

        ActiveAccount account = null;
        if (cursor.moveToNext()) {
            account = new ActiveAccount(cursor.getString(1),cursor.getString(2));
        }
        cursor.close();
        close();
        return account;
    }
}
