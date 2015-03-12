package org.kore.kolabnotes.android.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.kore.kolab.notes.Notebook;
import org.w3c.dom.Comment;

import java.sql.SQLException;
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
            DatabaseHelper.COLUMN_COMMENT };

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

        Cursor cursor = database.query(DatabaseHelper.TABLE_COMMENTS,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Notebook notebook = cursorToComment(cursor);
            notebooks.add(notebook);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return notebooks;
    }

    private Notebook cursorToComment(Cursor cursor) {
        //Comment comment = new Comment();
        //comment.setId(cursor.getLong(0));
        //comment.setComment(cursor.getString(1));
        return null;
    }
}
