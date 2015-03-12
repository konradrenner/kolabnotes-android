package org.kore.kolabnotes.android.repository;

import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;

import org.kore.kolab.notes.Notebook;

import java.util.Collections;
import java.util.List;

/**
 * Created by koni on 12.03.15.
 */
public final class NotebookContentResolver {

    private final static String BASE_URI = "content://kore.kolabnotes/notebook/";

    private NotebookContentResolver(){
        //Stateless class
    }

    public static List<Notebook> getAll(Context context, String rootFolder){
        Uri uri = Uri.parse(BASE_URI + rootFolder);
        Cursor cursor = context.getContentResolver().query(uri, new String[0], null, null, null);

        if(cursor == null){
            return Collections.emptyList();
        }
        
        while(cursor.moveToNext()){
            //TODO was machen
        }
        return null;
    }

}
