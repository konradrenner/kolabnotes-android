package org.kore.kolabnotes.android.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Note;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by koni on 12.03.15.
 */
public class NoteRepository {

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

    public NoteRepository(Context context) {
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

    public void insert(Note note) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_UID, note.getIdentification().getUid());
        values.put(DatabaseHelper.COLUMN_PRODUCTID, note.getIdentification().getProductId());
        values.put(DatabaseHelper.COLUMN_CREATIONDATE, note.getAuditInformation().getCreationDate().getTime());
        values.put(DatabaseHelper.COLUMN_MODIFICATIONDATE, note.getAuditInformation().getLastModificationDate().getTime());
        values.put(DatabaseHelper.COLUMN_SUMMARY, note.getSummary());
        values.put(DatabaseHelper.COLUMN_DESCRIPTION, note.getDescription());
        values.put(DatabaseHelper.COLUMN_CLASSIFICATION, note.getClassification().toString());

        database.insert(DatabaseHelper.TABLE_NOTES, null,values);

        Modification modification = modificationRepository.getByUID(note.getIdentification().getUid());

        if(modification == null){
            modificationRepository.insert(note.getIdentification().getUid(), ModificationRepository.ModificationType.INS);
        }
    }

    public void update(Note note){
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_UID, note.getIdentification().getUid());
        values.put(DatabaseHelper.COLUMN_PRODUCTID, note.getIdentification().getProductId());
        values.put(DatabaseHelper.COLUMN_CREATIONDATE, note.getAuditInformation().getCreationDate().getTime());
        values.put(DatabaseHelper.COLUMN_MODIFICATIONDATE, note.getAuditInformation().getCreationDate().getTime());
        values.put(DatabaseHelper.COLUMN_SUMMARY, note.getSummary());
        values.put(DatabaseHelper.COLUMN_DESCRIPTION, note.getDescription());
        values.put(DatabaseHelper.COLUMN_CLASSIFICATION, note.getClassification().toString());

        database.update(DatabaseHelper.TABLE_NOTES, values,DatabaseHelper.COLUMN_UID + " = " + note.getIdentification().getUid(),null);

        Modification modification = modificationRepository.getByUID(note.getIdentification().getUid());

        if(modification == null){
            modificationRepository.insert(note.getIdentification().getUid(), ModificationRepository.ModificationType.UPD);
        }
    }

    public void delete(Note note) {
       database.delete(DatabaseHelper.TABLE_NOTES, DatabaseHelper.COLUMN_UID + " = " + note.getIdentification().getUid(), null);

        Modification modification = modificationRepository.getByUID(note.getIdentification().getUid());

        if(modification == null){
            modificationRepository.insert(note.getIdentification().getUid(), ModificationRepository.ModificationType.DEL);
        }
    }

    public List<Note> getAll() {
        List<Note> notes = new ArrayList<Note>();

        Cursor cursor = database.query(DatabaseHelper.TABLE_NOTES,
                allColumns,
                DatabaseHelper.COLUMN_DISCRIMINATOR+"="+DatabaseHelper.DESCRIMINATOR_NOTE,
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            Note note = cursorToComment(cursor);
            notes.add(note);
        }
        cursor.close();
        return notes;
    }

    private Note cursorToComment(Cursor cursor) {
        String uid = cursor.getString(1);
        String productId = cursor.getString(2);
        Long creationDate = cursor.getLong(3);
        Long modificationDate = cursor.getLong(4);
        String summary = cursor.getString(5);
        String description = cursor.getString(6);
        String classification = cursor.getString(7);

        Note.AuditInformation audit = new Note.AuditInformation(new Timestamp(creationDate),new Timestamp(modificationDate));
        Note.Identification ident = new Note.Identification(uid,productId);

        Note note = new Note(ident,audit, Note.Classification.valueOf(classification),description);
        note.setSummary(summary);

        List<String> tags = new TagRepository(context).getTagsFor(uid);

        if(tags != null && tags.size() > 0) {
            note.addCategories(tags.toArray(new String[tags.size()]));
        }
        return note;
    }
}
