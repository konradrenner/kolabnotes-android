package org.kore.kolabnotes.android.content;

import android.content.Context;
import android.util.Log;

import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolabnotes.android.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by koni on 11.06.15.
 */
public class DataCache  implements Serializable{

    private final List<Note> notes;
    private final Map<String,List<Note>> notesPerNotebook;
    private final List<Notebook> notebooks;
    private final NoteRepository notesRepository;
    private final NotebookRepository notebookRepository;
    private final AccountIdentifier account;
    private final Context context;
    private boolean initDone;

    public DataCache(Context context, AccountIdentifier account) {
        this.notes = new ArrayList<>();
        this.notebooks = new ArrayList<>();
        this.notesPerNotebook = new HashMap<>();
        this.notesRepository = new NoteRepository(context);
        this.notebookRepository = new NotebookRepository(context);
        this.account = account;
        this.context = context;
    }

    public void reloadData(){
        long ts = System.currentTimeMillis();
        Log.d("DataCache - reloadData", "Start reloading data");
        this.notes.clear();
        this.notesPerNotebook.clear();
        this.notebooks.clear();

        final NoteSorting noteSorting = Utils.getNoteSorting(context);

        this.notes.addAll(notesRepository.getAll(account.getAccount(),account.getRootFolder(), noteSorting));
        this.notebooks.addAll(notebookRepository.getAll(account.getAccount(),account.getRootFolder()));
        Log.d("DataCache - reloadData","Reloading finished in "+(System.currentTimeMillis()-ts)+"ms");
    }

    private void initData(){
        if(initDone){
            return;
        }
        reloadData();
        initDone = true;
    }

    public synchronized List<Note> getNotes() {
        initData();
        return Collections.unmodifiableList(notes);
    }

    public synchronized List<Note> getNotesFromNotebook(String uid) {
        initData();
        List<Note> perNotebook = notesPerNotebook.get(uid);
        if(perNotebook == null){
            final NoteSorting noteSorting = Utils.getNoteSorting(context);
            List<Note> fromNotebook = notesRepository.getFromNotebook(account.getAccount(), account.getRootFolder(), uid, noteSorting);
            perNotebook = new ArrayList<>(fromNotebook);
            notesPerNotebook.put(uid,perNotebook);
        }
        return Collections.unmodifiableList(perNotebook);
    }

    public synchronized List<Notebook> getNotebooks() {
        initData();
        return Collections.unmodifiableList(notebooks);
    }
}
