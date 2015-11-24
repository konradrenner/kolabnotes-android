package org.kore.kolabnotes.android.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.SharedNotebook;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by koni on 12.03.15.
 */
public class NotebookRepository {

    // Database fields
    private Context context;
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
            DatabaseHelper.COLUMN_DISCRIMINATOR,
            DatabaseHelper.COLUMN_SHARED};
    private ModificationRepository modificationRepository;

    public NotebookRepository(Context context) {
        this.context = context;
        this.modificationRepository = new ModificationRepository(context);
    }

    public boolean insert(String account, String rootFolder, Notebook note) {
        if(getBySummary(account,rootFolder,note.getSummary()) != null){
            //same logical as on Kolab-Server => don't create a new notebook if it exists with the summary
            return false;
        }

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
        values.put(DatabaseHelper.COLUMN_SHARED, Boolean.toString(note.isShared()));

        long rowId = ConnectionManager.getDatabase(context).insert(DatabaseHelper.TABLE_NOTES, null, values);

        Modification modification = modificationRepository.getUnique(account,rootFolder,note.getIdentification().getUid());

        if(modification == null){
            modificationRepository.insert(account,rootFolder,note.getIdentification().getUid(), ModificationRepository.ModificationType.INS,null, Modification.Descriminator.NOTEBOOK);
        }
        return rowId >= 0;
    }

    public void update(String account, String rootFolder,Notebook note){
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

        ConnectionManager.getDatabase(context).update(DatabaseHelper.TABLE_NOTES,
                values,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_UID + " = '" + note.getIdentification().getUid() + "' AND ",
                null);

        Modification modification = modificationRepository.getUnique(account,rootFolder,note.getIdentification().getUid());

        if(modification == null){
            modificationRepository.insert(account,rootFolder,note.getIdentification().getUid(), ModificationRepository.ModificationType.UPD,null, Modification.Descriminator.NOTEBOOK);
        }
    }

    public void delete(String account, String rootFolder,Notebook note) {
        ConnectionManager.getDatabase(context).delete(DatabaseHelper.TABLE_NOTES,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_UID_NOTEBOOK + " = '" + note.getIdentification().getUid() + "' ",
                null);

        ConnectionManager.getDatabase(context).delete(DatabaseHelper.TABLE_NOTES,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_UID + " = '" + note.getIdentification().getUid() + "' ",
                null);

        Modification modification = modificationRepository.getUnique(account,rootFolder,note.getIdentification().getUid());

        if(modification == null){
            modificationRepository.insert(account,rootFolder,note.getIdentification().getUid(), ModificationRepository.ModificationType.DEL,note.getSummary(), Modification.Descriminator.NOTEBOOK);
        }
    }

    public Notebook getByUID(String account, String rootFolder,String uid) {
        Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_NOTES,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_UID + " = '" + uid + "' AND " +
                        DatabaseHelper.COLUMN_DISCRIMINATOR + " = '" + DatabaseHelper.DESCRIMINATOR_NOTEBOOK + "' ",
                null,
                null,
                null,
                null);

        Notebook note = null;
        if (cursor.moveToNext()) {
            note = cursorToNotebook(account, rootFolder, cursor);
        }
        cursor.close();
        return note;
    }

    public Notebook getBySummary(String account, String rootFolder, String name) {

        Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_NOTES,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_SUMMARY + " = '" + name + "' AND " +
                        DatabaseHelper.COLUMN_DISCRIMINATOR + " = '" + DatabaseHelper.DESCRIMINATOR_NOTEBOOK + "' ",
                null,
                null,
                null,
                null);

        Notebook nb = null;
        if (cursor.moveToNext()) {
            nb = cursorToNotebook(account, rootFolder, cursor);
        }else{
            cursor.close();
            //try with 'Other User' added
            String withOther = "Other Users/"+name;
            cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_NOTES,
                    allColumns,
                    DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                            DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                            DatabaseHelper.COLUMN_SUMMARY + " = '" + withOther + "' AND " +
                            DatabaseHelper.COLUMN_DISCRIMINATOR + " = '" + DatabaseHelper.DESCRIMINATOR_NOTEBOOK + "' ",
                    null,
                    null,
                    null,
                    null);

            if (cursor.moveToNext()) {
                nb = cursorToNotebook(account, rootFolder, cursor);
            }
        }
        cursor.close();
        return nb;
    }

    public List<Notebook> getAll(String account, String rootFolder) {
        List<Notebook> notes = new ArrayList<Notebook>();

        Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_NOTES,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_DISCRIMINATOR + " = '" + DatabaseHelper.DESCRIMINATOR_NOTEBOOK + "' ",
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            Notebook note = cursorToNotebook(account, rootFolder, cursor);
            notes.add(note);
        }
        cursor.close();
        return notes;
    }

    private Notebook cursorToNotebook(String account, String rootFolder,Cursor cursor) {
        String uid = cursor.getString(3);
        String productId = cursor.getString(4);
        Long creationDate = cursor.getLong(5);
        Long modificationDate = cursor.getLong(6);
        String summary = cursor.getString(7);
        String description = cursor.getString(8);
        String classification = cursor.getString(9);
        boolean shared = Boolean.parseBoolean(cursor.getString(11));

        AuditInformation audit = new AuditInformation(new Timestamp(creationDate),new Timestamp(modificationDate));
        Identification ident = new Identification(uid,productId);

        Notebook notebook;
        if(shared){
            SharedNotebook nb = new SharedNotebook(ident,audit, Note.Classification.valueOf(classification),summary);
            if(nb.isGlobalShared()){
                nb.setShortName(summary);
            }else{
                //Removing of 'Other Users' saves space
                nb.setShortName(summary.substring(summary.indexOf('/')+1));
            }
            notebook = nb;
        }else{
            notebook = new Notebook(ident,audit, Note.Classification.valueOf(classification),summary);
        }
        notebook.setDescription(description);

        return notebook;
    }
}
