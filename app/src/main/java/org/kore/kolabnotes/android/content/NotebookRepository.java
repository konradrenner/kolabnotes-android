package org.kore.kolabnotes.android.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.w3c.dom.Comment;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by koni on 12.03.15.
 */
public class NotebookRepository {

    // Database fields
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;
    private String[] allColumns = { DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_UID,
            DatabaseHelper.COLUMN_PRODUCTID ,
            DatabaseHelper.COLUMN_CREATIONDATE ,
            DatabaseHelper.COLUMN_MODIFICATIONDATE ,
            DatabaseHelper.COLUMN_SUMMARY ,
            DatabaseHelper.COLUMN_DESCRIPTION ,
            DatabaseHelper.COLUMN_CLASSIFICATION };

    public NotebookRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public Comment create(Notebook notebook) {
        ContentValues values = new ContentValues();
        //values.put(DatabaseHelper.COLUMN_COMMENT, notebook);
        //long insertId = database.insert(DatabaseHelper.TABLE_COMMENTS, null,
        //        values);
        //Cursor cursor = database.query(DatabaseHelper.TABLE_COMMENTS,
        //        allColumns, DatabaseHelper.COLUMN_ID + " = " + insertId, null,
        //        null, null, null);
        //cursor.moveToFirst();
        //Comment newComment = cursorToComment(cursor);
        //cursor.close();
        return null;
    }

    public void delete(Notebook notebook) {
       //long id = comment.getId();
       // System.out.println("Comment deleted with id: " + id);
       //database.delete(DatabaseHelper.TABLE_COMMENTS, DatabaseHelper.COLUMN_ID
       //        + " = " + id, null);
    }

    public List<Notebook> getAll() {
        List<Notebook> notebooks = new ArrayList<Notebook>();

        Cursor cursor = database.query(DatabaseHelper.TABLE_NOTES,
                allColumns,
                DatabaseHelper.COLUMN_DISCRIMINATOR+"=?",
                new String[]{DatabaseHelper.DESCRIMINATOR_NOTEBOOK},
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            Notebook notebook = cursorToComment(cursor);
            notebooks.add(notebook);
        }
        cursor.close();
        return notebooks;
    }

    private Notebook cursorToComment(Cursor cursor) {
        String uid = cursor.getString(1);
        String productId = cursor.getString(2);
        Long creationDate = cursor.getLong(3);
        Long modificationDate = cursor.getLong(4);
        String summary = cursor.getString(5);
        String description = cursor.getString(6);
        String classification = cursor.getString(7);

        Note.AuditInformation audit = new Note.AuditInformation(new Timestamp(creationDate),new Timestamp(modificationDate));
        Note.Identification ident = new Note.Identification(uid,productId);

        Notebook notebook = new Notebook(ident,audit, Note.Classification.valueOf(classification),description);
        notebook.setSummary(summary);
        //TODO
        notebook.addCategories(null);
        return null;
    }
}
