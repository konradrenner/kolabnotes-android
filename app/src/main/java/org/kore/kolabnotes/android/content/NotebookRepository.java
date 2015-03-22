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
    private Context context;
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
    private ModificationRepository modificationRepository;

    public NotebookRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
        this.context = context;
        this.modificationRepository = new ModificationRepository(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void insert(Notebook notebook) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_UID, notebook.getIdentification().getUid());
        values.put(DatabaseHelper.COLUMN_PRODUCTID, notebook.getIdentification().getProductId());
        values.put(DatabaseHelper.COLUMN_CREATIONDATE, notebook.getAuditInformation().getCreationDate().getTime());
        values.put(DatabaseHelper.COLUMN_MODIFICATIONDATE, notebook.getAuditInformation().getLastModificationDate().getTime());
        values.put(DatabaseHelper.COLUMN_SUMMARY, notebook.getSummary());
        values.put(DatabaseHelper.COLUMN_DESCRIPTION, notebook.getDescription());
        values.put(DatabaseHelper.COLUMN_CLASSIFICATION, notebook.getClassification().toString());

        database.insert(DatabaseHelper.TABLE_NOTES, null,values);

        Modification modification = modificationRepository.getByUID(notebook.getIdentification().getUid());

        if(modification == null){
            modificationRepository.insert(notebook.getIdentification().getUid(), ModificationRepository.ModificationType.INS);
        }
    }

    public void update(Notebook notebook){
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_UID, notebook.getIdentification().getUid());
        values.put(DatabaseHelper.COLUMN_PRODUCTID, notebook.getIdentification().getProductId());
        values.put(DatabaseHelper.COLUMN_CREATIONDATE, notebook.getAuditInformation().getCreationDate().getTime());
        values.put(DatabaseHelper.COLUMN_MODIFICATIONDATE, notebook.getAuditInformation().getCreationDate().getTime());
        values.put(DatabaseHelper.COLUMN_SUMMARY, notebook.getSummary());
        values.put(DatabaseHelper.COLUMN_DESCRIPTION, notebook.getDescription());
        values.put(DatabaseHelper.COLUMN_CLASSIFICATION, notebook.getClassification().toString());

        database.update(DatabaseHelper.TABLE_NOTES, values,DatabaseHelper.COLUMN_UID + " = " + notebook.getIdentification().getUid(),null);

        Modification modification = modificationRepository.getByUID(notebook.getIdentification().getUid());

        if(modification == null){
            modificationRepository.insert(notebook.getIdentification().getUid(), ModificationRepository.ModificationType.UPD);
        }
    }

    public void delete(Notebook notebook) {
       database.delete(DatabaseHelper.TABLE_NOTES, DatabaseHelper.COLUMN_UID + " = " + notebook.getIdentification().getUid(), null);

        Modification modification = modificationRepository.getByUID(notebook.getIdentification().getUid());

        if(modification == null){
            modificationRepository.insert(notebook.getIdentification().getUid(), ModificationRepository.ModificationType.DEL);
        }
    }

    public List<Notebook> getAll() {
        List<Notebook> notebooks = new ArrayList<Notebook>();

        Cursor cursor = database.query(DatabaseHelper.TABLE_NOTES,
                allColumns,
                DatabaseHelper.COLUMN_DISCRIMINATOR+"="+DatabaseHelper.DESCRIMINATOR_NOTEBOOK,
                null,
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

        List<String> tags = new TagRepository(context).getTagsFor(uid);

        if(tags != null && tags.size() > 0) {
            notebook.addCategories(tags.toArray(new String[tags.size()]));
        }

        return notebook;
    }
}
