package org.kore.kolabnotes.android.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;

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
            DatabaseHelper.COLUMN_ACCOUNT,
            DatabaseHelper.COLUMN_ROOT_FOLDER,
            DatabaseHelper.COLUMN_UID,
            DatabaseHelper.COLUMN_PRODUCTID ,
            DatabaseHelper.COLUMN_CREATIONDATE ,
            DatabaseHelper.COLUMN_MODIFICATIONDATE ,
            DatabaseHelper.COLUMN_SUMMARY ,
            DatabaseHelper.COLUMN_DESCRIPTION ,
            DatabaseHelper.COLUMN_CLASSIFICATION,
            DatabaseHelper.COLUMN_DISCRIMINATOR };
    private ModificationRepository modificationRepository;

    public NotebookRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
        this.context = context;
        this.modificationRepository = new ModificationRepository(context);
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

    public boolean insert(String account, String rootFolder, Notebook note) {
        if(getBySummary(account,rootFolder,note.getSummary()) != null){
            //same logical as on Kolab-Server => don't create a new notebook if it exists with the summary
            return false;
        }

        open();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_DISCRIMINATOR, DatabaseHelper.DESCRIMINATOR_NOTEBOOK);
        values.put(DatabaseHelper.COLUMN_ROOT_FOLDER, rootFolder);
        values.put(DatabaseHelper.COLUMN_ACCOUNT, account);
        values.put(DatabaseHelper.COLUMN_UID, note.getIdentification().getUid());
        values.put(DatabaseHelper.COLUMN_PRODUCTID, note.getIdentification().getProductId());
        values.put(DatabaseHelper.COLUMN_CREATIONDATE, note.getAuditInformation().getCreationDate().getTime());
        values.put(DatabaseHelper.COLUMN_MODIFICATIONDATE, note.getAuditInformation().getLastModificationDate().getTime());
        values.put(DatabaseHelper.COLUMN_SUMMARY, note.getSummary());
        values.put(DatabaseHelper.COLUMN_DESCRIPTION, note.getDescription());
        values.put(DatabaseHelper.COLUMN_CLASSIFICATION, note.getClassification().toString());

        long rowId = database.insert(DatabaseHelper.TABLE_NOTES, null,values);

        Modification modification = modificationRepository.getUnique(account,rootFolder,note.getIdentification().getUid());

        if(modification == null){
            modificationRepository.insert(account,rootFolder,note.getIdentification().getUid(), ModificationRepository.ModificationType.INS,null, Modification.Descriminator.NOTEBOOK);
        }
        close();
        return rowId >= 0;
    }

    public void update(String account, String rootFolder,Notebook note){
        open();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_UID, note.getIdentification().getUid());
        values.put(DatabaseHelper.COLUMN_ROOT_FOLDER, rootFolder);
        values.put(DatabaseHelper.COLUMN_ACCOUNT, account);
        values.put(DatabaseHelper.COLUMN_PRODUCTID, note.getIdentification().getProductId());
        values.put(DatabaseHelper.COLUMN_CREATIONDATE, note.getAuditInformation().getCreationDate().getTime());
        values.put(DatabaseHelper.COLUMN_MODIFICATIONDATE, note.getAuditInformation().getLastModificationDate().getTime());
        values.put(DatabaseHelper.COLUMN_SUMMARY, note.getSummary());
        values.put(DatabaseHelper.COLUMN_DESCRIPTION, note.getDescription());
        values.put(DatabaseHelper.COLUMN_CLASSIFICATION, note.getClassification().toString());

        database.update(DatabaseHelper.TABLE_NOTES,
                values,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account+"' AND "+
                DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder+"' AND "+
                DatabaseHelper.COLUMN_UID + " = '" + note.getIdentification().getUid()+"' AND ",
                null);

        Modification modification = modificationRepository.getUnique(account,rootFolder,note.getIdentification().getUid());

        if(modification == null){
            modificationRepository.insert(account,rootFolder,note.getIdentification().getUid(), ModificationRepository.ModificationType.UPD,null, Modification.Descriminator.NOTEBOOK);
        }
        close();
    }

    public void delete(String account, String rootFolder,Notebook note) {
        open();
        database.delete(DatabaseHelper.TABLE_NOTES,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account+"' AND "+
                DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder+"' AND "+
                DatabaseHelper.COLUMN_UID + " = '" + note.getIdentification().getUid()+"' ",
                null);

        Modification modification = modificationRepository.getUnique(account,rootFolder,note.getIdentification().getUid());

        if(modification == null){
            modificationRepository.insert(account,rootFolder,note.getIdentification().getUid(), ModificationRepository.ModificationType.DEL,null, Modification.Descriminator.NOTEBOOK);
        }
        close();
    }

    public Notebook getByUID(String account, String rootFolder,String uid) {
        openReadonly();
        Cursor cursor = database.query(DatabaseHelper.TABLE_NOTES,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account+"' AND "+
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder+"' AND "+
                        DatabaseHelper.COLUMN_UID + " = '" + uid+"' AND "+
                        DatabaseHelper.COLUMN_DISCRIMINATOR+" = '"+DatabaseHelper.DESCRIMINATOR_NOTEBOOK+"' ",
                null,
                null,
                null,
                null);

        Notebook note = null;
        if (cursor.moveToNext()) {
            note = cursorToNote(account,rootFolder,cursor);
        }
        cursor.close();
        close();
        return note;
    }

    public Notebook getBySummary(String account, String rootFolder, String name) {
        openReadonly();

        Cursor cursor = database.query(DatabaseHelper.TABLE_NOTES,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account+"' AND "+
                DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder+"' AND "+
                DatabaseHelper.COLUMN_SUMMARY + " = '" + name+"' AND "+
                DatabaseHelper.COLUMN_DISCRIMINATOR+" = '"+DatabaseHelper.DESCRIMINATOR_NOTEBOOK+"' ",
                null,
                null,
                null,
                null);

        Notebook nb = null;
        if (cursor.moveToNext()) {
            nb = cursorToNote(account,rootFolder,cursor);
        }
        cursor.close();
        close();
        return nb;
    }

    public List<Notebook> getAll(String account, String rootFolder) {
        openReadonly();
        List<Notebook> notes = new ArrayList<Notebook>();

        Cursor cursor = database.query(DatabaseHelper.TABLE_NOTES,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account+"' AND "+
                DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder+"' AND "+
                DatabaseHelper.COLUMN_DISCRIMINATOR+" = '"+DatabaseHelper.DESCRIMINATOR_NOTEBOOK+"' ",
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            Notebook note = cursorToNote(account,rootFolder,cursor);
            notes.add(note);
        }
        cursor.close();
        close();
        return notes;
    }

    private Notebook cursorToNote(String account, String rootFolder,Cursor cursor) {
        String uid = cursor.getString(3);
        String productId = cursor.getString(4);
        Long creationDate = cursor.getLong(5);
        Long modificationDate = cursor.getLong(6);
        String summary = cursor.getString(7);
        String description = cursor.getString(8);
        String classification = cursor.getString(9);

        AuditInformation audit = new AuditInformation(new Timestamp(creationDate),new Timestamp(modificationDate));
        Identification ident = new Identification(uid,productId);

        Notebook note = new Notebook(ident,audit, Note.Classification.valueOf(classification),summary);
        note.setDescription(description);

        return note;
    }
}
