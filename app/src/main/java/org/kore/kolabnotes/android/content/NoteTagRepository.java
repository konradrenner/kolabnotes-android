package org.kore.kolabnotes.android.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Colors;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Tag;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by koni on 12.03.15.
 */
public class NoteTagRepository {
    private final static String NOTES_COLUMNS = "note."+DatabaseHelper.COLUMN_UID+
            ", note."+DatabaseHelper.COLUMN_PRODUCTID+
            ", note."+DatabaseHelper.COLUMN_CREATIONDATE+
            ", note."+DatabaseHelper.COLUMN_MODIFICATIONDATE+
            ", note."+DatabaseHelper.COLUMN_SUMMARY+
            ", note."+DatabaseHelper.COLUMN_DESCRIPTION+
            ", note."+DatabaseHelper.COLUMN_CLASSIFICATION+
            ", note."+DatabaseHelper.COLUMN_COLOR;

    private final static String TAG_COLUMNS = "tag."+DatabaseHelper.COLUMN_TAG_UID+
            ", tag."+DatabaseHelper.COLUMN_PRODUCTID+
            ", tag."+DatabaseHelper.COLUMN_CREATIONDATE+
            ", tag."+DatabaseHelper.COLUMN_MODIFICATIONDATE+
            ", tag."+DatabaseHelper.COLUMN_TAGNAME+
            ", tag."+DatabaseHelper.COLUMN_PRIORITY+
            ", tag."+DatabaseHelper.COLUMN_COLOR+
            ", tag."+DatabaseHelper.COLUMN_UID;

    private final static String QUERY_TAGS_WITH_NOTEID = "SELECT "+TAG_COLUMNS+" from "+DatabaseHelper.TABLE_TAGS+" tag, "+DatabaseHelper.TABLE_NOTE_TAGS+" notetags " +
            " where notetags."+DatabaseHelper.COLUMN_ACCOUNT+" = ? " +
            " and notetags."+DatabaseHelper.COLUMN_ROOT_FOLDER+" = ? " +
            " and notetags."+DatabaseHelper.COLUMN_IDNOTE+" = ? " +
            " and notetags."+DatabaseHelper.COLUMN_IDTAG+" = tag."+ DatabaseHelper.COLUMN_TAGNAME+" " +
            " and notetags."+DatabaseHelper.COLUMN_ACCOUNT+" = tag."+ DatabaseHelper.COLUMN_ACCOUNT+" "+
            " and notetags."+DatabaseHelper.COLUMN_ROOT_FOLDER+" = tag."+ DatabaseHelper.COLUMN_ROOT_FOLDER+" ";


    private final static String QUERY_NOTES = "SELECT "+NOTES_COLUMNS+" from "+DatabaseHelper.TABLE_NOTES+" note, "+DatabaseHelper.TABLE_NOTE_TAGS+" notetags " +
            " where notetags."+DatabaseHelper.COLUMN_ACCOUNT+" = ? " +
            " and notetags."+DatabaseHelper.COLUMN_ROOT_FOLDER+" = ? " +
            " and notetags."+DatabaseHelper.COLUMN_IDTAG+" = ? " +
            " and notetags."+DatabaseHelper.COLUMN_IDNOTE+" = note."+ DatabaseHelper.COLUMN_UID+" " +
            " and notetags."+DatabaseHelper.COLUMN_ACCOUNT+" = note."+ DatabaseHelper.COLUMN_ACCOUNT+" "+
            " and notetags."+DatabaseHelper.COLUMN_ROOT_FOLDER+" = note."+ DatabaseHelper.COLUMN_ROOT_FOLDER+" ";


    // Database fields
    private Context context;
    private String[] allColumns = { DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_IDTAG,
            DatabaseHelper.COLUMN_ACCOUNT,
            DatabaseHelper.COLUMN_ROOT_FOLDER,
            DatabaseHelper.COLUMN_IDNOTE};

    public NoteTagRepository(Context context) {
        this.context = context;
    }

