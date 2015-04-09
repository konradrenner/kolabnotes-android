package org.kore.kolabnotes.android.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;

import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by koni on 12.03.15.
 */
public class ModificationRepository {

    public enum ModificationType{
        UPD,DEL,INS;
    }

    // Database fields
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;
    private String[] allColumns = { DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_UID,
            DatabaseHelper.COLUMN_MODIFICATIONTYPE };

    public ModificationRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open(){
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void insert(String uid, ModificationType type) {
        open();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_UID, uid);
        values.put(DatabaseHelper.COLUMN_MODIFICATIONTYPE, type.toString());

        database.insert(DatabaseHelper.TABLE_MODIFICATION, null,values);
        close();
    }

    public void deleteAll() {
       database.delete(DatabaseHelper.TABLE_MODIFICATION, null, null);
    }

    public List<Modification> getAll() {
        open();
        List<Modification> modifications = new ArrayList<Modification>();

        Cursor cursor = database.query(DatabaseHelper.TABLE_MODIFICATION,
                allColumns,
                null,
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            Modification mod = cursorToModification(cursor);
            modifications.add(mod);
        }
        cursor.close();
        close();
        return modifications;
    }

    public Modification getByUID(String uid) {
        open();
        Cursor cursor = database.query(DatabaseHelper.TABLE_MODIFICATION,
                allColumns,
                DatabaseHelper.COLUMN_UID+" = '"+uid+"' ",
                null,
                null,
                null,
                null);

        Modification mod = null;
        if (cursor.moveToNext()) {
            mod = cursorToModification(cursor);
        }
        cursor.close();
        close();
        return mod;
    }

    private Modification cursorToModification(Cursor cursor) {
        String uid = cursor.getString(1);
        ModificationRepository.ModificationType type = ModificationRepository.ModificationType.valueOf(cursor.getString(3));
        Long modDate = cursor.getLong(3);

        Modification modification = new Modification(uid, type, new Timestamp(modDate));
        return modification;
    }
}
