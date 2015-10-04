package org.kore.kolabnotes.android.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by koni on 12.03.15.
 */
public class TagRepository {
    // Database fields
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;
    private String[] allColumns = { DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_TAGNAME};

    public TagRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void openReadonly() {
        database = dbHelper.getReadableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public boolean insert(String account, String rootFolder, String tagname, String color, int priority) {
        open();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ACCOUNT,account);
        values.put(DatabaseHelper.COLUMN_ROOT_FOLDER,rootFolder);
        values.put(DatabaseHelper.COLUMN_TAGNAME,tagname);
        values.put(DatabaseHelper.COLUMN_COLOR,color);
        values.put(DatabaseHelper.COLUMN_PRIORITY,priority);

        long rowId = database.insert(DatabaseHelper.TABLE_TAGS, null,values);
        close();
        return rowId >= 0;
    }

    public List<String> getAll() {
        openReadonly();
        List<String> tags = new ArrayList<String>();

        Cursor cursor = database.query(DatabaseHelper.TABLE_TAGS,
                allColumns,
                null,
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            tags.add(cursorToTag(cursor));
        }
        cursor.close();
        close();
        return tags;
    }

    private String cursorToTag(Cursor cursor) {
        return cursor.getString(1);
    }

}
