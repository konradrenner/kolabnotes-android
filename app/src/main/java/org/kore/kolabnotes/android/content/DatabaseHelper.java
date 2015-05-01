package org.kore.kolabnotes.android.content;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by koni on 12.03.15.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String COLUMN_ROOT_FOLDER = "rootFolder";
    public static final String COLUMN_ACCOUNT = "account";

    public static final String TABLE_ACTIVEACCOUNT = "activeaccount";

    public static final String TABLE_NOTES = "notes";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_UID = "uid";
    public static final String COLUMN_UID_NOTEBOOK = "uid_notebook";
    public static final String COLUMN_DISCRIMINATOR = "discriminator";
    public static final String COLUMN_PRODUCTID = "productId";
    public static final String COLUMN_CREATIONDATE = "creationDate";
    public static final String COLUMN_MODIFICATIONDATE = "lastModificationDate";
    public static final String COLUMN_CLASSIFICATION = "classification";
    public static final String COLUMN_SUMMARY = "summary";
    public static final String COLUMN_DESCRIPTION = "description";

    public static final String DESCRIMINATOR_NOTEBOOK = "NOTEBOOK";
    public static final String DESCRIMINATOR_NOTE = "NOTE";

    public static final String TABLE_TAGS = "tags";
    public static final String COLUMN_TAGNAME = "tagname";

    public static final String TABLE_NOTE_TAGS = "notes_tags";
    public static final String COLUMN_IDNOTE = "id_note";
    public static final String COLUMN_IDTAG = "id_tag";

    public static final String TABLE_MODIFICATION = "modifications";
    public static final String COLUMN_MODIFICATIONTYPE = "modificationType";

    private static final String DATABASE_NAME = "kolabnotes.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String CREATE_NOTES = "create table "
            + TABLE_NOTES +
            "(" + COLUMN_ID+ " integer primary key autoincrement, "
            + COLUMN_ACCOUNT + " text not null, "
            + COLUMN_ROOT_FOLDER + " text not null, "
            + COLUMN_DISCRIMINATOR + " text not null, "
            + COLUMN_UID + " text not null unique, "
            + COLUMN_PRODUCTID + " text not null, "
            + COLUMN_CREATIONDATE + " integer, "
            + COLUMN_MODIFICATIONDATE + " integer, " //milliseconds
            + COLUMN_CLASSIFICATION + " text, "
            + COLUMN_UID_NOTEBOOK + " text, "
            + COLUMN_SUMMARY + " text not null, "
            + COLUMN_DESCRIPTION + " text);";

    private static final String CREATE_TAGS = "create table "
            + TABLE_TAGS +
            "(" + COLUMN_ID+ " integer primary key autoincrement, "
            + COLUMN_TAGNAME + " text not null unique );";

    private static final String CREATE_TAGS_NOTES = "create table "
            + TABLE_NOTE_TAGS +
            "(" + COLUMN_ID+ " integer primary key autoincrement, "
            + COLUMN_ACCOUNT + " text not null, "
            + COLUMN_ROOT_FOLDER + " text not null, "
            + COLUMN_IDNOTE + " text not null, "
            + COLUMN_IDTAG + " text not null);";

    private static final String CREATE_MODIFICATION = "create table "
            + TABLE_MODIFICATION +
            "(" + COLUMN_ID+ " integer primary key autoincrement, "
            + COLUMN_ACCOUNT + " text not null, "
            + COLUMN_ROOT_FOLDER + " text not null, "
            + COLUMN_UID + " text not null unique, "
            + COLUMN_UID_NOTEBOOK + " text, "
            + COLUMN_DISCRIMINATOR + " text, "
            + COLUMN_MODIFICATIONDATE + " integer, " //milliseconds
            + COLUMN_MODIFICATIONTYPE + " text not null);";

    private static final String CREATE_ACTIVEACCOUNT = "create table "
            + TABLE_ACTIVEACCOUNT +
            "(" + COLUMN_ID+ " integer primary key autoincrement, "
            + COLUMN_ACCOUNT + " text not null, "
            + COLUMN_ROOT_FOLDER + " text not null );";

    private static final String INIT_ACTIVEACCOUNT = "insert into "
            + TABLE_ACTIVEACCOUNT +
            "(" + COLUMN_ACCOUNT + ", "
            + COLUMN_ROOT_FOLDER + " )" +
            "VALUES ('local','Notes');";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_NOTES);
        database.execSQL(CREATE_TAGS);
        database.execSQL(CREATE_MODIFICATION);
        database.execSQL(CREATE_TAGS_NOTES);
        database.execSQL(CREATE_ACTIVEACCOUNT);
        database.execSQL(INIT_ACTIVEACCOUNT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(DatabaseHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MODIFICATION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTE_TAGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TAGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACTIVEACCOUNT);
        onCreate(db);
    }

}