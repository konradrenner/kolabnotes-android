package org.kore.kolabnotes.android.content;

import android.content.Context;

import org.kore.kolab.notes.NotesRepository;

/**
 * This class syncs the database with data from a given repository
 *
 * Created by koni on 18.04.15.
 */
public class RepositoryManager {

    private final NoteTagRepository noteTagRepository;
    private final TagRepository tagRepository;
    private final NoteRepository noteRepository;
    private final NotebookRepository notebookRepository;
    private final ModificationRepository modificationRepository;

    private final NotesRepository repo;

    public RepositoryManager(Context context, NotesRepository repo) {
        this.noteTagRepository = new NoteTagRepository(context);
        this.tagRepository = new TagRepository(context);
        this.noteRepository = new NoteRepository(context);
        this.notebookRepository = new NotebookRepository(context);
        this.modificationRepository = new ModificationRepository(context);
        this.repo = repo;
    }

    /**
     * Gets the data from the given NotesRepository and changes the local data
     */
    public void syncFromNotesRepository(){
        //TODO
    }

    /**
     * Puts the local made changes into the NotesRepository
     */
    public void syncFromLocalData(){
        //TODO
    }
}
