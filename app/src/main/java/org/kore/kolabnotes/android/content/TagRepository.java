package org.kore.kolabnotes.android.content;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Colors;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Tag;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by koni on 12.03.15.
 */
public class TagRepository {
    // Database fields
    private String[] allColumns = { DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_ACCOUNT,
            DatabaseHelper.COLUMN_ROOT_FOLDER,
            DatabaseHelper.COLUMN_TAG_UID,
            DatabaseHelper.COLUMN_PRODUCTID,
            DatabaseHelper.COLUMN_CREATIONDATE,
            DatabaseHelper.COLUMN_MODIFICATIONDATE,
            DatabaseHelper.COLUMN_COLOR,
            DatabaseHelper.COLUMN_PRIORITY,
            DatabaseHelper.COLUMN_TAGNAME,
            DatabaseHelper.COLUMN_UID};
    private final Context context;

    public TagRepository(Context context) {
        this.context = context;
    }

    public boolean insert(String account, String rootFolder, Tag tag) {

        if(existsTagNameForAccount(account,rootFolder,tag.getName())){
            //do nothing if the tag already exists with this name, for this account
            return false;
        }

        long rowId = doInsert(account, rootFolder, tag);
        return rowId >= 0;
    }

    private long doInsert(String account, String rootFolder, Tag tag){
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ACCOUNT,account);
        values.put(DatabaseHelper.COLUMN_ROOT_FOLDER,rootFolder);
        values.put(DatabaseHelper.COLUMN_UID, UUID.randomUUID().toString());
        values.put(DatabaseHelper.COLUMN_TAG_UID,tag.getIdentification().getUid());
        values.put(DatabaseHelper.COLUMN_PRODUCTID,tag.getIdentification().getProductId());
        values.put(DatabaseHelper.COLUMN_CREATIONDATE,tag.getAuditInformation().getCreationDate().getTime());
        values.put(DatabaseHelper.COLUMN_MODIFICATIONDATE,tag.getAuditInformation().getLastModificationDate().getTime());
        values.put(DatabaseHelper.COLUMN_TAGNAME,tag.getName());
        values.put(DatabaseHelper.COLUMN_COLOR,tag.getColor() == null ? null : tag.getColor().getHexcode());
        values.put(DatabaseHelper.COLUMN_PRIORITY,tag.getPriority());

