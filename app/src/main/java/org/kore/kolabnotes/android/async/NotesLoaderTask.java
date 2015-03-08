package org.kore.kolabnotes.android.async;

import android.os.AsyncTask;

import org.kore.kolab.notes.AccountInformation;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.NotesRepository;
import org.kore.kolab.notes.RemoteNotesRepository;
import org.kore.kolab.notes.imap.ImapNotesRepository;
import org.kore.kolab.notes.v3.KolabNotesParserV3;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by koni on 28.02.15.
 */
public class NotesLoaderTask extends AsyncTask<NotesRepository, Integer, RemoteNotesRepository> {

    private NotesRepository repo;

    @Override
    protected RemoteNotesRepository doInBackground(NotesRepository... params) {
        repo = params[0];
        AccountInformation info = AccountInformation.createForHost("imap.kolabserver.com").username("").password("").build();
        ImapNotesRepository imapNotesRepository = new ImapNotesRepository(repo.getNotesParser(), info, repo.getRootFolder());
        imapNotesRepository.refresh();
        return imapNotesRepository;
    }

    @Override
    protected void onPostExecute(RemoteNotesRepository result) {
        repo.trackExisitingNotebooks(result.getNotebooks());
    }
}
