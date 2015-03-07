package org.kore.kolabnotes.android;

import android.os.AsyncTask;

import org.kore.kolab.notes.AccountInformation;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.RemoteNotesRepository;
import org.kore.kolab.notes.imap.ImapRepository;
import org.kore.kolab.notes.v3.KolabNotesParserV3;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by koni on 28.02.15.
 */
public class NotesLoaderTask extends AsyncTask<RemoteNotesRepository, Integer, RemoteNotesRepository> {

    @Override
    protected RemoteNotesRepository doInBackground(RemoteNotesRepository... params) {
        try {
            params[0].refresh();
        }catch(Throwable t){
            t.printStackTrace();
        }
        return params[0];
    }

    @Override
    protected void onPostExecute(RemoteNotesRepository result) {
        //nothing at the moment
    }
}