        return ConnectionManager.getDatabase(context).insert(DatabaseHelper.TABLE_TAGS, null, values);
    }

    public void update(String account, String rootFolder,Tag tag){
        Tag oldTag = getTagWithUID(account, rootFolder, tag.getIdentification().getUid());

        String tagId = tag.getIdentification().getUid();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ACCOUNT,account);
        values.put(DatabaseHelper.COLUMN_ROOT_FOLDER,rootFolder);
        values.put(DatabaseHelper.COLUMN_TAG_UID,tagId);
        values.put(DatabaseHelper.COLUMN_PRODUCTID,tag.getIdentification().getProductId());
        values.put(DatabaseHelper.COLUMN_CREATIONDATE,tag.getAuditInformation().getCreationDate().getTime());
        values.put(DatabaseHelper.COLUMN_MODIFICATIONDATE,tag.getAuditInformation().getLastModificationDate().getTime());
        values.put(DatabaseHelper.COLUMN_TAGNAME,tag.getName());
        values.put(DatabaseHelper.COLUMN_COLOR,tag.getColor() == null ? null : tag.getColor().getHexcode());
        values.put(DatabaseHelper.COLUMN_PRIORITY, tag.getPriority());

        String whereClause = DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                DatabaseHelper.COLUMN_TAG_UID + " = '" + tagId + "' ";

        ConnectionManager.getDatabase(context).update(DatabaseHelper.TABLE_TAGS,
                values,
                whereClause,
                null);

        new NoteTagRepository(context).updateTagID(account,rootFolder,oldTag.getName(),tag.getName());
    }

    public void delete(String account, String rootFolder, Tag tag) {
        ConnectionManager.getDatabase(context).delete(DatabaseHelper.TABLE_TAGS,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_TAG_UID + " = '" + tag.getIdentification().getUid() + "' ",
                null);
        new NoteTagRepository(context).deleteWithTagName(account, rootFolder, tag.getName());

        ModificationRepository modificationRepository = new ModificationRepository(context);
        Modification modification = modificationRepository.getUnique(account,rootFolder,tag.getIdentification().getUid());

        if(modification == null){
            modificationRepository.insert(account,rootFolder,tag.getIdentification().getUid(), ModificationRepository.ModificationType.DEL,tag.getName(), Modification.Descriminator.TAG);
        }
    }

    public boolean existsTagNameFor(String account, String rootFolder, String tagName){
        boolean ret = existsTagNameForAccount(account,rootFolder,tagName);
        return ret;
    }

    private boolean existsTagNameForAccount(String account, String rootFolder, String tagName){
        Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_TAGS,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_TAGNAME + " = '" + tagName + "' ",
                null,
                null,
                null,
                null);

        if(cursor.moveToNext()){
            return true;
        }

        return false;
    }

    public Tag getTagWithName(String account, String rootFolder, String tagName){
        Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_TAGS,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_TAGNAME + " = '" + tagName + "' ",
                null,
                null,
                null,
                null);

        Tag tag = null;
        if(cursor.moveToNext()){
            tag = cursorToTag(cursor);
        }
        return tag;
    }

    public Tag getTagWithUID(String account, String rootFolder, String uid){
        Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_TAGS,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_TAG_UID + " = '" + uid + "' ",
                null,
                null,
                null,
                null);

        Tag tag = null;
        if(cursor.moveToNext()){
            tag = cursorToTag(cursor);
        }

        return tag;
    }

    public List<String> getAllTagNames(String account, String rootFolder) {
        List<String> tags = new ArrayList<String>();

        Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_TAGS,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' ",
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            tags.add(cursorToTagName(cursor));
        }
        cursor.close();
        return tags;
    }

    public List<Tag> getAll(String account, String rootFolder) {
        List<Tag> tags = new ArrayList<Tag>();

        Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_TAGS,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' ",
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            tags.add(cursorToTag(cursor));
        }
        cursor.close();
        return tags;
    }

    public List<Tag> getAllModifiedAfter(String account, String rootFolder, Date date) {
        List<Tag> tags = new ArrayList<Tag>();

        Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_TAGS,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_MODIFICATIONDATE + " > " + date.getTime(),
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            tags.add(cursorToTag(cursor));
        }
        cursor.close();
        return tags;
    }

    public void cleanAccount(String account, String rootFolder){
        ConnectionManager.getDatabase(context).delete(DatabaseHelper.TABLE_TAGS,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' ",
                null);
    }

    public Map<String,Tag> getAllAsMap(String account, String rootFolder) {
        HashMap<String,Tag> tags = new HashMap<String,Tag>();

        Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_TAGS,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' ",
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            Tag tag = cursorToTag(cursor);
            tags.put(tag.getName(), tag);
        }
        cursor.close();
        return tags;
    }

    private String cursorToTagName(Cursor cursor) {
        return cursor.getString(9);
    }


    private Tag cursorToTag(Cursor cursor){
        String uid = cursor.getString(3);
        String productId = cursor.getString(4);
        Long creationDate = cursor.getLong(5);
        Long modificationDate = cursor.getLong(6);
        String color = cursor.getString(7);
        int priority = cursor.getInt(8);
        String tagName = cursor.getString(9);
        String oldUID = cursor.getString(10);

        String correctUID = uid;
        if(correctUID == null){
            correctUID = oldUID;
        }

        AuditInformation audit = new AuditInformation(new Timestamp(creationDate),new Timestamp(modificationDate));
        Identification ident = new Identification(correctUID,productId);

        Tag tag = new Tag(ident,audit);
        tag.setColor(Colors.getColor(color));
        tag.setName(tagName);
        tag.setPriority(priority);

        return tag;
    }
}
