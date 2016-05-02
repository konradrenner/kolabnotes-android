package org.kore.kolabnotes.android.content;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.kore.kolabnotes.android.security.AuthenticatorActivity;

/**
 * Created by koni on 12.03.15.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String COLUMN_ROOT_FOLDER = "rootFolder";
    public static final String COLUMN_ACCOUNT = "account";

    public static final String TABLE_ACTIVEACCOUNT = "activeaccount";
    public static final String TABLE_ACCOUNTS = "accounts";

    public static final String TABLE_NOTES = "notes";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_COLOR = "color";
    public static final String COLUMN_UID = "uid";
    public static final String COLUMN_TAG_UID = "uidtag";
    public static final String COLUMN_UID_NOTEBOOK = "uid_notebook";
    public static final String COLUMN_DISCRIMINATOR = "discriminator";
    public static final String COLUMN_PRODUCTID = "productId";
    public static final String COLUMN_CREATIONDATE = "creationDate";
    public static final String COLUMN_MODIFICATIONDATE = "lastModificationDate";
    public static final String COLUMN_CLASSIFICATION = "classification";
    public static final String COLUMN_SUMMARY = "summary";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_SHARED = "shared";
    public static final String COLUMN_MODIFICATION_ALLOWED = "mod_allowed";
    public static final String COLUMN_CREATION_ALLOWED = "create_allowed";

    public static final String DESCRIMINATOR_NOTEBOOK = "NOTEBOOK";
    public static final String DESCRIMINATOR_NOTE = "NOTE";

    public static final String TABLE_TAGS = "newtags";
    public static final String COLUMN_TAGNAME = "tagname";
    public static final String COLUMN_PRIORITY = "priority";

    public static final String TABLE_OLD_TAGS = "tags";

    public static final String TABLE_NOTE_TAGS = "notes_tags";
    public static final String COLUMN_IDNOTE = "id_note";
    public static final String COLUMN_IDTAG = "id_tag";

    public static final String TABLE_ATTACHMENT = "attachment";
    public static final String COLUMN_IDATTACHMENT = "id_attachment";
    public static final String COLUMN_FILENAME = "filetype";
    public static final String COLUMN_MIMETYPE = "mimetype";
    public static final String COLUMN_FILESIZE = "filesize";

    public static final String TABLE_MODIFICATION = "modifications";
    public static final String COLUMN_MODIFICATIONTYPE = "modificationType";

    private static final String DATABASE_NAME = "kolabnotes.db";
    private static final int DATABASE_VERSION = 7;

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
            + COLUMN_COLOR + " text, "
            + COLUMN_SUMMARY + " text not null, "
            + COLUMN_SHARED + " text, " //false or true
            + COLUMN_MODIFICATION_ALLOWED + " text, " //false or true
            + COLUMN_CREATION_ALLOWED + " text, " //false or true
            + COLUMN_DESCRIPTION + " text);";

    private static final String CREATE_TAGS = "create table "
            + TABLE_TAGS +
            "(" + COLUMN_ID+ " integer primary key autoincrement, "
            + COLUMN_ACCOUNT + " text not null, "
            + COLUMN_ROOT_FOLDER + " text not null, "
            + COLUMN_UID + " text not null unique, "
            + COLUMN_TAG_UID + " text not null, "
            + COLUMN_PRODUCTID + " text not null, "
            + COLUMN_CREATIONDATE + " integer, "
            + COLUMN_MODIFICATIONDATE + " integer, " //milliseconds
            + COLUMN_COLOR + " text, "
            + COLUMN_PRIORITY + " integer, "
            + COLUMN_TAGNAME + " text not null );";

    private static final String CREATE_TAGS_NOTES = "create table "
            + TABLE_NOTE_TAGS +
            "(" + COLUMN_ID+ " integer primary key autoincrement, "
            + COLUMN_ACCOUNT + " text not null, "
            + COLUMN_ROOT_FOLDER + " text not null, "
            + COLUMN_IDNOTE + " text not null, "
            + COLUMN_IDTAG + " text not null);";

    private static final String CREATE_ATTACHMENT = "create table "
            + TABLE_ATTACHMENT +
            "(" + COLUMN_ID+ " integer primary key autoincrement, "
            + COLUMN_ACCOUNT + " text not null, "
            + COLUMN_ROOT_FOLDER + " text not null, "
            + COLUMN_IDNOTE + " text not null, "
            + COLUMN_IDATTACHMENT + " text not null, "
            + COLUMN_CREATIONDATE + " integer, "
            + COLUMN_FILESIZE + " integer not null, "
            + COLUMN_FILENAME + " text not null, "
            + COLUMN_MIMETYPE + " text not null);";

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

    private static final String CREATE_ACCOUNTS = "create table "
            + TABLE_ACCOUNTS +
            "(" + COLUMN_ID+ " integer primary key autoincrement, "
            + COLUMN_ACCOUNT + " text not null, "
            + COLUMN_ROOT_FOLDER + " text not null );";

    private static final String INIT_ACTIVEACCOUNT = "insert into "
            + TABLE_ACTIVEACCOUNT +
            "(" + COLUMN_ACCOUNT + ", "
            + COLUMN_ROOT_FOLDER + " )" +
            "VALUES ('local','Notes');";

    private static final String INIT_ACCOUNTS = "insert into "
            + TABLE_ACCOUNTS +
            "(" + COLUMN_ACCOUNT + ", "
            + COLUMN_ROOT_FOLDER + " )" +
            "VALUES ('local','Notes');";

    private final Context context;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_NOTES);
        database.execSQL(CREATE_TAGS);
        database.execSQL(CREATE_MODIFICATION);
        database.execSQL(CREATE_TAGS_NOTES);
        database.execSQL(CREATE_ATTACHMENT);
        database.execSQL(CREATE_ACTIVEACCOUNT);
        database.execSQL(INIT_ACTIVEACCOUNT);

        database.execSQL(CREATE_ACCOUNTS);
        database.execSQL(INIT_ACCOUNTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(DatabaseHelper.class.getName(), "Upgrading database from version " + oldVersion + " to version "+ newVersion);
        if(oldVersion == 2){
            db.execSQL("ALTER TABLE "+TABLE_TAGS+" ADD COLUMN "+COLUMN_TAG_UID+" text ");
        }else if(oldVersion < 2){
            db.execSQL(CREATE_TAGS);
        }

        if(oldVersion < 4){
            db.execSQL("ALTER TABLE "+TABLE_NOTES+" ADD COLUMN "+COLUMN_SHARED+" text ");
        }

        if(oldVersion < 5){
            db.execSQL("ALTER TABLE "+TABLE_NOTES+" ADD COLUMN "+COLUMN_MODIFICATION_ALLOWED+" text ");
            db.execSQL("ALTER TABLE "+TABLE_NOTES+" ADD COLUMN "+COLUMN_CREATION_ALLOWED+" text ");
        }
        if(oldVersion < 6){
            db.execSQL(CREATE_ATTACHMENT);
        }
        if(oldVersion < 7){
            createAccountsTable(db);
        }
    }

    private void createAccountsTable(SQLiteDatabase db) {
        db.execSQL(CREATE_ACCOUNTS);
        final ActiveAccountRepository activeAccountRepository = new ActiveAccountRepository(context);
        activeAccountRepository.insertAccount(db, "local", "Notes");

        final AccountManager accountManager = AccountManager.get(context);
        final Account[] accounts = accountManager.getAccounts();

        for(Account account : accounts){
            String email = accountManager.getUserData(account, AuthenticatorActivity.KEY_EMAIL);
            String rootFolder = accountManager.getUserData(account,AuthenticatorActivity.KEY_ROOT_FOLDER);

            activeAccountRepository.insertAccount(db, email, rootFolder);
        }
    }

}