    public void insert(String account, String rootFolder, String uidNote, String tagname) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_IDNOTE,uidNote);
        values.put(DatabaseHelper.COLUMN_IDTAG,tagname);
        values.put(DatabaseHelper.COLUMN_ROOT_FOLDER,rootFolder);
        values.put(DatabaseHelper.COLUMN_ACCOUNT, account);

        ConnectionManager.getDatabase(context).insert(DatabaseHelper.TABLE_NOTE_TAGS, null, values);
    }

    public void updateTagID(String account, String rootFolder,String oldTagName, String newTagName){
        ConnectionManager.getDatabase(context).rawQuery("UPDATE " + DatabaseHelper.TABLE_NOTE_TAGS +
                        " SET "+DatabaseHelper.COLUMN_IDTAG+" = '"+newTagName+"' " +
                        " WHERE "+DatabaseHelper.COLUMN_ACCOUNT+" = ?1 " +
                        " AND "+DatabaseHelper.COLUMN_ROOT_FOLDER+" = ?2 " +
                        " AND "+DatabaseHelper.COLUMN_IDTAG+" = ?3 ",
                new String[]{account,rootFolder,oldTagName});
    }

    public void delete(String account, String rootFolder, String uidNote, String tagname) {
        ConnectionManager.getDatabase(context).delete(DatabaseHelper.TABLE_NOTE_TAGS,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_IDNOTE + " = '" + uidNote + "' AND " +
                        DatabaseHelper.COLUMN_IDTAG + " = '" + tagname + "' ",
                null);
    }

    public void deleteWithTagName(String account, String rootFolder, String tagname) {
        ConnectionManager.getDatabase(context).delete(DatabaseHelper.TABLE_NOTE_TAGS,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_IDTAG + " = '" + tagname + "' ",
                null);
    }

    public void delete(String account, String rootFolder, String uidNote) {
        ConnectionManager.getDatabase(context).delete(DatabaseHelper.TABLE_NOTE_TAGS,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_IDNOTE + " = '" + uidNote + "' ",
                null);
    }

    public List<Tag> getTagsFor(String account,String rootFolder, String noteuid) {
        List<Tag> tags = new ArrayList<Tag>();

        Cursor cursor = ConnectionManager.getDatabase(context).rawQuery(QUERY_TAGS_WITH_NOTEID, new String[]{account, rootFolder, noteuid});

        while (cursor.moveToNext()) {
            tags.add(cursorToTag(cursor));
        }
        cursor.close();
        return tags;
    }

    public List<String> getTagNamesFor(String account,String rootFolder, String noteuid) {
        List<String> tags = new ArrayList<String>();

        Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_NOTE_TAGS,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_IDNOTE + " = '" + noteuid + "' ",
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

    void cleanAccount(String account, String rootFolder){
        ConnectionManager.getDatabase(context).delete(DatabaseHelper.TABLE_NOTE_TAGS,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' ",
                null);
    }

    public List<Note> getNotesWith(String account,String rootFolder,String tagname, NoteSorting noteSorting) {
        List<Note> notes = new ArrayList<Note>();

        Cursor cursor = ConnectionManager.getDatabase(context).rawQuery(QUERY_NOTES + " order by " + noteSorting.getColumnName() + " " + noteSorting.getDirection(), new String[]{account, rootFolder, tagname});


        while (cursor.moveToNext()) {
            notes.add(cursorToNote(cursor));
        }
        cursor.close();

        //load the tags for each note
        for(Note note : notes){
            List<Tag> tags = getTagsFor(account, rootFolder, note.getIdentification().getUid());

            if (tags != null && tags.size() > 0) {
                for(Tag tag : tags){
                    note.addCategories(tag);
                }
            }
        }

        return notes;
    }

    private String cursorToTagName(Cursor cursor) {
        return cursor.getString(1);
    }

    private Note cursorToNote(Cursor cursor) {
        String uid = cursor.getString(0);
        String productId = cursor.getString(1);
        Long creationDate = cursor.getLong(2);
        Long modificationDate = cursor.getLong(3);
        String summary = cursor.getString(4);
        String classification = cursor.getString(6);
        String color = cursor.getString(7);

        AuditInformation audit = new AuditInformation(new Timestamp(creationDate),new Timestamp(modificationDate));
        Identification ident = new Identification(uid,productId);

        Note note = new Note(ident,audit, Note.Classification.valueOf(classification),summary);
        note.setColor(Colors.getColor(color));

        return note;
    }

    private Tag cursorToTag(Cursor cursor){
        String uid = cursor.getString(0);
        String productId = cursor.getString(1);
        Long creationDate = cursor.getLong(2);
        Long modificationDate = cursor.getLong(3);
        String tagName = cursor.getString(4);
        int priority = cursor.getInt(5);
        String color = cursor.getString(6);
        String oldUID = cursor.getString(7);

        if(uid == null){
            uid = oldUID;
        }

        AuditInformation audit = new AuditInformation(new Timestamp(creationDate),new Timestamp(modificationDate));
        Identification ident = new Identification(uid,productId);

        Tag tag = new Tag(ident,audit);
        tag.setColor(Colors.getColor(color));
        tag.setName(tagName);
        tag.setPriority(priority);

        return tag;
    }
}
