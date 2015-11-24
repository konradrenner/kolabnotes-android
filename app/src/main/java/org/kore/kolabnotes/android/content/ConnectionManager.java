package org.kore.kolabnotes.android.content;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by koni on 24.11.15.
 */
public final class ConnectionManager {
    private final static ConnectionManager INSTANCE = new ConnectionManager();
    private DatabaseHelper helper;

    private ConnectionManager(){
        //nothing
    }

    public static synchronized SQLiteDatabase getDatabase(Context context){
        if(INSTANCE.helper == null){
            INSTANCE.helper = new DatabaseHelper(context);
        }
        return INSTANCE.helper.getWritableDatabase();
    }
}
