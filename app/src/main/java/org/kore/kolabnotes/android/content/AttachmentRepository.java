package org.kore.kolabnotes.android.content;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import org.kore.kolab.notes.Attachment;
import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Colors;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Tag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by koni on 12.03.15.
 */
public class AttachmentRepository {
    // Database fields
    private String[] allColumns = { DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_ACCOUNT,
            DatabaseHelper.COLUMN_ROOT_FOLDER,
            DatabaseHelper.COLUMN_IDNOTE,
            DatabaseHelper.COLUMN_IDATTACHMENT,
            DatabaseHelper.COLUMN_CREATIONDATE,
            DatabaseHelper.COLUMN_FILESIZE,
            DatabaseHelper.COLUMN_FILENAME,
            DatabaseHelper.COLUMN_MIMETYPE};
    private final Context context;

    public AttachmentRepository(Context context) {
        this.context = context;
    }



    public boolean insert(String account, String rootFolder, String noteUID,  Attachment attachment) {

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            long rowId = doInsert(account, rootFolder, noteUID, attachment);

            boolean ret = rowId >= 0;
            if (ret) {
                File filesDir = context.getFilesDir();
                File folder = new File(filesDir, account + File.separator + rootFolder + File.separator + noteUID);

                if (!folder.exists()) {
                    folder.mkdir();
                }

                ContentResolver contentResolver = context.getContentResolver();

                File file = new File(folder, attachment.getFileName());

                try(ByteArrayInputStream inputStream = new ByteArrayInputStream(attachment.getData()); OutputStream outputStream = contentResolver.openOutputStream(Uri.fromFile(file))){
                    int bytes;
                    byte[] buffer = new byte[1024];
                    while ((bytes = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytes);
                    }
                }catch (FileNotFoundException e){
                    Log.e("attachment","could not find attachement "+file+" : "+e);
                    attachment.setData(new byte[0]);
                } catch (IOException e) {
                    Log.e("attachment", "problem loading attachment " + file + " : " + e);
                }
            }
            return ret;
        }
        return false;
    }

    private long doInsert(String account, String rootFolder, String noteUID, Attachment attachment){
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ACCOUNT,account);
        values.put(DatabaseHelper.COLUMN_ROOT_FOLDER,rootFolder);
        values.put(DatabaseHelper.COLUMN_IDNOTE, noteUID);
        values.put(DatabaseHelper.COLUMN_IDATTACHMENT,attachment.getId());
        values.put(DatabaseHelper.COLUMN_FILESIZE,attachment.getData().length);
        values.put(DatabaseHelper.COLUMN_FILENAME, attachment.getFileName());
        values.put(DatabaseHelper.COLUMN_MIMETYPE, attachment.getMimeType());
        values.put(DatabaseHelper.COLUMN_CREATIONDATE, System.currentTimeMillis());

        return ConnectionManager.getDatabase(context).insert(DatabaseHelper.TABLE_ATTACHMENT, null, values);
    }

    public void delete(String account, String rootFolder, String noteUID, Attachment attachment) {
        ConnectionManager.getDatabase(context).delete(DatabaseHelper.TABLE_ATTACHMENT,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_IDNOTE + " = '" + noteUID + "' AND " +
                        DatabaseHelper.COLUMN_IDATTACHMENT + " = '" + attachment.getId() + "' ",
                null);


        File filesDir = context.getFilesDir();
        File file = new File(filesDir,account+File.separator+rootFolder+File.separator+noteUID+File.separator+attachment.getFileName());
        if(file.exists()){
            file.delete();
        }
    }

    public void deleteForNote(String account, String rootFolder, String noteUID) {
        ConnectionManager.getDatabase(context).delete(DatabaseHelper.TABLE_ATTACHMENT,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_IDNOTE + " = '" + noteUID + "' ",
                null);


        File filesDir = context.getFilesDir();
        File folder = new File(filesDir,account+File.separator+rootFolder+File.separator+noteUID);
        deleteAttachmentsFromFolder(folder);
    }


    public Attachment getAttachmentWithAttachmentID(String account, String rootFolder, String attachmentid){
        Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_ATTACHMENT,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_TAG_UID + " = '" + attachmentid + "' ",
                null,
                null,
                null,
                null);

        Attachment attachment = null;
        if(cursor.moveToNext()){
            attachment = cursorToAttachmment(cursor, true);
        }

        return attachment;
    }

    public List<Attachment> getAllForNote(String account, String rootFolder, String noteUid, boolean withFile) {
        List<Attachment> attachments = new ArrayList<Attachment>();

        Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_ATTACHMENT,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_IDNOTE + " = '" + noteUid + "' ",
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            attachments.add(cursorToAttachmment(cursor, withFile));
        }
        cursor.close();
        return attachments;
    }

    public boolean hasNoteAttachments(String account, String rootFolder, String noteUid) {
        Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_ATTACHMENT,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_IDNOTE + " = '" + noteUid + "' ",
                null,
                null,
                null,
                null);

        boolean ret = cursor.moveToNext();
        cursor.close();
        return ret;
    }

    public List<Attachment> getAllCreatedAfter(String account, String rootFolder, Date date) {
        List<Attachment> attachments = new ArrayList<Attachment>();

        Cursor cursor = ConnectionManager.getDatabase(context).query(DatabaseHelper.TABLE_ATTACHMENT,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_MODIFICATIONDATE + " > " + date.getTime(),
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            attachments.add(cursorToAttachmment(cursor, true));
        }
        cursor.close();
        return attachments;
    }

    void cleanAccount(String account, String rootFolder){
        ConnectionManager.getDatabase(context).delete(DatabaseHelper.TABLE_ATTACHMENT,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' ",
                null);

        File filesDir = context.getFilesDir();
        File directory = new File(filesDir,account+File.separator+rootFolder);

        deleteAttachmentsFromFolder(directory);
    }

    private void deleteAttachmentsFromFolder(File directory) {
        if(directory.exists()){
            String[] children = directory.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(directory, children[i]).delete();
            }

            directory.delete();
        }
    }

    private Attachment cursorToAttachmment(Cursor cursor, boolean withFile){
        String account = cursor.getString(1);
        String rootFolder = cursor.getString(2);
        String noteUID = cursor.getString(3);
        String id = cursor.getString(4);
        int filesize = cursor.getInt(6);
        String filename = cursor.getString(7);
        String mimetype = cursor.getString(8);

        Attachment attachment = new Attachment(id,filename,mimetype);

        if(withFile && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            File filesDir = context.getFilesDir();
            File file = new File(filesDir,account+File.separator+rootFolder+File.separator+noteUID+File.separator+filename);
            try(FileInputStream inputStream = new FileInputStream(file); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()){
                int bytes;
                byte[] buffer = new byte[1024];
                while ((bytes = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytes);
                }

                attachment.setData(outputStream.toByteArray());
            }catch (FileNotFoundException e){
                Log.e("attachment","could not find attachement "+file+" : "+e);
                attachment.setData(new byte[0]);
            } catch (IOException e) {
                Log.e("attachment", "problem loading attachment " + file + " : " + e);
                attachment.setData(new byte[0]);
            }
        }else{
            attachment.setData(new byte[filesize]);
        }

        return attachment;
    }
}
