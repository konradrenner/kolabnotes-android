package org.kore.kolabnotes.android.repository;

import org.kore.kolab.notes.AccountInformation;
import org.kore.kolab.notes.NotesRepository;
import org.kore.kolab.notes.RemoteNotesRepository;
import org.kore.kolab.notes.imap.ImapRepository;
import org.kore.kolab.notes.v3.KolabNotesParserV3;
import org.kore.kolabnotes.android.NotesLoaderTask;

/**
 * Created by koni on 07.03.15.
 */
public class LocalRepository {
    private final static LocalRepository INSTANCE = new LocalRepository();

    private final RemoteNotesRepository repository;
    private String selectedNotebook;
    private String selectedNote;

    private LocalRepository(){
        AccountInformation info = AccountInformation.createForHost("imap.kolabserver.com").username("").password("").build();
        repository = new ImapRepository(new KolabNotesParserV3(), info, "Notes");

        new NotesLoaderTask().execute(repository);
    }

    public static final LocalRepository getInstance(){
        return INSTANCE;
    }

    public NotesRepository getRepositoryData(){
        return repository;
    }

    public void setSelectedNotebook(String uid){
        this.selectedNotebook = uid;
    }

    public void setSelectedNote(String uid){
        this.selectedNote = uid;
    }

    public String getSelectedNotebook() {
        return selectedNotebook;
    }

    public String getSelectedNote() {
        return selectedNote;
    }
}
